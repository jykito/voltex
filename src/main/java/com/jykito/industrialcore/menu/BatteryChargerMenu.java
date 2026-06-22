package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.BatteryChargerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class BatteryChargerMenu extends AbstractContainerMenu {
    public final BatteryChargerBlockEntity blockEntity;
    private final ContainerData data;

    public BatteryChargerMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    public BatteryChargerMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.BATTERY_CHARGER_MENU.get(), pContainerId);
        this.blockEntity = (BatteryChargerBlockEntity) entity;
        this.data = data;

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            this.addSlot(new SlotItemHandler(iItemHandler, 0, 120, 62));
        });

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 43 + j * 19, 134 + i * 19));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inv, k, 43 + k * 19, 197));
        }
        addDataSlots(data);
    }

    public int getEnergy() {
        return (this.data.get(1) << 16) | (this.data.get(0) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (this.data.get(3) << 16) | (this.data.get(2) & 0xFFFF);
    }

    public int getScaledEnergy() {
        int energy = getEnergy();
        int maxEnergy = getMaxEnergy();
        return maxEnergy != 0 && energy != 0 ? (int)((long) energy * 110 / maxEnergy) : 0;
    }

    public int getScaledCharge(int barHeight) {
        if (this.slots.isEmpty()) return 0;
        ItemStack stack = this.slots.get(0).getItem();
        if (stack.isEmpty()) return 0;
        return stack.getCapability(ForgeCapabilities.ENERGY).map(e -> {
            int max = e.getMaxEnergyStored();
            return max <= 0 ? 0 : (int)((long) e.getEnergyStored() * barHeight / max);
        }).orElse(0);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        if (index < 1) {
            if (!moveItemStackTo(sourceStack, 1, 37, true)) return ItemStack.EMPTY;
        } else if (index < 37) {
            if (sourceStack.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
                if (!moveItemStackTo(sourceStack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();

        sourceSlot.onTake(playerIn, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.BATTERY_CHARGER.get());
    }
}
