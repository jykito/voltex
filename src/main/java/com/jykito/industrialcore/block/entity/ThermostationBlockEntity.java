package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.custom.ThermostationCoreBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.heat.HeatBuffer;
import com.jykito.industrialcore.heat.HeatCapability;
import com.jykito.industrialcore.heat.IHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.network.chat.Component;
import net.minecraftforge.items.ItemStackHandler;
import com.jykito.industrialcore.menu.ThermostationMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thermal station (tower): core + expanders above. Thermogenerators push heat into the side faces of any
 * layer -> it all accumulates in the CORE buffer. Radiators on the back faces of the layers remove heat. The core pushes
 * the aggregated output down (to a pipe/columns), paying FE per unit pushed. A full buffer does not stop the station:
 * it just accepts no more heat, so the thermogenerators are held back.
 * One BE class for core and expander; only the core ticks the logic, the expander forwards heat to the core.
 */
public class ThermostationBlockEntity extends BlockEntity implements MenuProvider {

    private static final int HEAT_BASE      = 40_000;   // buffer of a solo core
    private static final int HEAT_PER_EXP   = 20_000;   // +buffer per expander
    private static final int HEAT_MAX_CAP   = HEAT_BASE + 15 * HEAT_PER_EXP; // cap for the buffer constructor
    private static final int BASE_OUTPUT    = 100;      // output of a solo core per tick
    private static final int OUTPUT_PER_RAD = 120;      // +throughput per radiator per tick
    public  static final int ENERGY_CAP      = 100_000; // core FE buffer
    private static final int ENERGY_PER_HEAT = 5;       // FE per unit of heat pushed out

    private final HeatBuffer buffer = new HeatBuffer(HEAT_MAX_CAP);  // the real cap is set by tower height (rescan)
    private int radScanCd = 0, radiators = 0, maxOutput = BASE_OUTPUT;
    private int activeCd = 0;                                        // "working" holds for N ticks after an output (anti-flicker)

    private LazyOptional<IHeatHandler> lazyIn = LazyOptional.empty();  // intake on the side faces -> core buffer
    private LazyOptional<IHeatHandler> lazyOut = LazyOptional.empty(); // output DOWN (core): the pipe sees it for connecting, intake = 0

    private final MachineEnergy energyStorage = MachineEnergy.consumer(ENERGY_CAP, ENERGY_CAP);   // FE input on the CORE back face
    private LazyOptional<IEnergyStorage> energyOptional = LazyOptional.empty();

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {  // x2 upgrade slot (locked in the menu for now)
        @Override protected void onContentsChanged(int s) { setChanged(); }
    };
    private int genAccum = 0, genRate = 0;                             // heat generation per tick tracking (for the GUI)
    protected final ContainerData data;

