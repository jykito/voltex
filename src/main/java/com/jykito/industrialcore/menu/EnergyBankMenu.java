package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.EnergyBankBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EnergyBankMenu extends MachineMenu {
    public final EnergyBankBlockEntity blockEntity;
    private final ContainerData data;

    public EnergyBankMenu(int id, Inventory inv, FriendlyByteBuf extra) {
        this(id, inv, inv.player.level().getBlockEntity(extra.readBlockPos()), new SimpleContainerData(9));
    }

    public EnergyBankMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ENERGY_BANK_MENU.get(), id);
        this.blockEntity = (EnergyBankBlockEntity) entity;
        this.data = data;
        // no machine slots, only the player inventory (layout as on the station)
        addPlayerInventory(inv, 44, 133);
        addDataSlots(data);
    }

    public int getEnergy()    { return unpack(data.get(0), data.get(1)); }
    public int getMaxEnergy() { return unpack(data.get(7), data.get(8)); }
    public int getIoIn()      { return unpack(data.get(2), data.get(3)); }
    public int getIoOut()     { return unpack(data.get(4), data.get(5)); }
    public int getReserve()   { return data.get(6); }

    public int getScaledEnergy(int w) { int m = getMaxEnergy(); return m <= 0 ? 0 : (int) ((long) getEnergy() * w / m); }

    /** The reserve % comes from the client as the button click id (0..100 fits into a byte), no custom packet needed. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id <= 100) { blockEntity.setReserve(id); return true; }
        return false;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; } // no machine slots

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
}
