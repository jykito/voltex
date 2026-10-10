package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.SolarPanelBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SolarPanelMenu extends MachineMenu {

    public final SolarPanelBlockEntity blockEntity;
    private final ContainerData data;

    public SolarPanelMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(7));
    }

    public SolarPanelMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.SOLAR_PANEL_MENU.get(), id);
        this.blockEntity = (SolarPanelBlockEntity) entity;
        this.data = data;
        addDataSlots(data);

        addPlayerInventory(inv, 44, 133);
    }

    public int getEnergyStored()   { return unpack(data.get(0), data.get(1)); }
    public int getMaxEnergy()      { return unpack(data.get(2), data.get(3)); }
    public int getCurrentGen()     { return unpack(data.get(4), data.get(5)); }
    public boolean isGenerating()  { return data.get(6) == 1; }

    public int getScaledEnergy(int barHeight) {
        int max = getMaxEnergy();
        if (max <= 0) return 0;
        return (int)((long) getEnergyStored() * barHeight / max);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
}
