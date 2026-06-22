package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.FluidConnectorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class FluidConnectorMenu extends AbstractContainerMenu {

    public final FluidConnectorBlockEntity blockEntity;
    private final ContainerData data;

    public FluidConnectorMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(3));
    }

    public FluidConnectorMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FLUID_CONNECTOR_MENU.get(), containerId);
        this.blockEntity = (FluidConnectorBlockEntity) entity;
        this.data = data;

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

    @Override
    public boolean clickMenuButton(Player player, int id) {
        switch (id) {
            case 0 -> { blockEntity.cycleMode(); data.set(0, blockEntity.mode.ordinal()); }
            case 1 -> { blockEntity.filterWhitelist = !blockEntity.filterWhitelist; blockEntity.setChanged(); }
            case 2 -> { blockEntity.filterMatchNbt  = !blockEntity.filterMatchNbt;  blockEntity.setChanged(); }
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
        int invStart = 1, invEnd = 37;
        if (index == 0) {
            if (!moveItemStackTo(stack, invStart, invEnd, false)) return ItemStack.EMPTY;
        } else {
            if (stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.ConnectorUpgradeItem) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (index < invStart + 27) {

                if (!moveItemStackTo(stack, invStart + 27, invEnd, false)) return ItemStack.EMPTY;
            } else {

                if (!moveItemStackTo(stack, invStart, invStart + 27, false)) return ItemStack.EMPTY;
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
                player, ModBlocks.FLUID_CONNECTOR.get());
    }

    private static class UpgradeSlot extends SlotItemHandler {
        UpgradeSlot(IItemHandler handler, int index, int x, int y) { super(handler, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.ConnectorUpgradeItem;
        }
        @Override public int getMaxStackSize() { return 1; }
    }
}
