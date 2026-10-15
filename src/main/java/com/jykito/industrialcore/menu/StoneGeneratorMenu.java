package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.StoneGeneratorBlockEntity;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class StoneGeneratorMenu extends MachineMenu {

    public final StoneGeneratorBlockEntity blockEntity;
    private final ContainerData data;

    public StoneGeneratorMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(8));
    }

    public StoneGeneratorMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.STONE_GENERATOR_MENU.get(), containerId);
        this.blockEntity = (StoneGeneratorBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();

        this.addSlot(new SlotItemHandler(h, 0, 120, 49) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        this.addSlot(new SlotItemHandler(h, 1, 61, 40));
        this.addSlot(new SlotItemHandler(h, 2, 61, 73));
        this.addSlot(new SlotItemHandler(h, 3, 179, 40));
        this.addSlot(new SlotItemHandler(h, 4, 179, 73));

        addPlayerInventory(inv, 44, 133);

        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) { blockEntity.cycleMode();    return true; }
        if (id == 1) { blockEntity.toggleRunning(); return true; }
        return false;
    }

    public int getMode()      { return data.get(6); }
    public boolean isRunning(){ return data.get(7) != 0; }

    public int getEnergyStored() { return unpack(data.get(2), data.get(3)); }
    public int getMaxEnergy()    { return unpack(data.get(4), data.get(5)); }

    public int getScaledEnergy() {
        return scale(getEnergyStored(), getMaxEnergy(), 110);
    }

    public int getScaledProgress() {
        int progress = data.get(0), maxProgress = data.get(1);
        return scaleProgress(progress, maxProgress, 9);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        final int MACHINE_SLOTS = 5;
        final int INV_END = MACHINE_SLOTS + 36;

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(sourceStack, MACHINE_SLOTS, INV_END, true)) return ItemStack.EMPTY;
        } else {

            if (sourceStack.getItem() instanceof MachineUpgradeItem) {
                if (!moveItemStackTo(sourceStack, 1, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
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
