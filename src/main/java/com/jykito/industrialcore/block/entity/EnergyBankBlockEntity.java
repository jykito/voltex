package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.EnergyBankBlock;
import com.jykito.industrialcore.block.custom.EnergyBankTier;
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
import net.minecraft.world.item.ItemStack;
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
 * Capacity and throughput come from the block's {@link EnergyBankTier}. The charge travels with the item
 * (BlockEntityTag), so a bank can be moved and upgraded without losing it.
 */
public class EnergyBankBlockEntity extends BlockEntity implements MenuProvider {

    private static class BankEnergy extends MachineEnergy {
        int inTick, outTick;                       // flow accumulated per tick (for the monitor)
        BankEnergy(EnergyBankTier tier) { super(tier.capacity, tier.ioRate, tier.ioRate); }
        @Override public int receiveEnergy(int max, boolean sim) { int r = super.receiveEnergy(max, sim); if (!sim) inTick += r; return r; }
        @Override public int extractEnergy(int max, boolean sim) { int e = super.extractEnergy(max, sim); if (!sim) outTick += e; return e; }
    }
    private final EnergyBankTier tier;
    private final BankEnergy energy;
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    private int reservePercent = 0;      // keep this % in reserve (do not give below it)
    private int ioIn = 0, ioOut = 0;     // FE/tick of the previous tick (GUI monitor)
    protected final ContainerData data;

    public EnergyBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_BANK_BE.get(), pos, state);
        this.tier = tierOf(state);
        this.energy = new BankEnergy(tier);
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
                    case 7 -> tier.capacity & 0xFFFF;
                    case 8 -> (tier.capacity >>> 16) & 0xFFFF;
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 9; }
        };
    }

    public int getEnergyStored() { return energy.getEnergyStored(); }
    public int getMaxEnergy()    { return tier.capacity; }
    public EnergyBankTier getTier() { return tier; }

    private static EnergyBankTier tierOf(BlockState state) {
        return state.getBlock() instanceof EnergyBankBlock b ? b.tier : EnergyBankTier.T1;
    }

    /** The bank as an item that keeps its charge and reserve, for the wrench and for the upgrade recipe. */
    public ItemStack toItem() {
        ItemStack stack = new ItemStack(getBlockState().getBlock());
        if (energy.getEnergyStored() > 0 || reservePercent > 0) saveToItem(stack);
        return stack;
    }
    public int getReserve()      { return reservePercent; }
    public void setReserve(int pct) { reservePercent = Mth.clamp(pct, 0, 100); setChanged(); }

    @Override public void onLoad() { super.onLoad(); lazyEnergy = LazyOptional.of(() -> energy); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyEnergy.invalidate(); }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override public Component getDisplayName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }
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
        final int capacity = be.tier.capacity, ioRate = be.tier.ioRate;
        int reserveAmt = be.reservePercent * (capacity / 100);
        if (be.energy.getEnergyStored() - reserveAmt > 0) {
            for (Direction d : Direction.values()) {
                int avail = be.energy.getEnergyStored() - reserveAmt;
                if (avail <= 0) break;
                BlockEntity nbe = level.getBlockEntity(pos.relative(d));
                if (nbe == null) continue;
                final int cap = Math.min(ioRate, avail);
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
        final int step = capacity / 10, hyst = capacity / 100;   // hysteresis 1%, so the gauge does not flicker at a boundary
        while (nl < 10 && charge >= (nl + 1) * step) nl++;
        while (nl > 0 && charge <  nl * step - hyst) nl--;
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
