package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.EnergyBankBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.menu.EnergyBankMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Energy bank: FE storage. Accepts on all sides, pushes surplus to neighbours (keeps a RESERVE %).
 * The charge reaches the client two ways: blockstate LEVEL (gauge on the side) + ContainerData (GUI).
 */
public class EnergyBankBlockEntity extends BlockEntity implements MenuProvider {

    public static final int CAPACITY = 10_000_000;
    private static final int IO_RATE  = 100_000;
    private static final int HYST     = CAPACITY / 100; // gauge level hysteresis (1%), so it does not flicker at a boundary

    private static class BankEnergy extends MachineEnergy {
        int inTick, outTick;                       // flow accumulated per tick (for the monitor)
        BankEnergy() { super(CAPACITY, IO_RATE, IO_RATE); }
        @Override public int receiveEnergy(int max, boolean sim) { int r = super.receiveEnergy(max, sim); if (!sim) inTick += r; return r; }
        @Override public int extractEnergy(int max, boolean sim) { int e = super.extractEnergy(max, sim); if (!sim) outTick += e; return e; }
    }
    private final BankEnergy energy = new BankEnergy();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    private int reservePercent = 0;      // keep this % in reserve (do not give below it)
    private int ioIn = 0, ioOut = 0;     // FE/tick of the previous tick (GUI monitor)
    protected final ContainerData data;

    public EnergyBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_BANK_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override public int get(int i) {
                int e = energy.getEnergyStored();
                return switch (i) {
                    case 0 -> e & 0xFFFF;
                    case 1 -> (e >>> 16) & 0xFFFF;
                    case 2 -> ioIn & 0xFFFF;
                    case 3 -> (ioIn >>> 16) & 0xFFFF;
                    case 4 -> ioOut & 0xFFFF;
                    case 5 -> (ioOut >>> 16) & 0xFFFF;
                    case 6 -> reservePercent;
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 7; }
        };
    }

    public int getEnergyStored() { return energy.getEnergyStored(); }
    public int getMaxEnergy()    { return CAPACITY; }
    public int getReserve()      { return reservePercent; }
    public void setReserve(int pct) { reservePercent = Mth.clamp(pct, 0, 100); setChanged(); }

    @Override public void onLoad() { super.onLoad(); lazyEnergy = LazyOptional.of(() -> energy); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyEnergy.invalidate(); }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.energy_bank"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new EnergyBankMenu(id, inv, this, this.data);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyBankBlockEntity be) {
        // flow snapshot of the previous tick (GUI monitor), NET.
        // A dumb wire into a dead end pulls from the bank and pushes the same FE back on the next tick:
        // a gross counter would show a phantom 100k output. Net: flowed in == flowed out -> 0.
        int flowIn = be.energy.inTick, flowOut = be.energy.outTick;
        be.energy.inTick = 0; be.energy.outTick = 0;
        int net = flowIn - flowOut;
        be.ioIn = Math.max(0, net); be.ioOut = Math.max(0, -net);

        // output: keep the RESERVE, push the surplus into receiving neighbours
        int reserveAmt = be.reservePercent * (CAPACITY / 100);
        if (be.energy.getEnergyStored() - reserveAmt > 0) {
            for (Direction d : Direction.values()) {
                int avail = be.energy.getEnergyStored() - reserveAmt;
                if (avail <= 0) break;
                BlockEntity nbe = level.getBlockEntity(pos.relative(d));
                if (nbe == null) continue;
                final int cap = Math.min(IO_RATE, avail);
                nbe.getCapability(ForgeCapabilities.ENERGY, d.getOpposite()).ifPresent(n -> {
                    if (n.canReceive()) {
                        int sent = n.receiveEnergy(cap, false);
                        if (sent > 0) be.energy.extractEnergy(sent, false);
                    }
                });
            }
        }

        // visual level 0..10 (hysteresis + throttle: remesh ONLY when the level changes)
        int charge = be.energy.getEnergyStored();
        int lvl = state.getValue(EnergyBankBlock.LEVEL), nl = lvl;
        while (nl < 10 && charge >= (nl + 1) * (CAPACITY / 10)) nl++;
        while (nl > 0 && charge <  nl * (CAPACITY / 10) - HYST) nl--;
        if (nl != lvl) level.setBlock(pos, state.setValue(EnergyBankBlock.LEVEL, nl), 2);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("reserve", reservePercent);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getInt("energy"));
        reservePercent = tag.getInt("reserve");
    }
}
