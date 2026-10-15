package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.CultivatorBlockEntity;
import com.jykito.industrialcore.item.upgrade.CultivatorUpgradeItem;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import static com.jykito.industrialcore.block.entity.CultivatorBlockEntity.*;

/** Cultivator menu. Menu slots follow the machine slot order, so the menu slot index = the machine index. */
public class CultivatorMenu extends MachineMenu {

    /** T2, the "Rows" window: lines one under another, each with an input, an arrow, an output; module on the right, upgrades in a column. */
    public static final int T2_IN_X = 102, T2_OUT_X = 144, T2_ROW_Y = 37, T2_ROW_STEP = 20;
    public static final int T2_MODULE_X = 167, T2_MODULE_Y = 57, T2_UPG_X = 190, T2_UPG_Y = 36, T2_UPG_STEP = 18;

    public final CultivatorBlockEntity blockEntity;
    private final ContainerData data;
    private final int lanes;

    public CultivatorMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(6));
    }

    public CultivatorMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        this(ModMenuTypes.CULTIVATOR_MENU.get(), containerId, inv, entity, data, 1);
    }

    public static CultivatorMenu t2(int containerId, Inventory inv, FriendlyByteBuf buf) {
        return t2(containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(4 + 2 * T2_LANES));
    }

    public static CultivatorMenu t2(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        return new CultivatorMenu(ModMenuTypes.CULTIVATOR_T2_MENU.get(), containerId, inv, entity, data, T2_LANES);
    }

    private CultivatorMenu(MenuType<?> type, int containerId, Inventory inv, BlockEntity entity, ContainerData data, int lanes) {
        super(type, containerId);
        this.blockEntity = (CultivatorBlockEntity) entity;
        this.data = data;
        this.lanes = lanes;

        var h = this.blockEntity.getItemHandler();
        if (lanes == 1) {
            this.addSlot(new SlotItemHandler(h, 0, 102, 58));
            this.addSlot(outputSlot(h, 1, 144, 58));
            this.addSlot(new SlotItemHandler(h, SLOT_MODULE, 123, 77));
            for (int i = 0; i < 4; i++) this.addSlot(new SlotItemHandler(h, SLOT_UPG_FIRST + i, 94 + 20 * i, 32));
        } else {
            this.addSlot(new SlotItemHandler(h, 0, T2_IN_X, T2_ROW_Y));
            this.addSlot(outputSlot(h, 1, T2_OUT_X, T2_ROW_Y));
            this.addSlot(new SlotItemHandler(h, SLOT_MODULE, T2_MODULE_X, T2_MODULE_Y));
            for (int i = 0; i < 4; i++) this.addSlot(new SlotItemHandler(h, SLOT_UPG_FIRST + i, T2_UPG_X, T2_UPG_Y + T2_UPG_STEP * i));
            for (int lane = 1; lane < lanes; lane++) {
                int y = T2_ROW_Y + T2_ROW_STEP * lane;
                this.addSlot(new SlotItemHandler(h, inSlot(lane), T2_IN_X, y));
                this.addSlot(outputSlot(h, outSlot(lane), T2_OUT_X, y));
            }
        }

        addPlayerInventory(inv, 44, 133);

        addDataSlots(data);
    }

    private static SlotItemHandler outputSlot(net.neoforged.neoforge.items.IItemHandler h, int slot, int x, int y) {
        return new SlotItemHandler(h, slot, x, y) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        };
    }

    public int getLanes() { return lanes; }

    public int getEnergyStored() { return unpack(data.get(2), data.get(3)); }
    public int getMaxEnergy()    { return unpack(data.get(4), data.get(5)); }

    public int getScaledEnergy() {
        return scale(getEnergyStored(), getMaxEnergy(), 110);
    }

    public int getScaledProgress() { return getScaledProgress(0); }

    /** Line arrow: the first uses cells 0 and 1 (as T1), the others a pair after the energy. */
    public int getScaledProgress(int lane) {
        int i = lane == 0 ? 0 : 4 + 2 * lane;
        return scaleProgress(data.get(i), data.get(i + 1), 22);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        final int MACHINE_SLOTS = blockEntity.getItemHandler().getSlots();
        final int INV_END = MACHINE_SLOTS + 36;

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(sourceStack, MACHINE_SLOTS, INV_END, true)) return ItemStack.EMPTY;
        } else {
            if (sourceStack.getItem() instanceof CultivatorUpgradeItem) {
                if (!moveItemStackTo(sourceStack, SLOT_MODULE, SLOT_MODULE + 1, false)) return ItemStack.EMPTY;
            } else if (sourceStack.getItem() instanceof MachineUpgradeItem) {
                if (!moveItemStackTo(sourceStack, SLOT_UPG_FIRST, SLOT_UPG_LAST + 1, false)) return ItemStack.EMPTY;
            } else {
                // inputs are not contiguous (0, 7, 9): in turn, the first line first
                boolean moved = false;
                for (int lane = 0; lane < lanes && !sourceStack.isEmpty(); lane++)
                    moved |= moveItemStackTo(sourceStack, inSlot(lane), inSlot(lane) + 1, false);
                if (!moved) return ItemStack.EMPTY;
            }
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();
        sourceSlot.onTake(player, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
}
