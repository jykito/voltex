package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.industrialcore.item.upgrade.PortUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/** Late multiblock port window. ME ports have their own window, {@link MePortMenu}. */
public class ItemFluidPortMenu extends MachineMenu {

    /** Port grid 9x3 with step 18 (step 19 does not fit into the panel), the top-left item at (48, 32). */
    public static final int GRID_X = 48, GRID_Y = 32, GRID_STEP = 18;
    public static final int UPGRADE_X = 191, UPGRADE_Y = 94;

    private static final int PORT_SLOTS = PortUpgradeItem.MAX_SLOTS;
    private static final int UPGRADE_SLOT = PORT_SLOTS;
    private static final int PLAYER_START = PORT_SLOTS + 1;
    private static final int PLAYER_END = PLAYER_START + 36;

    /** Buttons: 0 mode, 1 auto-exchange. */
    public static final int BTN_MODE = 0, BTN_AUTO = 1;

    public final ItemFluidPortBlockEntity blockEntity;
    private final ContainerData data;

    public ItemFluidPortMenu(int id, Inventory inv, FriendlyByteBuf extra) {
        this(id, inv, (ItemFluidPortBlockEntity) inv.player.level().getBlockEntity(extra.readBlockPos()),
                new SimpleContainerData(ItemFluidPortBlockEntity.DATA_COUNT));
    }

    public ItemFluidPortMenu(int id, Inventory inv, ItemFluidPortBlockEntity be, ContainerData data) {
        super(ModMenuTypes.ITEM_FLUID_PORT_MENU.get(), id);
        this.blockEntity = be;
        this.data = data;

        for (int i = 0; i < PORT_SLOTS; i++) {
            final int index = i;
            addSlot(new SlotItemHandler(be.getItems(), i, GRID_X + (i % 9) * GRID_STEP, GRID_Y + (i / 9) * GRID_STEP) {
                @Override public boolean isActive() { return index < activeSlots() && !isFluidMode(); }
            });
        }
        addSlot(new SlotItemHandler(be.getUpgrade(), 0, UPGRADE_X, UPGRADE_Y));
        addPlayerInventory(inv, 44, 133);
        addDataSlots(data);
    }

    public boolean isOutput()    { return blockEntity.isOutput(); }
    /** Fluid only: the slot grid is hidden and the tank takes its place. */
    public boolean isFluidMode() { return data.get(0) == ItemFluidPortBlockEntity.MODE_FLUIDS; }
    /** Items + fluid: the grid is visible, the fluid volume as a line of text. */
    public boolean isBothMode()  { return data.get(0) == ItemFluidPortBlockEntity.MODE_BOTH; }
    public int getTier()         { return data.get(1); }
    public int activeSlots()     { return PortUpgradeItem.slots(getTier()); }
    public int getFluidAmount()  { return unpack(data.get(2), data.get(3)); }
    public int getFluidCapacity() { return PortUpgradeItem.fluid(getTier()); }
    public boolean isAuto()      { return data.get(4) == 1; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BTN_MODE) return blockEntity.toggleMode();
        if (id == BTN_AUTO) { blockEntity.toggleAuto(); return true; }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack ret = stack.copy();

        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof PortUpgradeItem) {
            if (!moveItemStackTo(stack, UPGRADE_SLOT, UPGRADE_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (!isOutput() && !isFluidMode()) {
            if (!moveItemStackTo(stack, 0, activeSlots(), false)) return ItemStack.EMPTY;
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
