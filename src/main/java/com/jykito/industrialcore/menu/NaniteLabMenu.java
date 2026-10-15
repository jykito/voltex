package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.NaniteLabBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/** Nanite lab window: scientist, sample, blank card, written card, inventory. */
public class NaniteLabMenu extends MachineMenu {

    public static final int STRAIN_X = 23, STRAIN_Y = 47, SAMPLE_X = 23, SAMPLE_Y = 75,
            BLANK_X = 97, BLANK_Y = 61, OUT_X = 197, OUT_Y = 61, INV_X = 44, INV_Y = 145;
    private static final int MACHINE_SLOTS = 4;

    public final NaniteLabBlockEntity lab;
    private final ContainerData data;

    public NaniteLabMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(8));
    }

    public NaniteLabMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.NANITE_LAB_MENU.get(), id);
        this.lab = (NaniteLabBlockEntity) be;
        this.data = data;
        var h = lab.getItems();
        addSlot(new SlotItemHandler(h, NaniteLabBlockEntity.SLOT_STRAIN, STRAIN_X, STRAIN_Y));
        addSlot(new SlotItemHandler(h, NaniteLabBlockEntity.SLOT_SAMPLE, SAMPLE_X, SAMPLE_Y));
        addSlot(new SlotItemHandler(h, NaniteLabBlockEntity.SLOT_BLANK, BLANK_X, BLANK_Y));
        addSlot(new SlotItemHandler(h, NaniteLabBlockEntity.SLOT_OUTPUT, OUT_X, OUT_Y) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        addPlayerInventory(inv, INV_X, INV_Y);
        addDataSlots(data);
    }

    public int energy()    { return unpack(data.get(0), data.get(1)); }
    public int energyCap() { return unpack(data.get(2), data.get(3)); }
    public int progress()  { return data.get(4); }
    public int maxProgress() { return data.get(5); }
    /** Knowledge threshold of the sample topic, 0..1. */
    public float threshold() { return data.get(7) / 1000f; }
    public NaniteLabBlockEntity.Status status() {
        var v = NaniteLabBlockEntity.Status.values();
        return v[Math.max(0, Math.min(v.length - 1, data.get(6)))];
    }

    public int scaledEnergy(int px)   { return scale(energy(), energyCap(), px); }
    public int scaledProgress(int px) { return scaleProgress(progress(), maxProgress(), px); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), result = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int to = 0; to < MACHINE_SLOTS - 1 && !moved; to++)
                if (slots.get(to).mayPlace(stack)) moved = moveItemStackTo(stack, to, to + 1, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return !lab.isRemoved() && player.distanceToSqr(lab.getBlockPos().getCenter()) < 64;
    }
}
