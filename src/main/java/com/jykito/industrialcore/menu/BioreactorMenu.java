package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.BioreactorBlockEntity;
import com.jykito.industrialcore.item.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/** Bioreactor window: the ore washer layout plus an additive slot above the input. Menu slots: 0 biomass, 1 additive, 2-4 outputs, 5-8 upgrades. */
public class BioreactorMenu extends MachineMenu {
    public final BioreactorBlockEntity blockEntity;
    private final ContainerData data;

    public BioreactorMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(8));
    }

    public BioreactorMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.BIOREACTOR_MENU.get(), pContainerId);
        this.blockEntity = (BioreactorBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();
        this.addSlot(new SlotItemHandler(h, BioreactorBlockEntity.SLOT_INPUT, 92, 64));
        this.addSlot(new SlotItemHandler(h, BioreactorBlockEntity.SLOT_ADDITIVE, 92, 44));

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

    public int getScaledProgress(int barWidth) { return scaleProgress(data.get(0), data.get(1), barWidth); }
    public int getEnergyStored() { return unpack(data.get(2), data.get(3)); }
    public int getMaxEnergy()    { return unpack(data.get(4), data.get(5)); }
    public int getScaledEnergy() { return scale(getEnergyStored(), getMaxEnergy(), 110); }
    public int getHeat()         { return data.get(6); }
    /** Heat threshold of the current recipe, 0 if there is no recipe. */
    public int getRequiredHeat() { return data.get(7); }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        final int MACHINE_SLOTS = 9;
        final int INV_END = MACHINE_SLOTS + 36;

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(sourceStack, MACHINE_SLOTS, INV_END, true)) return ItemStack.EMPTY;
        } else if (sourceStack.is(ModItems.BIOMASS.get())) {
            if (!moveItemStackTo(sourceStack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(sourceStack, 5, 9, false) && !moveItemStackTo(sourceStack, 1, 2, false)) {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();
        sourceSlot.onTake(playerIn, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.BIOREACTOR.get());
    }
}
