package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.MinerCoreBlockEntity;
import com.jykito.industrialcore.item.upgrade.AcceleratorItem;
import com.jykito.industrialcore.item.custom.MinerLensItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class MinerCoreMenu extends AbstractContainerMenu {
    public final MinerCoreBlockEntity blockEntity;
    private final ContainerData data;

    public MinerCoreMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(11));
    }

    public MinerCoreMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.MINER_CORE_MENU.get(), containerId);
        this.blockEntity = (MinerCoreBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();

        this.addSlot(new SlotItemHandler(h, 0, 118, 59) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof MinerLensItem;
            }
        });

        int[] upgradeY = {38, 59, 80, 101};
        for (int i = 0; i < 4; i++) {
            final int slot = i + 1;
            this.addSlot(new SlotItemHandler(h, slot, 186, upgradeY[i]) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getItem() instanceof AcceleratorItem;
                }
            });
        }

        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 139 + i * 19));
        for (int k = 0; k < 9; ++k)
            this.addSlot(new Slot(inv, k, 44 + k * 19, 202));

        addDataSlots(data);
    }

    public int getTotalEnergy()    { return ((data.get(1) & 0xFFFF) << 16) | (data.get(0) & 0xFFFF); }
    public int getMaxTotalEnergy() { return ((data.get(3) & 0xFFFF) << 16) | (data.get(2) & 0xFFFF); }
    public int getTotalFluid()     { return ((data.get(5) & 0xFFFF) << 16) | (data.get(4) & 0xFFFF); }
    public int getMaxTotalFluid()  { return ((data.get(7) & 0xFFFF) << 16) | (data.get(6) & 0xFFFF); }
    public int getProgress()       { return data.get(8);  }
    public int getMaxProgress()    { return data.get(9);  }
    public boolean isStructureValid() { return data.get(10) == 1; }

    public int getScaledEnergy(int pixels) {
        int max = getMaxTotalEnergy();
        return max == 0 ? 0 : (int)((long) getTotalEnergy() * pixels / max);
    }
    public int getScaledFluid(int pixels) {
        int max = getMaxTotalFluid();
        return max == 0 ? 0 : (int)((long) getTotalFluid() * pixels / max);
    }
    public int getScaledProgress(int pixels) {
        int max = getMaxProgress();
        if (max == 0 || getProgress() == 0) return 0;
        return (int) Math.min((long) pixels, ((long) getProgress() * pixels + max - 1) / max);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        result = stack.copy();

        final int CONTAINER_END = 5;
        final int INV_START = 5, INV_END = 32;
        final int HOTBAR_START = 32, HOTBAR_END = 41;

        if (index < CONTAINER_END) {

            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof MinerLensItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof AcceleratorItem) {
            if (!moveItemStackTo(stack, 1, CONTAINER_END, false)) return ItemStack.EMPTY;
        } else if (index < HOTBAR_START) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, INV_START, INV_END, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.MINER_CORE.get());
    }
}
