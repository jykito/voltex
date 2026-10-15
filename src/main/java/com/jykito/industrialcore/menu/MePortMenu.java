package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.custom.MePortBlock;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.industrialcore.block.entity.MePortAccess;
import com.jykito.industrialcore.item.upgrade.PortUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ME port window, the "Console" layout (tools/gui_gen/me_port_gui2.js, K1).
 * <ul>
 *   <li>ME input like an AE2 interface: pairs of rows "config above stock". A config cell is a ghost: what to keep and
 *   how many; the stock below it is what the port has already taken from the network. To the right of each pair, the fluid config above its tank.</li>
 *   <li>Pattern port like a provider: pattern rows, below them the "sent to the machine" buffer, on the right the buttons
 *   (blocking, visibility in the pattern terminal, priority) and the buffer tanks.</li>
 *   <li>ME output: the queue to the network and its tank.</li>
 * </ul>
 * Config cells are invisible slots after the player inventory: the window gets them from the server, and a click on a cell
 * is sent by the screen as a normal click on such a slot, handled in {@link #clicked}.
 */
public class MePortMenu extends MachineMenu {

    // ── layout: cell coordinates on the art (the item is 1 px right and down) ──
    public static final int GRID_X = 47, SIDE_X = 227, STEP = 18;
    public static final int[] CFG_Y = {8, 47, 86}, STO_Y = {26, 65, 104}, GAP_Y = {44, 83, 122};
    public static final int[] PAT_Y = {8, 26, 44}, BUF_Y = {68, 86, 104};
    public static final int[] OUT_Y = {31, 49, 67};
    public static final int TALL_H = 242, SHORT_H = 224;

    private static final int SLOTS = PortUpgradeItem.MAX_SLOTS;

    /** Buttons: blocking, terminal visibility; ME input fluid: LMB on tank cell t is 10 + t, RMB is 13 + t. */
    public static final int BTN_BLOCKING = 1, BTN_SHOW = 2, BTN_FLUID_LEFT = 10, BTN_FLUID_RIGHT = 13;
    /** Priority limit: the number travels to the window in a 16-bit cell. */
    public static final int PRIORITY_LIMIT = 9999;

    public final ItemFluidPortBlockEntity blockEntity;
    public final MePortAccess me;
    public final MePortBlock.MeKind kind;
    private final ContainerData data;
    @Nullable private final ItemStackHandler config, patterns;
    /** Slot group bounds: port slots [0, upgradeSlot), upgrade, player inventory, config ghosts. */
    public final int upgradeSlot, playerStart, playerEnd, cfgStart;

    public MePortMenu(int id, Inventory inv, FriendlyByteBuf extra) {
        this(id, inv, (ItemFluidPortBlockEntity) inv.player.level().getBlockEntity(extra.readBlockPos()), new SimpleContainerData(DATA_COUNT));
    }

    public MePortMenu(int id, Inventory inv, ItemFluidPortBlockEntity be) {
        this(id, inv, be, serverData(be));
    }

    private MePortMenu(int id, Inventory inv, ItemFluidPortBlockEntity be, ContainerData data) {
        super(ModMenuTypes.ME_PORT_MENU.get(), id);
        this.blockEntity = be;
        this.me = (MePortAccess) be;
        this.kind = ((MePortBlock) be.getBlockState().getBlock()).meKind();
        this.data = data;
        this.config = me.meConfig();
        this.patterns = me.mePatterns();
        boolean tall = kind != MePortBlock.MeKind.OUTPUT;

        if (kind == MePortBlock.MeKind.INPUT) {
            grid(be.getItems(), STO_Y, true);
        } else if (kind == MePortBlock.MeKind.PATTERN) {
            grid(patterns, PAT_Y, true);
            // "sent to the machine" buffer: can be taken out (if a job got stuck), cannot be put into
            for (int i = 0; i < SLOTS; i++) {
                final int index = i;
                addSlot(new SlotItemHandler(be.getItems(), i, GRID_X + 1 + (i % 9) * STEP, BUF_Y[i / 9] + 1) {
                    @Override public boolean isActive() { return index < activeSlots(); }
                    @Override public boolean mayPlace(@NotNull ItemStack s) { return false; }
                });
            }
        } else {
            grid(be.getItems(), OUT_Y, true);
        }
        upgradeSlot = slots.size();
        addSlot(new SlotItemHandler(be.getUpgrade(), 0, SIDE_X + 1, tall ? 127 : 91));
        playerStart = slots.size();
        addPlayerInventory(inv, 44, tall ? 159 : 133);
        playerEnd = slots.size();
        cfgStart = slots.size();
        if (config != null)
            for (int i = 0; i < SLOTS; i++)
                addSlot(new SlotItemHandler(config, i, -10000, -10000) {
                    @Override public boolean isActive() { return false; }
                    @Override public boolean mayPickup(Player p) { return false; }
                    @Override public boolean mayPlace(@NotNull ItemStack s) { return false; }
                });
        addDataSlots(data);
    }

    /** 27 slots in a 9x3 grid by rows {@code ys}; the ones the upgrade opened are open. */
    private void grid(ItemStackHandler handler, int[] ys, boolean open) {
        for (int i = 0; i < SLOTS; i++) {
            final int index = i;
            addSlot(new SlotItemHandler(handler, i, GRID_X + 1 + (i % 9) * STEP, ys[i / 9] + 1) {
                @Override public boolean isActive() { return open && index < activeSlots(); }
            });
        }
    }

    // ── window data: tier, network status, blocking, terminal visibility, priority ──

    public static final int DATA_COUNT = 5;

    private static ContainerData serverData(ItemFluidPortBlockEntity be) {
        MePortAccess me = (MePortAccess) be;
        return new ContainerData() {
            @Override public int get(int i) {
                return switch (i) {
                    case 0 -> be.tier();
                    case 1 -> me.meStatus();
                    case 2 -> me.meBlocking() ? 1 : 0;
                    case 3 -> me.meShowInTerminal() ? 1 : 0;
                    case 4 -> me.mePriority();
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    public int activeSlots()       { return PortUpgradeItem.slots(data.get(0)); }
    public int tankCapacity()      { return PortUpgradeItem.fluid(data.get(0)); }
    /** Open tanks: one per open row. */
    public int openTanks()         { return Math.max(1, Math.min(me.maxTanks(), activeSlots() / 9)); }
    public boolean online()        { return data.get(1) == 2; }
    public boolean blocking()      { return data.get(2) == 1; }
    public boolean showInTerminal() { return data.get(3) == 1; }
    public int priority()          { return (short) data.get(4); }
    public ItemStack meConfig(int i) { return config == null ? ItemStack.EMPTY : config.getStackInSlot(i); }
    public FluidStack meFluidConfig(int t) { return me.meFluidConfig(t); }
    public int patternCount() {
        int n = 0;
        if (patterns != null) for (int i = 0; i < activeSlots(); i++) if (!patterns.getStackInSlot(i).isEmpty()) n++;
        return n;
    }

    // ── ME input config ──

    /**
     * Click on a config cell (invisible slot {@code cfgStart + i}), as in the AE2 interface:
     * LMB with an item sets as many as are in hand, with the same item adds; LMB with an empty hand clears;
     * RMB with an item sets or adds one, RMB with an empty hand removes one.
     */
    @Override
    public void clicked(int slotId, int button, @NotNull ClickType type, @NotNull Player player) {
        if (config != null && slotId >= cfgStart && slotId < cfgStart + SLOTS) {
            int i = slotId - cfgStart;
            if (i < activeSlots() && type == ClickType.PICKUP) clickConfig(i, button == 1);
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    private void clickConfig(int i, boolean right) {
        ItemStack hand = getCarried(), cur = config.getStackInSlot(i);
        boolean same = !cur.isEmpty() && ItemStack.isSameItemSameComponents(cur, hand);
        int add = right ? 1 : hand.getCount();
        if (hand.isEmpty()) {
            if (!right || cur.getCount() <= 1) config.setStackInSlot(i, ItemStack.EMPTY);
            else config.setStackInSlot(i, cur.copyWithCount(cur.getCount() - 1));
        } else if (same) config.setStackInSlot(i, cur.copyWithCount(Math.min(cur.getMaxStackSize(), cur.getCount() + add)));
        else config.setStackInSlot(i, hand.copyWithCount(Math.min(hand.getMaxStackSize(), add)));
    }

    /** Scroll wheel over a config cell: 0..26 items, 27..29 fluids. */
    public void adjustAmount(int index, int delta) {
        if (config != null && index >= 0 && index < activeSlots()) {
            ItemStack cur = config.getStackInSlot(index);
            if (!cur.isEmpty()) config.setStackInSlot(index, cur.copyWithCount(Math.max(1, Math.min(cur.getMaxStackSize(), cur.getCount() + delta))));
        } else if (index >= SLOTS && index - SLOTS < openTanks()) {
            int t = index - SLOTS;
            FluidStack cur = me.meFluidConfig(t);
            if (!cur.isEmpty()) me.setMeFluidConfig(t, cur.copyWithAmount(Math.max(1, Math.min(tankCapacity(), cur.getAmount() + delta))));
        }
    }

    /** Drag from JEI: an item sets a full stack, a fluid sets a bucket. */
    public void setGhost(int index, ItemStack item, FluidStack fluid) {
        if (config != null && index >= 0 && index < activeSlots() && !item.isEmpty())
            config.setStackInSlot(index, item.copyWithCount(item.getMaxStackSize()));
        else if (index >= SLOTS && index - SLOTS < openTanks() && !fluid.isEmpty())
            me.setMeFluidConfig(index - SLOTS, fluid.copyWithAmount(Math.min(1000, tankCapacity())));
    }

    public void setPriority(int value) {
        if (patterns != null) me.setMePriority(Math.max(-PRIORITY_LIMIT, Math.min(PRIORITY_LIMIT, value)));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BTN_BLOCKING && patterns != null) { me.toggleMeBlocking(); return true; }
        if (id == BTN_SHOW && patterns != null) { me.toggleMeShowInTerminal(); return true; }
        boolean right = id >= BTN_FLUID_RIGHT;
        int t = id - (right ? BTN_FLUID_RIGHT : BTN_FLUID_LEFT);
        if (config != null && id >= BTN_FLUID_LEFT && id < BTN_FLUID_RIGHT + 3 && t < openTanks()) {
            clickFluid(t, right);
            return true;
        }
        return false;
    }

    /** Click on the fluid config: a container sets or adds its volume (RMB by 1000 mB), LMB with an empty hand clears, RMB removes 1000. */
    private void clickFluid(int t, boolean right) {
        FluidStack hand = FluidUtil.getFluidContained(getCarried()).orElse(FluidStack.EMPTY), cur = me.meFluidConfig(t);
        int cap = tankCapacity();
        if (hand.isEmpty()) {
            if (!right || cur.getAmount() <= 1000) me.setMeFluidConfig(t, FluidStack.EMPTY);
            else me.setMeFluidConfig(t, cur.copyWithAmount(cur.getAmount() - 1000));
            return;
        }
        int add = right ? 1000 : hand.getAmount();
        if (FluidStack.isSameFluidSameComponents(cur, hand)) me.setMeFluidConfig(t, cur.copyWithAmount(Math.min(cap, cur.getAmount() + add)));
        else me.setMeFluidConfig(t, hand.copyWithAmount(Math.min(cap, add)));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || index >= playerEnd) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack ret = stack.copy();

        if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof PortUpgradeItem) {
            if (!moveItemStackTo(stack, upgradeSlot, upgradeSlot + 1, false)) return ItemStack.EMPTY;
        } else if (config != null) {
            // ME input: Shift on an item puts a ghost into the first free cell (as many as in the stack), the item itself stays with the player
            for (int i = 0; i < activeSlots(); i++)
                if (config.getStackInSlot(i).isEmpty()) { config.setStackInSlot(i, stack.copy()); break; }
            return ItemStack.EMPTY;
        } else if (patterns != null) {
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
