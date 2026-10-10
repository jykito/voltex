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

public abstract class BaseMachineMenu extends MachineMenu {
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

        addPlayerInventory(inv, 44, 133);

        addDataSlots(data);
    }

    protected int[] upgradeSlotY() { return new int[]{37, 55, 73, 91}; }

    public int getScaledProgress(int barWidth) {
        int progress    = this.data.get(4);
        int maxProgress = this.data.get(5);
        return scaleProgress(progress, maxProgress, barWidth);
    }

    public int getEnergyStored() { return unpack(data.get(0), data.get(1)); }
    public int getMaxEnergy()    { return unpack(data.get(2), data.get(3)); }

    public int getScaledEnergy() {
        return scale(getEnergyStored(), getMaxEnergy(), 110);
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
