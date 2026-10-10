package com.jykito.industrialcore.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/** Общее для меню машин: инвентарь игрока в сетке GUI мода и распаковка синхронизированных чисел. */
public abstract class MachineMenu extends AbstractContainerMenu {

    /** Шаг сетки слотов в артах мода (у ванили 18). */
    protected static final int SLOT_STEP = 19;
    /** Хотбар ниже верхнего ряда инвентаря: три ряда плюс зазор 6 px. */
    private static final int HOTBAR_OFFSET = 3 * SLOT_STEP + 6;

    protected MachineMenu(@Nullable MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    /** Инвентарь игрока 3×9 с левым верхним слотом в (x, y) и хотбар под ним. */
    protected void addPlayerInventory(Inventory inv, int x, int y) {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inv, j + i * 9 + 9, x + j * SLOT_STEP, y + i * SLOT_STEP));
        for (int k = 0; k < 9; k++)
            addSlot(new Slot(inv, k, x + k * SLOT_STEP, y + HOTBAR_OFFSET));
    }

    /**
     * ContainerData передаёт клиенту только 16 бит на ячейку, поэтому int больше 32767
     * (энергия, ёмкости) идёт двумя ячейками. Младшую маскируем: иначе её знак портит число.
     */
    protected static int unpack(int lo, int hi) {
        return (hi << 16) | (lo & 0xFFFF);
    }

    /** Длина заливки полоски в пикселях, с округлением вниз. */
    protected static int scale(int value, int max, int px) {
        return max != 0 && value != 0 ? (int) ((long) value * px / max) : 0;
    }

    /** Длина стрелки прогресса: первый же тик уже виден (округление вверх), не длиннее px. */
    protected static int scaleProgress(int progress, int max, int px) {
        if (max == 0 || progress == 0) return 0;
        return Math.min(px, (progress * px + max - 1) / max);
    }
}
