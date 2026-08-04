package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.EnergyBankBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EnergyBankMenu extends AbstractContainerMenu {
    public final EnergyBankBlockEntity blockEntity;
    private final ContainerData data;

    public EnergyBankMenu(int id, Inventory inv, FriendlyByteBuf extra) {
        this(id, inv, inv.player.level().getBlockEntity(extra.readBlockPos()), new SimpleContainerData(7));
    }

    public EnergyBankMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ENERGY_BANK_MENU.get(), id);
        this.blockEntity = (EnergyBankBlockEntity) entity;
        this.data = data;
        // слотов машины нет — только инвентарь игрока (раскладка как у станции)
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 133 + i * 19));
        for (int k = 0; k < 9; ++k)
            addSlot(new Slot(inv, k, 44 + k * 19, 196));
        addDataSlots(data);
    }

    public int getEnergy()    { return (data.get(1) << 16) | (data.get(0) & 0xFFFF); }
    public int getMaxEnergy() { return EnergyBankBlockEntity.CAPACITY; }
    public int getIoIn()      { return (data.get(3) << 16) | (data.get(2) & 0xFFFF); }
    public int getIoOut()     { return (data.get(5) << 16) | (data.get(4) & 0xFFFF); }
    public int getReserve()   { return data.get(6); }

    public int getScaledEnergy(int w) { int m = getMaxEnergy(); return m <= 0 ? 0 : (int) ((long) getEnergy() * w / m); }

    /** Резерв % приходит с клиента как id клика-кнопки (0..100 влезает в байт) — кастом-пакет не нужен. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id <= 100) { blockEntity.setReserve(id); return true; }
        return false;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; } // машины-слотов нет

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.ENERGY_BANK.get());
    }
}
