package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.FluidGeneratorBlockEntity;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class FluidGeneratorMenu extends AbstractContainerMenu {

    public final FluidGeneratorBlockEntity blockEntity;
    private final ContainerData data;

    public FluidGeneratorMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(6));
    }

    public FluidGeneratorMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FLUID_GENERATOR_MENU.get(), containerId);
        this.blockEntity = (FluidGeneratorBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();

        this.addSlot(new SlotItemHandler(h, 0, 100, 56));
        this.addSlot(new SlotItemHandler(h, 1, 140, 56));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 133 + i * 19));
        for (int k = 0; k < 9; k++)
            this.addSlot(new Slot(inv, k, 44 + k * 19, 196));

        addDataSlots(data);
    }

    public int getEnergyStored() { return (data.get(3) << 16) | (data.get(2) & 0xFFFF); }
    public int getMaxEnergy()    { return (data.get(5) << 16) | (data.get(4) & 0xFFFF); }

    public int getScaledEnergy() {
        int energy = getEnergyStored(), maxEnergy = getMaxEnergy();
        return maxEnergy != 0 && energy != 0 ? (int) ((long) energy * 110 / maxEnergy) : 0;
    }

    public int getScaledProgress() {
        return getScaledProgress(12);
    }

    public int getScaledProgress(int pixels) {
        int progress = data.get(0), maxProgress = data.get(1);
        if (maxProgress == 0 || progress == 0) return 0;
        return Math.min(pixels, (progress * pixels + maxProgress - 1) / maxProgress);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        final int MACHINE_SLOTS = 2;
        final int INV_END = MACHINE_SLOTS + 36;

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(sourceStack, MACHINE_SLOTS, INV_END, true)) return ItemStack.EMPTY;
        } else {

            if (sourceStack.getItem() instanceof MachineUpgradeItem) {
                if (!moveItemStackTo(sourceStack, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();
        sourceSlot.onTake(player, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
}
