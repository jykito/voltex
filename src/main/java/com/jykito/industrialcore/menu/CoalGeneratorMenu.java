package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.CoalGeneratorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class CoalGeneratorMenu extends MachineMenu {
    public final CoalGeneratorBlockEntity blockEntity;
    private final ContainerData data;

    public CoalGeneratorMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(7));
    }

    public CoalGeneratorMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.COAL_GENERATOR_MENU.get(), pContainerId);
        this.blockEntity = (CoalGeneratorBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();
        this.addSlot(new SlotItemHandler(h, 0, 96, 45) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() == Items.COAL || stack.getItem() == Items.CHARCOAL || stack.getItem() == Items.COAL_BLOCK;
            }
        });

        addPlayerInventory(inv, 44, 133);

        addDataSlots(data);
    }

    public int getEnergyStored() { return unpack(data.get(0), data.get(1)); }
    public int getMaxEnergy()    { return unpack(data.get(2), data.get(3)); }

    public int getScaledEnergy() {
        return scale(getEnergyStored(), getMaxEnergy(), 42);
    }

    public int getBurnProgress()   { return data.get(4); }
    public int getMaxBurnTime()    { return data.get(5); }
    public int getEnergyPerTick()  { return data.get(6); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index == 0) {
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            }

            else {

                if (itemstack1.getItem() == Items.COAL || itemstack1.getItem() == Items.CHARCOAL || itemstack1.getItem() == Items.COAL_BLOCK) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }

                else if (index < 28) {
                    if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 37 && !this.moveItemStackTo(itemstack1, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.COAL_GENERATOR.get());
    }
}
