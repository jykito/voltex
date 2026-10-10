package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.OreWasherBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class OreWasherMenu extends MachineMenu {
    public final OreWasherBlockEntity blockEntity;
    private final ContainerData data;

    public OreWasherMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(6));
    }

    public OreWasherMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ORE_WASHER_MENU.get(), pContainerId);
        this.blockEntity = (OreWasherBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();

        this.addSlot(new SlotItemHandler(h, 0, 92, 54));

        this.addSlot(new SlotItemHandler(h, 1, 149, 34) { @Override public boolean mayPlace(@NotNull ItemStack s) { return false; } });
        this.addSlot(new SlotItemHandler(h, 2, 149, 54) { @Override public boolean mayPlace(@NotNull ItemStack s) { return false; } });
        this.addSlot(new SlotItemHandler(h, 3, 149, 74) { @Override public boolean mayPlace(@NotNull ItemStack s) { return false; } });

        this.addSlot(new SlotItemHandler(h, 4, 189, 37));
        this.addSlot(new SlotItemHandler(h, 5, 189, 55));
        this.addSlot(new SlotItemHandler(h, 6, 189, 73));
        this.addSlot(new SlotItemHandler(h, 7, 189, 91));

        addPlayerInventory(inv, 44, 133);

        addDataSlots(data);
    }

    public int getScaledProgress(int barWidth) {
        return scaleProgress(data.get(0), data.get(1), barWidth);
    }

    public int getEnergyStored() { return unpack(data.get(2), data.get(3)); }
    public int getMaxEnergy()    { return unpack(data.get(4), data.get(5)); }

    public int getScaledEnergy() {
        return scale(getEnergyStored(), getMaxEnergy(), 110);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        final int MACHINE_SLOTS = 8;
        final int INV_END = MACHINE_SLOTS + 36;

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(sourceStack, MACHINE_SLOTS, INV_END, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(sourceStack, 0, 1, false)) {
                if (!moveItemStackTo(sourceStack, 4, 8, false)) {
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
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.ORE_WASHER.get());
    }
}
