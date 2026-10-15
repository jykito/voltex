package com.jykito.industrialcore.block.entity;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * A tank that grows together with the parallel module.
 * <p>
 * Without it a large module would hit the tank on the very first batch: x64 at once asks for 64 times
 * more fluid, and the player would see "parallel does not work" instead of "the tank is too small".
 * <p>
 * Multipliers of the base capacity, not absolute numbers: changing the base moves the whole ladder.
 * Growth deliberately lags behind batch growth, so a large module needs a denser supply
 * rather than taking load off it.
 * <p>
 * ⚠️ Capacity is NOT stored in the tank NBT: {@code writeToNBT} only stores the fluid. So it has to be
 * saved and read through {@link #save} and {@link #load}, otherwise after relogging the tank returns
 * to the base capacity and the GUI gauge computes its fraction from the wrong number.
 */
public final class AdaptiveTank {

    private AdaptiveTank() {}

    public static int capacityFor(int base, int parallel) {
        if (parallel >= 64) return base * 16;
        if (parallel >= 32) return base * 8;
        if (parallel >= 8)  return base * 4;
        return base;
    }

    /**
     * Fits the capacity to the module. When shrinking, the excess is drained: while the tank holds more than its capacity,
     * the gauge computes a fraction above one and the fluid is drawn as a column taller than the tank.
     * That is also the price for removing the upgrade.
     *
     * @return true if the capacity changed; then the caller has to send a block update.
     */
    public static boolean resize(FluidTank tank, int base, int parallel) {
        int want = capacityFor(base, parallel);
        if (tank.getCapacity() == want) return false;

        tank.setCapacity(want);
        int excess = tank.getFluidAmount() - want;
        if (excess > 0) tank.drain(excess, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    public static void save(CompoundTag tag, String key, FluidTank tank) {
        tag.putInt(key, tank.getCapacity());
    }

    /** Call BEFORE reading the fluid, otherwise the tank reads its contents into the old capacity. */
    public static void load(CompoundTag tag, String key, FluidTank tank, int base) {
        tank.setCapacity(Math.max(base, tag.getInt(key)));
    }
}
