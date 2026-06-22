package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.CrateBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class CrateMenu extends AbstractContainerMenu {

    public final CrateBlockEntity blockEntity;
    private final int rows;
    private final int containerSlots;

    public CrateMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (CrateBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public CrateMenu(int id, Inventory inv, CrateBlockEntity be) {
        super(ModMenuTypes.CRATE_MENU.get(), id);
        this.blockEntity = be;
        this.rows = be.getRows();
        this.containerSlots = rows * 9;
        var h = be.getItems();

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new SlotItemHandler(h, c + r * 9, 8 + c * 18, 18 + r * 18));

        int k = (rows - 4) * 18;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 103 + i * 18 + k));
        for (int j = 0; j < 9; j++)
            addSlot(new Slot(inv, j, 8 + j * 18, 161 + k));
    }

    public int getRows() { return rows; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ret = stack.copy();
            if (index < containerSlots) {
                if (!moveItemStackTo(stack, containerSlots, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, containerSlots, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
            if (stack.getCount() == ret.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return ret;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
}
