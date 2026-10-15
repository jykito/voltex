package com.jykito.industrialcore.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/** Shared by machine menus: the player inventory in the mod's GUI grid and unpacking synced numbers. */
public abstract class MachineMenu extends AbstractContainerMenu {

    /** Slot grid step in the mod's art (vanilla uses 18). */
    protected static final int SLOT_STEP = 19;
    /** Hotbar below the top inventory row: three rows plus a 6 px gap. */
    private static final int HOTBAR_OFFSET = 3 * SLOT_STEP + 6;

    protected MachineMenu(@Nullable MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    /** Player inventory 3x9 with the top-left slot at (x, y) and the hotbar below it. */
    protected void addPlayerInventory(Inventory inv, int x, int y) {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inv, j + i * 9 + 9, x + j * SLOT_STEP, y + i * SLOT_STEP));
        for (int k = 0; k < 9; k++)
            addSlot(new Slot(inv, k, x + k * SLOT_STEP, y + HOTBAR_OFFSET));
    }

    /**
     * ContainerData only sends 16 bits per cell to the client, so an int above 32767
     * (energy, capacities) goes in two cells. The low half is masked, otherwise its sign spoils the number.
     */
    protected static int unpack(int lo, int hi) {
        return (hi << 16) | (lo & 0xFFFF);
    }

    /** Fill length of a bar in pixels, rounded down. */
    protected static int scale(int value, int max, int px) {
        return max != 0 && value != 0 ? (int) ((long) value * px / max) : 0;
    }

    /** Progress arrow length: the very first tick is already visible (rounded up), no longer than px. */
    protected static int scaleProgress(int progress, int max, int px) {
        if (max == 0 || progress == 0) return 0;
        return Math.min(px, (progress * px + max - 1) / max);
    }
}
