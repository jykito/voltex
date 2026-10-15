package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.RestructorBlockEntity;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.custom.CatalystItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import static com.jykito.industrialcore.block.entity.RestructorBlockEntity.*;

/** Restructor menu. Menu slots follow the machine slot order, so the menu slot index = the machine index. */
public class RestructorMenu extends MachineMenu {

    /** T2, the "Shared catalyst" window: three lines of input, charge, output; catalyst on the left, a vein to each input. */
    public static final int T2_IN_X = 99, T2_OUT_X = 141, T2_ROW_Y = 37, T2_ROW_STEP = 23, T2_CAT_X = 59, T2_CAT_Y = 60;

    public final RestructorBlockEntity blockEntity;
    private final ContainerData data;
    private final int lanes;

    public RestructorMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    public RestructorMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        this(ModMenuTypes.RESTRUCTOR_MENU.get(), pContainerId, inv, entity, data, 1);
    }

    public static RestructorMenu t2(int containerId, Inventory inv, FriendlyByteBuf buf) {
        return t2(containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(4 * T2_LANES));
    }

    public static RestructorMenu t2(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        return new RestructorMenu(ModMenuTypes.RESTRUCTOR_T2_MENU.get(), containerId, inv, entity, data, T2_LANES);
    }

    private RestructorMenu(MenuType<?> type, int pContainerId, Inventory inv, BlockEntity entity, ContainerData data, int lanes) {
        super(type, pContainerId);
        this.blockEntity = (RestructorBlockEntity) entity;
        this.data = data;
        this.lanes = lanes;

        var h = this.blockEntity.getItemHandler();

        if (lanes == 1) {
            this.addSlot(new SlotItemHandler(h, 0, 99, 59));
            this.addSlot(outputSlot(h, 1, 141, 59));
            this.addSlot(catalystSlot(h, 120, 82));
        } else {
            this.addSlot(new SlotItemHandler(h, 0, T2_IN_X, T2_ROW_Y));
            this.addSlot(outputSlot(h, 1, T2_OUT_X, T2_ROW_Y));
            this.addSlot(catalystSlot(h, T2_CAT_X, T2_CAT_Y));
        }
        this.addSlot(logisticsSlot(h, SLOT_UPG_FIRST, 185, 39));
        this.addSlot(logisticsSlot(h, SLOT_UPG_LAST, 185, 85));
        for (int lane = 1; lane < lanes; lane++) {
            int y = T2_ROW_Y + T2_ROW_STEP * lane;
            this.addSlot(new SlotItemHandler(h, inSlot(lane), T2_IN_X, y));
            this.addSlot(outputSlot(h, outSlot(lane), T2_OUT_X, y));
        }

        addPlayerInventory(inv, 44, 133);

        addDataSlots(data);
    }

    private static SlotItemHandler outputSlot(IItemHandler h, int slot, int x, int y) {
        return new SlotItemHandler(h, slot, x, y) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        };
    }

    private static SlotItemHandler catalystSlot(IItemHandler h, int x, int y) {
        return new SlotItemHandler(h, SLOT_CATALYST, x, y) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return stack.getItem() instanceof CatalystItem; }
        };
    }

    private static SlotItemHandler logisticsSlot(IItemHandler h, int slot, int x, int y) {
        return new SlotItemHandler(h, slot, x, y) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return isLogistics(stack); }
        };
    }

    private static boolean isLogistics(ItemStack stack) {
        return stack.getItem() == ModItems.ITEM_PUSHER.get() || stack.getItem() == ModItems.ITEM_PULLER.get();
    }

    public int getLanes() { return lanes; }

    public int getScaledProgress() { return getScaledProgress(0); }

    public int getScaledProgress(int lane) {
        int energy     = getEnergyAccumulated(lane);
        int recipeCost = getMaxRecipeCost(lane);
        if (energy <= 0 || recipeCost <= 0) return 0;

        return (int) Math.min(17L, ((long) energy * 17 + recipeCost - 1) / recipeCost);
    }

    public int getEnergyAccumulated() { return getEnergyAccumulated(0); }
    public int getMaxRecipeCost()     { return getMaxRecipeCost(0); }

    /** 4 cells per line: charge and cost, each in two halves. */
    public int getEnergyAccumulated(int lane) { return unpack(data.get(4 * lane), data.get(4 * lane + 1)); }
    public int getMaxRecipeCost(int lane)     { return unpack(data.get(4 * lane + 2), data.get(4 * lane + 3)); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        itemstack = slotStack.copy();

        int beSlots  = blockEntity.getItemHandler().getSlots();
        int invStart = beSlots;
        int invEnd   = invStart + 27;
        int hotEnd   = invEnd + 9;

        if (index < beSlots) {
            if (!this.moveItemStackTo(slotStack, invStart, hotEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(slotStack, itemstack);
        } else {
            if (slotStack.getItem() instanceof CatalystItem) {
                if (!this.moveItemStackTo(slotStack, SLOT_CATALYST, SLOT_CATALYST + 1, false)) return ItemStack.EMPTY;
            } else if (isLogistics(slotStack)) {
                if (!this.moveItemStackTo(slotStack, SLOT_UPG_FIRST, SLOT_UPG_LAST + 1, false)) return ItemStack.EMPTY;
            } else if (index < invEnd) {
                if (!toInputs(slotStack) && !this.moveItemStackTo(slotStack, invEnd, hotEnd, false)) return ItemStack.EMPTY;
            } else {
                if (!toInputs(slotStack) && !this.moveItemStackTo(slotStack, invStart, invEnd, false)) return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (slotStack.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, slotStack);
        return itemstack;
    }

    /** Into the line inputs in turn (on T1 across the whole machine slot block, as before: the slots filter out what does not fit). */
    private boolean toInputs(ItemStack stack) {
        if (lanes == 1) return this.moveItemStackTo(stack, 0, blockEntity.getItemHandler().getSlots(), false);
        boolean moved = false;
        for (int lane = 0; lane < lanes && !stack.isEmpty(); lane++)
            moved |= this.moveItemStackTo(stack, inSlot(lane), inSlot(lane) + 1, false);
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
}
