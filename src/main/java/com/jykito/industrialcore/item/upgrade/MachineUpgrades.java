package com.jykito.industrialcore.item.upgrade;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Что дают апгрейды, лежащие в слотах машины. Снимок на один тик.
 * <p>
 * Правила одни на все машины: ускорители перемножаются по каждой штуке в стаке, из модулей
 * параллели берётся самый сильный, направленный апгрейд задаёт сторону для своего режима
 * (при двух одинаковых побеждает нижний слот). Машина читает только то, что умеет, остальное
 * игнорирует, например печь не смотрит на жидкостные стороны.
 */
public record MachineUpgrades(float timeMultiplier, float energyMultiplier, int parallel,
                              @Nullable Direction itemPush, @Nullable Direction itemPull,
                              @Nullable Direction fluidPush, @Nullable Direction fluidPull) {

    /** Слоты апгрейдов {@code first..last} включительно. */
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

    /** Время цикла с ускорителями, не меньше 1 тика. */
    public int scaleTime(int baseTicks) {
        return Math.max(1, (int) (baseTicks * timeMultiplier));
    }

    /** Расход за тик с ускорителями, не меньше 1 FE. */
    public int scaleEnergy(int baseFe) {
        return Math.max(1, (int) (baseFe * energyMultiplier));
    }
}
