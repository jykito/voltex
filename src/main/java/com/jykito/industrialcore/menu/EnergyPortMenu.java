package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.EnergyPortBlockEntity;
import com.jykito.industrialcore.item.upgrade.PortUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class EnergyPortMenu extends MachineMenu {

    public static final int UPGRADE_X = 191, UPGRADE_Y = 94;

    public final EnergyPortBlockEntity blockEntity;
    private final ContainerData data;

    public EnergyPortMenu(int id, Inventory inv, FriendlyByteBuf extra) {
        this(id, inv, (EnergyPortBlockEntity) inv.player.level().getBlockEntity(extra.readBlockPos()), new SimpleContainerData(3));
    }

    public EnergyPortMenu(int id, Inventory inv, EnergyPortBlockEntity be, ContainerData data) {
        super(ModMenuTypes.ENERGY_PORT_MENU.get(), id);
        this.blockEntity = be;
        this.data = data;
        addSlot(new SlotItemHandler(be.getUpgrade(), 0, UPGRADE_X, UPGRADE_Y));
        addPlayerInventory(inv, 44, 133);
        addDataSlots(data);
    }

    public int getEnergy()    { return unpack(data.get(0), data.get(1)); }
    public int getTier()      { return data.get(2); }
    public int getMaxEnergy() { return PortUpgradeItem.energy(getTier()); }
    public int getScaledEnergy(int px) { return scale(getEnergy(), getMaxEnergy(), px); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack ret = stack.copy();

        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof PortUpgradeItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == ret.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return ret;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
}
