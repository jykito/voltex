package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.LiquefierBlockEntity;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class LiquefierMenu extends AbstractContainerMenu {

    public final LiquefierBlockEntity blockEntity;
    private final ContainerData data;

    public LiquefierMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(7));
    }

    public LiquefierMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.LIQUEFIER_MENU.get(), containerId);
        this.blockEntity = (LiquefierBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();

        this.addSlot(new SlotItemHandler(h, 0, 88, 42));
        this.addSlot(new SlotItemHandler(h, 1, 88, 64));

        this.addSlot(new SlotItemHandler(h, 2, 184, 29));
        this.addSlot(new SlotItemHandler(h, 3, 184, 47));
        this.addSlot(new SlotItemHandler(h, 4, 184, 65));
        this.addSlot(new SlotItemHandler(h, 5, 184, 83));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 43 + j * 19, 134 + i * 19));
        for (int k = 0; k < 9; k++)
            this.addSlot(new Slot(inv, k, 43 + k * 19, 197));

        addDataSlots(data);
    }

    public int getEnergyStored() {
        return (data.get(3) << 16) | (data.get(2) & 0xFFFF);
    }

    public int getMaxEnergy() {
        return (data.get(5) << 16) | (data.get(4) & 0xFFFF);
    }

    public int getScaledProgress() {
        int progress    = data.get(0);
        int maxProgress = data.get(1);
        if (maxProgress == 0 || progress == 0) return 0;
        return Math.min(33, (progress * 33 + maxProgress - 1) / maxProgress);
    }

    public int getScaledEnergy() {
        int energy    = (data.get(3) << 16) | (data.get(2) & 0xFFFF);
        int maxEnergy = (data.get(5) << 16) | (data.get(4) & 0xFFFF);
        return maxEnergy != 0 && energy != 0 ? (int)((long) energy * 110 / maxEnergy) : 0;
    }

    public int getHeat()    { return data.get(6); }
    public int getMaxHeat() { return 10000; }
    public int getScaledHeat(int size) {
        int heat = data.get(6);
        return heat <= 0 ? 0 : Math.min(size, heat * size / 10000);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        final int MACHINE_SLOTS = 6;
        final int INV_END = MACHINE_SLOTS + 36;

        if (index < MACHINE_SLOTS) {

            if (!moveItemStackTo(sourceStack, MACHINE_SLOTS, INV_END, true)) return ItemStack.EMPTY;
        } else {

            if (sourceStack.getItem() instanceof MachineUpgradeItem) {
                if (!moveItemStackTo(sourceStack, 2, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(sourceStack, 0, 2, false)) {
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
                player, ModBlocks.LIQUEFIER.get());
    }
}
