package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.ElectricFurnaceBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ElectricFurnaceMenu extends AbstractContainerMenu {
    public final ElectricFurnaceBlockEntity blockEntity;
    private final ContainerData data;

    private static final int TE_INVENTORY_SLOT_COUNT = 6;
    private static final int PLAYER_INVENTORY_FIRST_SLOT_INDEX = TE_INVENTORY_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 36;

    public ElectricFurnaceMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(6));
    }

    public ElectricFurnaceMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ELECTRIC_FURNACE_MENU.get(), pContainerId);
        this.blockEntity = (ElectricFurnaceBlockEntity) entity;
        this.data = data;

        var iItemHandler = this.blockEntity.getItemHandler();

        this.addSlot(new SlotItemHandler(iItemHandler, 0, 87, 59));
        this.addSlot(new SlotItemHandler(iItemHandler, 1, 145, 59) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        this.addSlot(new SlotItemHandler(iItemHandler, 2, 189, 37));
        this.addSlot(new SlotItemHandler(iItemHandler, 3, 189, 55));
        this.addSlot(new SlotItemHandler(iItemHandler, 4, 189, 73));
        this.addSlot(new SlotItemHandler(iItemHandler, 5, 189, 91));

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 133 + i * 19));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inv, k, 44 + k * 19, 196));
        }
        addDataSlots(data);
    }

    public int getScaledProgress() {
        int progress = this.data.get(4);
        int maxProgress = this.data.get(5);
        if (maxProgress == 0 || progress == 0) return 0;
        return Math.min(24, (progress * 24 + maxProgress - 1) / maxProgress);
    }

    public int getScaledEnergy(int barWidth) {
        int energy = (data.get(1) << 16) | (data.get(0) & 0xFFFF);
        int maxEnergy = (data.get(3) << 16) | (data.get(2) & 0xFFFF);
        if (maxEnergy <= 0) return 0;
        return (int) ((long) energy * barWidth / maxEnergy);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        if (index < TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_FIRST_SLOT_INDEX, PLAYER_INVENTORY_FIRST_SLOT_INDEX + PLAYER_INVENTORY_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_FIRST_SLOT_INDEX + PLAYER_INVENTORY_SLOT_COUNT) {

            if (!moveItemStackTo(sourceStack, 0, 1, false)) {
                if (!moveItemStackTo(sourceStack, 2, 6, false)) {
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

        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, com.jykito.industrialcore.block.ModBlocks.ELECTRIC_FURNACE.get());
    }
}
