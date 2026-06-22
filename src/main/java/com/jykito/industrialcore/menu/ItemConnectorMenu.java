package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.ItemConnectorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ItemConnectorMenu extends AbstractContainerMenu {

    public static final int FILTER_SLOTS = ItemConnectorBlockEntity.FILTER_SLOTS;

    public static final int FILTER_X0  = 86;
    public static final int FILTER_Y0  = 37;
    public static final int FILTER_GAP = 17;

    private static final int UPGRADE_IDX  = FILTER_SLOTS;
    private static final int INV_START    = FILTER_SLOTS + 1;
    private static final int INV_END      = INV_START + 36;

    public final ItemConnectorBlockEntity blockEntity;
    private final ContainerData data;

    public ItemConnectorMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(4));
    }

    public ItemConnectorMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ITEM_CONNECTOR_MENU.get(), containerId);
        this.blockEntity = (ItemConnectorBlockEntity) entity;
        this.data = data;

        for (int s = 0; s < FILTER_SLOTS; s++) {
            int col = s % 5, row = s / 5;
            addSlot(new GhostSlot(blockEntity.filterHandler, s,
                    FILTER_X0 + col * FILTER_GAP,
                    FILTER_Y0 + row * FILTER_GAP));
        }

        addSlot(new UpgradeSlot(blockEntity.upgradeHandler, 0, 189, 88));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 119 + i * 19));

        for (int k = 0; k < 9; k++)
            addSlot(new Slot(inv, k, 44 + k * 19, 181));

        addDataSlots(data);
    }

    public int getMode()         { return data.get(0); }
    public boolean isWhitelist() { return data.get(1) == 0; }
    public boolean isMatchNbt()  { return data.get(2) == 1; }
    public boolean isMatchDmg()  { return data.get(3) == 1; }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < FILTER_SLOTS) {
            ItemStack cursor = getCarried();
            if (!cursor.isEmpty()) {
                ItemStack ghost = cursor.copy();
                ghost.setCount(1);
                slots.get(slotId).set(ghost);
            } else {
                slots.get(slotId).set(ItemStack.EMPTY);
            }
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        switch (id) {
            case 0 -> { blockEntity.cycleMode(); data.set(0, blockEntity.mode.ordinal()); }
            case 1 -> { blockEntity.filterWhitelist   = !blockEntity.filterWhitelist;   blockEntity.setChanged(); }
            case 2 -> { blockEntity.filterMatchNbt    = !blockEntity.filterMatchNbt;    blockEntity.setChanged(); }
            case 3 -> { blockEntity.filterMatchDamage = !blockEntity.filterMatchDamage; blockEntity.setChanged(); }
            default -> { return false; }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < FILTER_SLOTS) return ItemStack.EMPTY;
        if (index == UPGRADE_IDX) {

            if (!moveItemStackTo(stack, INV_START, INV_END, false)) return ItemStack.EMPTY;
        } else {

            if (stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.ConnectorUpgradeItem) {
                if (!moveItemStackTo(stack, UPGRADE_IDX, UPGRADE_IDX + 1, false)) return ItemStack.EMPTY;
            } else if (index < INV_START + 27) {

                if (!moveItemStackTo(stack, INV_START + 27, INV_END, false)) return ItemStack.EMPTY;
            } else {

                if (!moveItemStackTo(stack, INV_START, INV_START + 27, false)) return ItemStack.EMPTY;
            }
        }
        if (stack.getCount() == 0) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.ITEM_CONNECTOR.get());
    }

    private static class GhostSlot extends SlotItemHandler {
        GhostSlot(IItemHandler handler, int index, int x, int y) { super(handler, index, x, y); }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public int getMaxStackSize() { return 1; }
    }

    private static class UpgradeSlot extends SlotItemHandler {
        UpgradeSlot(IItemHandler handler, int index, int x, int y) { super(handler, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.ConnectorUpgradeItem;
        }
        @Override public int getMaxStackSize() { return 1; }
    }
}
