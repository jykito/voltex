package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.ThermogeneratorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class ThermogeneratorMenu extends AbstractContainerMenu {
    public final ThermogeneratorBlockEntity blockEntity;
    private final ContainerData data;

    public ThermogeneratorMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(6));
    }

    public ThermogeneratorMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.THERMOGENERATOR_MENU.get(), pContainerId);
        this.blockEntity = (ThermogeneratorBlockEntity) entity;
        this.data = data;

        this.addSlot(new SlotItemHandler(this.blockEntity.getItemHandler(), 0, 119, 60));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 133 + i * 19));

        for (int k = 0; k < 9; k++)
            this.addSlot(new Slot(inv, k, 44 + k * 19, 196));

        addDataSlots(data);
    }

    public int getScaledHeat(int barWidth) {
        int heat    = this.data.get(0);
        int maxHeat = 3000;
        return heat > 0 ? heat * barWidth / maxHeat : 0;
    }

    public int getEnergy()         { return (this.data.get(2) << 16) | this.data.get(1); }
    public int getMaxEnergy()      { return (this.data.get(4) << 16) | this.data.get(3); }
    public int getGenerationRate() { return this.data.get(5); }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        if (index < 1) {
            if (!moveItemStackTo(sourceStack, 1, 37, true)) return ItemStack.EMPTY;
        } else if (index < 37) {
            if (!moveItemStackTo(sourceStack, 0, 1, false)) return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();

        sourceSlot.onTake(playerIn, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.THERMOGENERATOR.get());
    }
}
