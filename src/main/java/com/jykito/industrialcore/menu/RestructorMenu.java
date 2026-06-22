package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.RestructorBlockEntity;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.custom.CatalystItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class RestructorMenu extends AbstractContainerMenu {
    public final RestructorBlockEntity blockEntity;
    private final ContainerData data;

    public RestructorMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    public RestructorMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.RESTRUCTOR_MENU.get(), pContainerId);
        this.blockEntity = (RestructorBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();

        this.addSlot(new SlotItemHandler(h, 0, 99, 59));

        this.addSlot(new SlotItemHandler(h, 1, 141, 59) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        this.addSlot(new SlotItemHandler(h, 2, 120, 82) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof CatalystItem;
            }
        });

        this.addSlot(new SlotItemHandler(h, 3, 185, 39) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() == ModItems.ITEM_PUSHER.get()
                    || stack.getItem() == ModItems.ITEM_PULLER.get();
            }
        });

        this.addSlot(new SlotItemHandler(h, 4, 185, 85) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() == ModItems.ITEM_PUSHER.get()
                    || stack.getItem() == ModItems.ITEM_PULLER.get();
            }
        });

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
        int energy     = (data.get(1) << 16) | (data.get(0) & 0xFFFF);
        int recipeCost = (data.get(3) << 16) | (data.get(2) & 0xFFFF);
        if (energy <= 0 || recipeCost <= 0) return 0;

        return (int) Math.min(17L, ((long) energy * 17 + recipeCost - 1) / recipeCost);
    }

    public int getEnergyAccumulated() {
        return (data.get(1) << 16) | (data.get(0) & 0xFFFF);
    }

    public int getMaxRecipeCost() {
        return (data.get(3) << 16) | (data.get(2) & 0xFFFF);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        itemstack = slotStack.copy();

        int beSlots  = 5;
        int invStart = beSlots;
        int invEnd   = invStart + 27;
        int hotEnd   = invEnd + 9;

        if (index < beSlots) {
            if (!this.moveItemStackTo(slotStack, invStart, hotEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(slotStack, itemstack);
        } else {
            if (slotStack.getItem() instanceof CatalystItem) {
                if (!this.moveItemStackTo(slotStack, 2, 3, false)) return ItemStack.EMPTY;
            } else if (slotStack.getItem() == ModItems.ITEM_PUSHER.get()
                    || slotStack.getItem() == ModItems.ITEM_PULLER.get()) {
                if (!this.moveItemStackTo(slotStack, 3, 5, false)) return ItemStack.EMPTY;
            } else if (index < invEnd) {
                if (!this.moveItemStackTo(slotStack, 0, beSlots, false)
                        && !this.moveItemStackTo(slotStack, invEnd, hotEnd, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(slotStack, 0, beSlots, false)
                        && !this.moveItemStackTo(slotStack, invStart, invEnd, false)) return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (slotStack.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, slotStack);
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.RESTRUCTOR.get());
    }
}
