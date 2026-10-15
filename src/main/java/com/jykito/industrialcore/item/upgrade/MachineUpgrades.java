package com.jykito.industrialcore.item.upgrade;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What the upgrades in the machine slots give. A snapshot for one tick.
 * <p>
 * The rules are the same for all machines: accelerators multiply per item in the stack, of the
 * parallel modules the strongest one is taken, a directional upgrade sets the side for its mode
 * (with two of the same kind the lower slot wins). A machine reads only what it can use and ignores
 * the rest, for example the furnace does not look at fluid sides.
 */
public record MachineUpgrades(float timeMultiplier, float energyMultiplier, int parallel,
                              @Nullable Direction itemPush, @Nullable Direction itemPull,
                              @Nullable Direction fluidPush, @Nullable Direction fluidPull) {

    /** Upgrade slots {@code first..last} inclusive. */
    public static MachineUpgrades scan(IItemHandler handler, int first, int last) {
        float time = 1.0f, energy = 1.0f;
        int parallel = 1;
        Direction itemPush = null, itemPull = null, fluidPush = null, fluidPull = null;
        for (int s = first; s <= last; s++) {
            ItemStack stack = handler.getStackInSlot(s);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof AcceleratorItem acc) {
                for (int c = 0; c < stack.getCount(); c++) {
                    time   *= acc.getSpeedMultiplier();
                    energy *= acc.getEnergyMultiplier();
                }
            } else if (stack.getItem() instanceof ParallelModuleItem pm) {
                parallel = Math.max(parallel, pm.getParallelCount());
            } else if (stack.getItem() instanceof AdvancedParallelModuleItem adv) {
                parallel = Math.max(parallel, adv.getParallelCount());
            } else if (stack.getItem() instanceof DirectionalUpgradeItem dir) {
                Direction d = DirectionalUpgradeItem.getStoredDirection(stack);
                if (d != null) switch (dir.getMode()) {
                    case ITEM_PUSH  -> itemPush  = d;
                    case ITEM_PULL  -> itemPull  = d;
                    case FLUID_PUSH -> fluidPush = d;
                    case FLUID_PULL -> fluidPull = d;
                }
            }
        }
        return new MachineUpgrades(time, energy, parallel, itemPush, itemPull, fluidPush, fluidPull);
    }

    /** Cycle time with accelerators, at least 1 tick. */
    public int scaleTime(int baseTicks) {
        return Math.max(1, (int) (baseTicks * timeMultiplier));
    }

    /** Usage per tick with accelerators, at least 1 FE. */
    public int scaleEnergy(int baseFe) {
        return Math.max(1, (int) (baseFe * energyMultiplier));
    }
}
