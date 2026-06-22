package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class BlastFurnaceMenu extends AbstractContainerMenu {
    public final BlastFurnaceBlockEntity blockEntity;
    private final ContainerData data;

    public BlastFurnaceMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(3));
    }

    public BlastFurnaceMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.BLAST_FURNACE_MENU.get(), pContainerId);
        this.blockEntity = (BlastFurnaceBlockEntity) entity;
        this.data = data;

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            this.addSlot(new SlotItemHandler(iItemHandler, 0, 92, 37));
            this.addSlot(new SlotItemHandler(iItemHandler, 1, 92, 65));
            this.addSlot(new SlotItemHandler(iItemHandler, 2, 140, 51) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            });
            this.addSlot(new SlotItemHandler(iItemHandler, 3, 186, 30));
            this.addSlot(new SlotItemHandler(iItemHandler, 4, 186, 48));
            this.addSlot(new SlotItemHandler(iItemHandler, 5, 186, 66));
            this.addSlot(new SlotItemHandler(iItemHandler, 6, 186, 84));
        });

        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 133 + i * 19));

        for (int k = 0; k < 9; ++k)
            this.addSlot(new Slot(inv, k, 44 + k * 19, 196));
        addDataSlots(data);
    }

    public int getScaledProgress() {
        int progress = this.data.get(1);
        int maxProgress = this.data.get(2);
        if (maxProgress == 0 || progress == 0) return 0;
        return Math.min(28, (progress * 28 + maxProgress - 1) / maxProgress);
    }

    public int getScaledHeat() {
        int heat = this.data.get(0);
        int maxHeat = 4000;
        return heat != 0 ? Math.min(73, heat * 73 / maxHeat) : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        if (index < 7) {
            if (!moveItemStackTo(sourceStack, 7, 43, true)) return ItemStack.EMPTY;
        }

        else if (index < 43) {

            if (!moveItemStackTo(sourceStack, 0, 2, false)) {

                if (!moveItemStackTo(sourceStack, 3, 7, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();

        sourceSlot.onTake(playerIn, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.BLAST_FURNACE.get());
    }
}
