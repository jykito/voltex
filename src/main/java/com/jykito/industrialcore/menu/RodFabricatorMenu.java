package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.RodFabricatorBlockEntity;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class RodFabricatorMenu extends MachineMenu {
    public final RodFabricatorBlockEntity blockEntity;
    private final ContainerData data;

    public RodFabricatorMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(6));
    }

    public RodFabricatorMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ROD_FABRICATOR_MENU.get(), pContainerId);
        this.blockEntity = (RodFabricatorBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();

        this.addSlot(new SlotItemHandler(h, 0, 90, 44) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return blockEntity.isValidInput(0, stack);
            }
        });

        this.addSlot(new SlotItemHandler(h, 1, 90, 66) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return blockEntity.isValidInput(1, stack);
            }
        });

        this.addSlot(new SlotItemHandler(h, 2, 139, 55) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        this.addSlot(new SlotItemHandler(h, 3, 189, 37));
        this.addSlot(new SlotItemHandler(h, 4, 189, 55));
        this.addSlot(new SlotItemHandler(h, 5, 189, 73));
        this.addSlot(new SlotItemHandler(h, 6, 189, 91));

        addPlayerInventory(inv, 44, 133);

        addDataSlots(data);
    }

    public int getScaledProgress() {
        int progress = data.get(4);
        int maxProgress = data.get(5);
        return scaleProgress(progress, maxProgress, 29);
    }

    public int getMaxEnergy() { return unpack(data.get(2), data.get(3)); }

    public int getScaledEnergy() {
        return scale(getEnergyStored(), getMaxEnergy(), 110);
    }

    public int getEnergyStored() {
        return unpack(data.get(0), data.get(1));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        itemstack = slotStack.copy();

        final int MACHINE_END = 7;
        final int INV_START = 7, INV_END = 43;

        if (index < MACHINE_END) {

            if (!this.moveItemStackTo(slotStack, INV_START, INV_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(slotStack, itemstack);
        } else {
            if (blockEntity.isValidInput(0, slotStack)) {
                if (!this.moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (blockEntity.isValidInput(1, slotStack)) {
                if (!this.moveItemStackTo(slotStack, 1, 2, false)) return ItemStack.EMPTY;
            } else if (slotStack.getItem() instanceof MachineUpgradeItem) {
                if (!this.moveItemStackTo(slotStack, 3, 7, false)) return ItemStack.EMPTY;
            } else if (index < INV_START + 27) {

                if (!this.moveItemStackTo(slotStack, INV_START + 27, INV_END, false)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(slotStack, INV_START, INV_START + 27, false)) {
                return ItemStack.EMPTY;
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
                player, ModBlocks.ROD_FABRICATOR.get());
    }
}
