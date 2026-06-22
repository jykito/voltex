package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.RecipeExporterBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class RecipeExporterMenu extends AbstractContainerMenu {
    public final RecipeExporterBlockEntity blockEntity;

    public RecipeExporterMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (RecipeExporterBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public RecipeExporterMenu(int id, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.RECIPE_EXPORTER_MENU.get(), id);
        this.blockEntity = (RecipeExporterBlockEntity) entity;
        var h = this.blockEntity.getItemHandler();

        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                this.addSlot(new SlotItemHandler(h, r * 3 + c, 30 + c * 18, 17 + r * 18));

        this.addSlot(new SlotItemHandler(h, 9, 124, 35));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int k = 0; k < 9; k++)
            this.addSlot(new Slot(inv, k, 8 + k * 18, 142));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        final int MACHINE_SLOTS = 10;
        final int INV_END = MACHINE_SLOTS + 36;

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(sourceStack, MACHINE_SLOTS, INV_END, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(sourceStack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();
        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.RECIPE_EXPORTER.get());
    }
}
