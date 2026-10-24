package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.energy.MachineEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
 * Base of a wind turbine: the buffer the nacelle fills ({@link #receiveWind}) and the output into cables and machines
 * on every side except the top, where the mast stands.
 * <p>
 * A rotor from a pack can make far more than the basic one, so the buffer grows to hold {@link #BUFFER_TICKS} ticks of the
 * inflow and the output passes twice the inflow. The buffer only grows (and is saved), so a calm or a reload does not
 * cut the stored energy.
 */
public class WindTurbineBaseBlockEntity extends BlockEntity {

    public static final int CAPACITY = 250_000, OUTPUT = 1000;
    private static final int BUFFER_TICKS = 200;

    private final MachineEnergy energy = MachineEnergy.generator(CAPACITY, OUTPUT);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();
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
            IEnergyStorage target = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).orElse(null);
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
        lazyEnergy = LazyOptional.of(() -> energy);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && side != Direction.UP) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("capacity", energy.getMaxEnergyStored());
        tag.putInt("inflow", inflow);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inflow = tag.getInt("inflow");
        energy.setLimits(Math.max(CAPACITY, tag.getInt("capacity")), outputLimit());
        energy.setEnergy(tag.getInt("energy"));
    }
}
