package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.custom.SolarPanelBlock;
import com.jykito.industrialcore.block.entity.PanelCombinerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class PanelCombinerMenu extends AbstractContainerMenu {

    public final PanelCombinerBlockEntity blockEntity;
    private final ContainerData data;

    public static final int PANEL_START_X = 52;
    public static final int PANEL_START_Y = 35;
    public static final int PANEL_STEP    = 17;
    public static final int PANEL_COLS    = 9;
    public static final int PANEL_ROWS    = 2;
    public static final int INV_START_X   = 44;
    public static final int INV_START_Y   = 153;
    public static final int INV_STEP      = 19;
    public static final int HOTBAR_Y      = 216;

    public PanelCombinerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(8));
    }

    public PanelCombinerMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.PANEL_COMBINER_MENU.get(), id);
        this.blockEntity = (PanelCombinerBlockEntity) entity;
        this.data = data;
        addDataSlots(data);

        var h = blockEntity.getItemHandler();

        for (int row = 0; row < PANEL_ROWS; row++) {
            for (int col = 0; col < PANEL_COLS; col++) {
                int slot = col + row * PANEL_COLS;
                addSlot(new SlotItemHandler(h, slot, PANEL_START_X + col * PANEL_STEP, PANEL_START_Y + row * PANEL_STEP));
            }
        }

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, INV_START_X + col * INV_STEP, INV_START_Y + row * INV_STEP));

        for (int k = 0; k < 9; k++)
            addSlot(new Slot(inv, k, INV_START_X + k * INV_STEP, HOTBAR_Y));
    }

    public int getEnergyStored()   { return (data.get(1) << 16) | (data.get(0) & 0xFFFF); }
    public int getMaxEnergy()      { return (data.get(3) << 16) | (data.get(2) & 0xFFFF); }
    public int getCurrentGen()     { return (data.get(5) << 16) | (data.get(4) & 0xFFFF); }
    public int getCurrentOutput()  { return (data.get(7) << 16) | (data.get(6) & 0xFFFF); }

    public int getScaledEnergy(int barWidth) {
        int max = getMaxEnergy();
        if (max <= 0) return 0;
        return (int)((long) getEnergyStored() * barWidth / max);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack out = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        out = stack.copy();

        int panelEnd = PanelCombinerBlockEntity.PANEL_SLOTS;
        int invEnd   = panelEnd + 27;
        int hotEnd   = invEnd + 9;

        if (index < panelEnd) {
            if (!this.moveItemStackTo(stack, panelEnd, hotEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, out);
        } else {
            boolean isPanel = stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof SolarPanelBlock;
            if (isPanel) {
                if (!this.moveItemStackTo(stack, 0, panelEnd, false)) return ItemStack.EMPTY;
            } else if (index < invEnd) {
                if (!this.moveItemStackTo(stack, invEnd, hotEnd, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, panelEnd, invEnd, false)) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == out.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return out;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
}
