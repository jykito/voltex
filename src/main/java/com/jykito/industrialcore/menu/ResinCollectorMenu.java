package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.ResinCollectorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class ResinCollectorMenu extends AbstractContainerMenu {

    public final ResinCollectorBlockEntity blockEntity;
    private static final int MACHINE_SLOTS = 9;

    public ResinCollectorMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public ResinCollectorMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.RESIN_COLLECTOR_MENU.get(), containerId);
        this.blockEntity = (ResinCollectorBlockEntity) entity;
        var h = this.blockEntity.getItemHandler();

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                addSlot(new SlotItemHandler(h, row * 3 + col, 62 + col * 18, 18 + row * 18));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int k = 0; k < 9; k++)
            addSlot(new Slot(inv, k, 8 + k * 18, 142));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, MACHINE_SLOTS, false))
                    return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.RESIN_COLLECTOR.get());
    }
}
