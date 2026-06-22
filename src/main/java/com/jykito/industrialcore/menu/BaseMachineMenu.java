package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.BaseMachineBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseMachineMenu extends AbstractContainerMenu {
    public final BaseMachineBlockEntity blockEntity;
    private final ContainerData data;

    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_SLOT_COUNT = 6;
    private static final int PLAYER_INVENTORY_FIRST_SLOT_INDEX = TE_INVENTORY_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 36;

    public BaseMachineMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(pMenuType, pContainerId);
        this.blockEntity = (BaseMachineBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();
        this.addSlot(new SlotItemHandler(h, 0, 87, 59));
        this.addSlot(new SlotItemHandler(h, 1, 145, 59) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        int[] upgradeY = upgradeSlotY();
        for (int u = 0; u < 4; u++)
            this.addSlot(new SlotItemHandler(h, 2 + u, 189, upgradeY[u]));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 133 + i * 19));

        for (int k = 0; k < 9; k++)
            this.addSlot(new Slot(inv, k, 44 + k * 19, 196));

        addDataSlots(data);
    }

    protected int[] upgradeSlotY() { return new int[]{37, 55, 73, 91}; }

    public int getScaledProgress(int barWidth) {
        int progress    = this.data.get(4);
        int maxProgress = this.data.get(5);
        if (maxProgress == 0 || progress == 0) return 0;

        return Math.min(barWidth, (progress * barWidth + maxProgress - 1) / maxProgress);
    }

    public int getScaledEnergy() {
        int energy    = (data.get(1) << 16) | (data.get(0) & 0xFFFF);
        int maxEnergy = (data.get(3) << 16) | (data.get(2) & 0xFFFF);
        return maxEnergy != 0 && energy != 0 ? (int)((long) energy * 110 / maxEnergy) : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        if (index < TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_FIRST_SLOT_INDEX,
                    PLAYER_INVENTORY_FIRST_SLOT_INDEX + PLAYER_INVENTORY_SLOT_COUNT, true))
                return ItemStack.EMPTY;
        } else if (index < PLAYER_INVENTORY_FIRST_SLOT_INDEX + PLAYER_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + 1, false))
                if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX + 2, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false))
                    return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();

        sourceSlot.onTake(playerIn, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, blockEntity.getBlockState().getBlock());
    }
}
