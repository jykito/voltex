package com.jykito.industrialcore.block.entity;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.minecraft.core.HolderLookup;

import com.jykito.industrialcore.energy.MachineEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base of a wind turbine: the buffer the nacelle fills ({@link #receiveWind}) and the output into cables and machines
 * on every side except the top, where the mast stands.
 * <p>
 * A rotor from a pack can make far more than the basic one, so the buffer grows to hold {@link #BUFFER_TICKS} ticks of the
 * inflow and the output passes twice the inflow. The buffer only grows (and is saved), so a calm or a reload does not
 * cut the stored energy.
 */
public class WindTurbineBaseBlockEntity extends BlockEntity implements Caps.Provider  {

    public static final int CAPACITY = 250_000, OUTPUT = 1000;
    private static final int BUFFER_TICKS = 200;

    private final MachineEnergy energy = MachineEnergy.generator(CAPACITY, OUTPUT);
    /** FE per tick the nacelle sent last. */
    private int inflow;

    public WindTurbineBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_TURBINE_BASE_BE.get(), pos, state);
    }

    /** What the nacelle made this tick; surplus over the capacity is lost. */
    public void receiveWind(int fe) {
        if (fe != inflow) {
            inflow = fe;
            fitTo(fe);
        }
        energy.addEnergy(fe);
        setChanged();
    }

    private int outputLimit() {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(OUTPUT, 2L * inflow));
    }

    /** Grows the buffer and the output for this inflow; never shrinks them. */
    private void fitTo(int fe) {
        long cap = Math.max(energy.getMaxEnergyStored(), Math.max((long) CAPACITY, (long) fe * BUFFER_TICKS));
        energy.setLimits((int) Math.min(Integer.MAX_VALUE, cap), outputLimit());
    }

    public void serverTick(Level level, BlockPos pos) {
        int left = Math.min(energy.getEnergyStored(), outputLimit());
        for (Direction dir : Direction.values()) {
            if (left <= 0) break;
            if (dir == Direction.UP) continue;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;
            IEnergyStorage target = Caps.get(neighbor, Caps.ENERGY, dir.getOpposite()).orElse(null);
            if (target == null || !target.canReceive()) continue;
            int accepted = target.receiveEnergy(left, false);
            if (accepted > 0) {
                energy.consumeEnergy(accepted);
                left -= accepted;
                setChanged();
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

        @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK && side != Direction.UP) return (T) energy;
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("capacity", energy.getMaxEnergyStored());
        tag.putInt("inflow", inflow);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inflow = tag.getInt("inflow");
        energy.setLimits(Math.max(CAPACITY, tag.getInt("capacity")), outputLimit());
        energy.setEnergy(tag.getInt("energy"));
    }
}
