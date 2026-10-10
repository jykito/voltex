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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Окно МЭ-портов, раскладка «Пульт» (tools/gui_gen/me_port_gui2.js, К1).
 * <ul>
 *   <li>МЭ-вход как интерфейс AE2: пары рядов «настройка над запасом». Клетка настройки — призрак: что держать и
 *   сколько; запас под ней — то, что порт уже взял из сети. Справа от каждой пары — настройка жидкости над её баком.</li>
 *   <li>Порт шаблонов как поставщик: ряды шаблонов, под ними буфер «отправлено в машину», справа кнопки
 *   (блокировка, видимость в терминале шаблонов, приоритет) и баки буфера.</li>
 *   <li>МЭ-выход: очередь в сеть и её бак.</li>
 * </ul>
 * Клетки настройки — невидимые слоты после инвентаря игрока: окно получает их с сервера, а клик по клетке
 * экран отправляет обычным кликом по такому слоту, разбор — в {@link #clicked}.
 */
public class MePortMenu extends MachineMenu {

    // ── раскладка: координаты клеток на арте (предмет на 1 px правее и ниже) ──
    public static final int GRID_X = 47, SIDE_X = 227, STEP = 18;
    public static final int[] CFG_Y = {8, 47, 86}, STO_Y = {26, 65, 104}, GAP_Y = {44, 83, 122};
    public static final int[] PAT_Y = {8, 26, 44}, BUF_Y = {68, 86, 104};
    public static final int[] OUT_Y = {31, 49, 67};
    public static final int TALL_H = 242, SHORT_H = 224;

    private static final int SLOTS = PortUpgradeItem.MAX_SLOTS;

    /** Кнопки: блокировка, видимость в терминале; жидкость МЭ-входа: ЛКМ по клетке бака t — 10 + t, ПКМ — 13 + t. */
    public static final int BTN_BLOCKING = 1, BTN_SHOW = 2, BTN_FLUID_LEFT = 10, BTN_FLUID_RIGHT = 13;
    /** Предел приоритета: число едет в окно 16-битной ячейкой. */
    public static final int PRIORITY_LIMIT = 9999;

    public final ItemFluidPortBlockEntity blockEntity;
    public final MePortAccess me;
    public final MePortBlock.MeKind kind;
    private final ContainerData data;
    @Nullable private final ItemStackHandler config, patterns;
    /** Границы групп слотов: слоты порта [0, upgradeSlot), улучшение, инвентарь игрока, призраки настройки. */
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
            // буфер «отправлено в машину»: забрать можно (если заказ застрял), положить нельзя
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

    /** 27 слотов сеткой 9×3 по рядам {@code ys}; открыты те, что открыло улучшение. */
    private void grid(ItemStackHandler handler, int[] ys, boolean open) {
        for (int i = 0; i < SLOTS; i++) {
            final int index = i;
            addSlot(new SlotItemHandler(handler, i, GRID_X + 1 + (i % 9) * STEP, ys[i / 9] + 1) {
                @Override public boolean isActive() { return open && index < activeSlots(); }
            });
        }
    }

    // ── данные окна: тир, статус сети, блокировка, видимость в терминале, приоритет ──

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
    /** Открыто баков: по одному на открытый ряд. */
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

    // ── настройка МЭ-входа ──

    /**
     * Клик по клетке настройки (невидимый слот {@code cfgStart + i}), как в интерфейсе AE2:
     * ЛКМ предметом задаёт столько, сколько в руке, тем же предметом прибавляет; ЛКМ пустой рукой снимает;
     * ПКМ предметом задаёт или прибавляет по одному, ПКМ пустой рукой убавляет по одному.
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
        boolean same = !cur.isEmpty() && ItemStack.isSameItemSameTags(cur, hand);
        int add = right ? 1 : hand.getCount();
        if (hand.isEmpty()) {
            if (!right || cur.getCount() <= 1) config.setStackInSlot(i, ItemStack.EMPTY);
            else config.setStackInSlot(i, cur.copyWithCount(cur.getCount() - 1));
        } else if (same) config.setStackInSlot(i, cur.copyWithCount(Math.min(cur.getMaxStackSize(), cur.getCount() + add)));
        else config.setStackInSlot(i, hand.copyWithCount(Math.min(hand.getMaxStackSize(), add)));
    }

    /** Колесо над клеткой настройки: 0..26 — предметы, 27..29 — жидкости. */
    public void adjustAmount(int index, int delta) {
        if (config != null && index >= 0 && index < activeSlots()) {
            ItemStack cur = config.getStackInSlot(index);
            if (!cur.isEmpty()) config.setStackInSlot(index, cur.copyWithCount(Math.max(1, Math.min(cur.getMaxStackSize(), cur.getCount() + delta))));
        } else if (index >= SLOTS && index - SLOTS < openTanks()) {
            int t = index - SLOTS;
            FluidStack cur = me.meFluidConfig(t);
            if (!cur.isEmpty()) me.setMeFluidConfig(t, new FluidStack(cur, Math.max(1, Math.min(tankCapacity(), cur.getAmount() + delta))));
        }
    }

    /** Перетаскивание из JEI: предмет — полная стопка, жидкость — ведро. */
    public void setGhost(int index, ItemStack item, FluidStack fluid) {
        if (config != null && index >= 0 && index < activeSlots() && !item.isEmpty())
            config.setStackInSlot(index, item.copyWithCount(item.getMaxStackSize()));
        else if (index >= SLOTS && index - SLOTS < openTanks() && !fluid.isEmpty())
            me.setMeFluidConfig(index - SLOTS, new FluidStack(fluid, Math.min(1000, tankCapacity())));
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

    /** Клик по настройке жидкости: ёмкостью задать или прибавить её объём (ПКМ — по 1000 mB), пустой рукой ЛКМ снять, ПКМ убавить на 1000. */
    private void clickFluid(int t, boolean right) {
        FluidStack hand = FluidUtil.getFluidContained(getCarried()).orElse(FluidStack.EMPTY), cur = me.meFluidConfig(t);
        int cap = tankCapacity();
        if (hand.isEmpty()) {
            if (!right || cur.getAmount() <= 1000) me.setMeFluidConfig(t, FluidStack.EMPTY);
            else me.setMeFluidConfig(t, new FluidStack(cur, cur.getAmount() - 1000));
            return;
        }
        int add = right ? 1000 : hand.getAmount();
        if (cur.isFluidEqual(hand)) me.setMeFluidConfig(t, new FluidStack(cur, Math.min(cap, cur.getAmount() + add)));
        else me.setMeFluidConfig(t, new FluidStack(hand, Math.min(cap, add)));
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
            // МЭ-вход: Shift по предмету — призрак в первую свободную клетку (сколько в стопке), сам предмет остаётся у игрока
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
