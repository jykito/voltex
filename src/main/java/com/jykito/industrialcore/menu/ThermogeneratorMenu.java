package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.ThermogeneratorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ThermogeneratorMenu extends MachineMenu {
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

        addPlayerInventory(inv, 44, 133);

        addDataSlots(data);
    }

    public int getHeat()    { return data.get(0); }
    public int getMaxHeat() { return 3000; }

    public int getScaledHeat(int barWidth) {
        int heat = getHeat();
        return heat > 0 ? heat * barWidth / getMaxHeat() : 0;
    }

    // The mask is required: ContainerData travels over the network as a short, so a low half
    // above 32767 arrives negative and without & 0xFFFF fills the high bits with ones.
    // The buffer is 20000 now and does not get there, but any capacity growth would break the screen.
    public int getEnergy()         { return unpack(data.get(1), data.get(2)); }
    public int getMaxEnergy()      { return unpack(data.get(3), data.get(4)); }
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
