package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.ThermostationBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ThermostationMenu extends AbstractContainerMenu {
    public final ThermostationBlockEntity blockEntity;
    private final ContainerData data;

    private static final int TE_SLOTS = 1;
    private static final int PLAYER_FIRST = TE_SLOTS;
    private static final int PLAYER_COUNT = 36;

    public ThermostationMenu(int id, Inventory inv, FriendlyByteBuf extra) {
        this(id, inv, inv.player.level().getBlockEntity(extra.readBlockPos()), new SimpleContainerData(10));
    }

    public ThermostationMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.THERMOSTATION_MENU.get(), id);
        this.blockEntity = (ThermostationBlockEntity) entity;
        this.data = data;

        var handler = this.blockEntity.getItemHandler();
        this.addSlot(new SlotItemHandler(handler, 0, 120, 63) {          // x2-апгрейд слот — пока ЗАЛОЧЕН
            @Override public boolean mayPlace(@NotNull ItemStack s) { return false; }
        });

        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 133 + i * 19));
        for (int k = 0; k < 9; ++k)
            this.addSlot(new Slot(inv, k, 44 + k * 19, 196));

        addDataSlots(data);
    }

    public int getEnergy()    { return (data.get(1) << 16) | (data.get(0) & 0xFFFF); }
    public int getHeat()      { return (data.get(3) << 16) | (data.get(2) & 0xFFFF); }
    public int getMaxHeat()   { return (data.get(5) << 16) | (data.get(4) & 0xFFFF); }
    public int getGenRate()   { return data.get(6); }
    public int getRadiators() { return data.get(7); }
    public boolean isWorking(){ return data.get(8) != 0; }
    public int getMaxOutput() { return data.get(9); }

    public int getScaledEnergy(int barW) {
        int max = ThermostationBlockEntity.ENERGY_CAP;
        return max <= 0 ? 0 : (int) ((long) getEnergy() * barW / max);
    }
    public int getScaledHeat(int barW) {
        int max = getMaxHeat();
        return max <= 0 ? 0 : (int) ((long) getHeat() * barW / max);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < TE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_FIRST, PLAYER_FIRST + PLAYER_COUNT, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, TE_SLOTS, false)) return ItemStack.EMPTY;
        }
        if (stack.getCount() == 0) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.THERMOSTATION_CORE.get());
    }
}
