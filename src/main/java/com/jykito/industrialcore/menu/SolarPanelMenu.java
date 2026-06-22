package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.SolarPanelBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SolarPanelMenu extends AbstractContainerMenu {

    public final SolarPanelBlockEntity blockEntity;
    private final ContainerData data;

    public SolarPanelMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(6));
    }

    public SolarPanelMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.SOLAR_PANEL_MENU.get(), id);
        this.blockEntity = (SolarPanelBlockEntity) entity;
        this.data = data;
        addDataSlots(data);

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 132 + i * 19));

        for (int k = 0; k < 9; k++)
            addSlot(new Slot(inv, k, 44 + k * 19, 195));
    }

    public int getEnergyStored()   { return (data.get(1) << 16) | (data.get(0) & 0xFFFF); }
    public int getMaxEnergy()      { return (data.get(3) << 16) | (data.get(2) & 0xFFFF); }
    public int getCurrentGen()     { return data.get(4); }
    public boolean isGenerating()  { return data.get(5) == 1; }

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