    public ThermostationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMOSTATION_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override public int get(int i) {
                int energy = energyStorage.getEnergyStored(), heat = getHeat(), max = getMaxHeat();
                return switch (i) {
                    case 0 -> energy & 0xFFFF;
                    case 1 -> (energy >>> 16) & 0xFFFF;
                    case 2 -> heat & 0xFFFF;
                    case 3 -> (heat >>> 16) & 0xFFFF;
                    case 4 -> max & 0xFFFF;
                    case 5 -> (max >>> 16) & 0xFFFF;
                    case 6 -> Math.min(32767, genRate);
                    case 7 -> radiators;
                    case 8 -> activeCd > 0 ? 1 : 0;
                    case 9 -> Math.min(32767, maxOutput);
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 10; }
        };
    }

    public boolean isCore()   { return getBlockState().is(ModBlocks.THERMOSTATION_CORE.get()); }
    public int getHeat()      { HeatBuffer b = coreBuffer(); return b == null ? 0 : b.getHeat(); }
    public int getMaxHeat()   { HeatBuffer b = coreBuffer(); return b == null ? HEAT_BASE : b.getMaxHeat(); }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.thermostation_core"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ThermostationMenu(id, inv, this, this.data);
    }

    private Direction facing() {
        return getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
    }

    /** Buffer of the tower core (the core has its own; an expander walks down to the core). */
    @Nullable private HeatBuffer coreBuffer() { ThermostationBlockEntity c = core(); return c == null ? null : c.buffer; }

    @Nullable private ThermostationBlockEntity core() {
        if (isCore()) return this;
        if (level == null) return null;
        BlockPos p = worldPosition.below();
        for (int i = 0; i < 16; i++) {
            if (!(level.getBlockEntity(p) instanceof ThermostationBlockEntity be)) return null;
            if (be.isCore()) return be;
            p = p.below();
        }
        return null;
    }

    // side faces (by FACING) accept heat from thermogenerators
    private boolean isInputSide(Direction side) {
        Direction f = facing();
        return side == f.getClockWise() || side == f.getCounterClockWise();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == HeatCapability.HEAT && side != null) {
            if (isInputSide(side)) return lazyIn.cast();
            if (side == Direction.DOWN && isCore()) return lazyOut.cast();  // output down to the pipe (joint visual)
        }
        if (cap == ForgeCapabilities.ENERGY && isCore() && (side == null || side == facing().getOpposite()))
            return energyOptional.cast();                                   // FE input on the core back face
        return super.getCapability(cap, side);
    }

    @Override public void onLoad() {
        super.onLoad();
        lazyIn = LazyOptional.of(() -> new IHeatHandler() {
            @Override public int getHeat()    { HeatBuffer b = coreBuffer(); return b == null ? 0 : b.getHeat(); }
            @Override public int getMaxHeat()  { HeatBuffer b = coreBuffer(); return b == null ? HEAT_BASE : b.getMaxHeat(); }
            @Override public int receiveHeat(int amount, boolean simulate) {
                ThermostationBlockEntity c = core();
                if (c == null) return 0;
                int acc = c.buffer.receiveHeat(amount, simulate);         // full buffer -> 0 (back-pressure on thermogenerators)
                if (!simulate && acc > 0) c.genAccum += acc;              // generation tracking for the GUI
                return acc;
            }
        });
        lazyOut = LazyOptional.of(() -> new IHeatHandler() {              // output face downwards: the pipe "sees" it -> draws the joint
            @Override public int getHeat()    { HeatBuffer b = coreBuffer(); return b == null ? 0 : b.getHeat(); }
            @Override public int getMaxHeat()  { HeatBuffer b = coreBuffer(); return b == null ? HEAT_BASE : b.getMaxHeat(); }
            @Override public int receiveHeat(int amount, boolean simulate) { return 0; } // this is the output, it does not take heat back
        });
        energyOptional = LazyOptional.of(() -> energyStorage);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); lazyIn.invalidate(); lazyOut.invalidate(); energyOptional.invalidate(); }

    // ── tick only on the core ──
    public static void serverTick(Level level, BlockPos pos, BlockState state, ThermostationBlockEntity be) {
        if (!be.isCore()) return;

        be.genRate = be.genAccum; be.genAccum = 0;   // generation per tick snapshot for the GUI

        if (--be.radScanCd <= 0) { be.radScanCd = 10; be.rescan(level); } // tower scan: radiators + height (throttled)

        // output down: min(throughput, heat, energy budget); pay FE for what was pushed
        boolean worked = false;
        int have = be.buffer.getHeat();
        if (have > 0) {
            int budget = be.energyStorage.getEnergyStored() / ENERGY_PER_HEAT;
            int want = Math.min(be.maxOutput, Math.min(have, budget));
            if (want > 0) {
                IHeatHandler down = neighborHeat(level, pos.below(), Direction.UP); // output to the pipe/columns
                if (down != null) {
                    int accepted = down.receiveHeat(want, false);
                    if (accepted > 0) {
                        be.buffer.extract(accepted);
                        be.energyStorage.consumeEnergy(accepted * ENERGY_PER_HEAT);
                        worked = true;
                        be.setChanged();
                    }
                }
            }
        }

        // visual on/off: "working" holds 10 ticks after the last output (anti-flicker)
        if (worked) be.activeCd = 10; else if (be.activeCd > 0) be.activeCd--;
        boolean active = be.activeCd > 0;
        if (state.hasProperty(ThermostationCoreBlock.ACTIVE) && state.getValue(ThermostationCoreBlock.ACTIVE) != active)
            level.setBlock(pos, state.setValue(ThermostationCoreBlock.ACTIVE, active), 3);
    }

    /** Tower scan upwards: buffer = HEAT_BASE + expanders × HEAT_PER_EXP; throughput = BASE_OUTPUT + radiators × OUTPUT_PER_RAD. */
    private void rescan(Level level) {
        int rad = 0, expanders = 0; BlockPos p = worldPosition;
        for (int i = 0; i < 16; i++) {
            if (!(level.getBlockEntity(p) instanceof ThermostationBlockEntity be)) break;
            if (!be.isCore()) {
                expanders++;
                if (level.getBlockState(p.relative(be.facing().getOpposite())).is(ModBlocks.THERMOSTATION_RADIATOR.get())) rad++;
            }
            p = p.above();
        }
        radiators = rad;
        maxOutput = BASE_OUTPUT + rad * OUTPUT_PER_RAD;
        buffer.setMax(HEAT_BASE + expanders * HEAT_PER_EXP);
    }

    @Nullable private static IHeatHandler neighborHeat(Level level, BlockPos pos, Direction from) {
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? null : be.getCapability(HeatCapability.HEAT, from).resolve().orElse(null);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("heat", buffer.getHeat());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("inv", itemHandler.serializeNBT());
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        buffer.setHeat(tag.getInt("heat"));
        energyStorage.setEnergy(tag.getInt("energy"));
        if (tag.contains("inv")) itemHandler.deserializeNBT(tag.getCompound("inv"));
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = new CompoundTag(); saveAdditional(t); return t; }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
