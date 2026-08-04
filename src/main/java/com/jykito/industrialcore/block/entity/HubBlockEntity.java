package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.logistics.HubFaceMode;
import com.jykito.industrialcore.item.upgrade.HubUpgradeItem;
import com.jykito.industrialcore.menu.HubMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HubBlockEntity extends BlockEntity implements MenuProvider {

    public final boolean fluid;

    /** Режим каждой грани, индекс = Direction.get3DDataValue() (0..5). */
    public final HubFaceMode[] faceModes = new HubFaceMode[6];

    /** Связи в логистическую сеть (как у коннекторов). */
    public final Set<BlockPos> linkedPositions = new HashSet<>();

    /** По одному слоту улучшения на грань (слот i = грань i). */
    public final ItemStackHandler upgradeHandler = new ItemStackHandler(6) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof HubUpgradeItem;
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };

    public static final int FILTER_SLOTS = 9; // на грань (3x3)
    /** Ghost-фильтр ВВОДА (что хаб кладёт): индекс = грань*9 + слот. */
    public final ItemStackHandler filterHandler = new ItemStackHandler(6 * FILTER_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
    };
    /** Ghost-фильтр ВЫВОДА (что хаб тянет): индекс как у filterHandler. */
    public final ItemStackHandler filterOutHandler = new ItemStackHandler(6 * FILTER_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public int getSlotLimit(int slot) { return 1; }
    };
    /** Лимит на фильтр-слот ВВОДА (0 = без лимита). Индекс как у filterHandler. */
    public final int[] filterCaps = new int[6 * FILTER_SLOTS];

    /** Хэндлер фильтра по направлению: true = вывод, false = ввод. */
    public ItemStackHandler filter(boolean output) { return output ? filterOutHandler : filterHandler; }

    /** Фантом-фильтры ЖИДКОСТИ (fluid-хаб): индекс = грань*9 + слот. */
    public final FluidStack[] fluidFilterIn  = new FluidStack[6 * FILTER_SLOTS];
    public final FluidStack[] fluidFilterOut = new FluidStack[6 * FILTER_SLOTS];
    public FluidStack[] fluidFilter(boolean output) { return output ? fluidFilterOut : fluidFilterIn; }

    // пер-грань тумблеры
    public final boolean[] whitelist  = new boolean[6]; // default true
    public final boolean[] matchNbt   = new boolean[6];
    public final boolean[] roundRobin = new boolean[6];
    private final int[] rrIndex = new int[6];

    private final int[] tickCounter = new int[6];
    private static final int INTERVAL = 20;
    private static final int MAX_TRANSFER = 8;   // предметов за перенос
    private static final int MAX_MB = 500;       // мБ за перенос

    public HubBlockEntity(BlockPos pos, BlockState state, boolean fluid) {
        super(ModBlockEntities.ITEM_HUB_BE.get(), pos, state);
        this.fluid = fluid;
        Arrays.fill(faceModes, HubFaceMode.OFF);
        Arrays.fill(whitelist, true);
        Arrays.fill(fluidFilterIn, FluidStack.EMPTY);
        Arrays.fill(fluidFilterOut, FluidStack.EMPTY);
    }

    public void setFluidFilter(int face, int slot, boolean output, FluidStack fluid) {
        if (face < 0 || face >= 6 || slot < 0 || slot >= FILTER_SLOTS) return;
        fluidFilter(output)[face * FILTER_SLOTS + slot] = fluid;
        setChanged(); sync();
    }

    public void toggle(int face, int which) {
        if (face < 0 || face >= 6) return;
        switch (which) {
            case 0 -> whitelist[face]  = !whitelist[face];
            case 1 -> matchNbt[face]   = !matchNbt[face];
            case 2 -> roundRobin[face] = !roundRobin[face];
        }
        setChanged(); sync();
    }

    public HubFaceMode getMode(Direction d) { return faceModes[d.get3DDataValue()]; }

    public void setMode(Direction d, HubFaceMode m) {
        faceModes[d.get3DDataValue()] = m;
        setChanged(); sync();
    }

    public void cycleMode(Direction d) { setMode(d, getMode(d).next()); }

    /** Shift-клик: which 0 = переключить ВВОД, 1 = переключить ВЫВОД (даёт OFF/INPUT/OUTPUT/BOTH). */
    public void toggleDir(int face, int which) {
        if (face < 0 || face >= 6) return;
        faceModes[face] = (which == 0) ? faceModes[face].toggleInput() : faceModes[face].toggleOutput();
        setChanged(); sync();
    }

    public void addLink(BlockPos other)    { linkedPositions.add(other);    setChanged(); sync(); }
    public void removeLink(BlockPos other) { linkedPositions.remove(other); setChanged(); sync(); }

    public void clearAllLinks(Level level) {
        for (BlockPos linked : new HashSet<>(linkedPositions)) {
            BlockEntity be = level.getBlockEntity(linked);
            if (be instanceof LogisticsNodeBlockEntity n)  n.removeLink(worldPosition);
            else if (be instanceof HubBlockEntity h)       h.removeLink(worldPosition);
        }
        linkedPositions.clear();
        setChanged(); sync();
    }

    public void sync() {
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // ---- перенос (стейдж 1: только предметы, без фильтра/лимита) ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, HubBlockEntity be) {
        if (be.fluid) { fluidTick(level, pos, be); return; }
        for (Direction dir : Direction.values()) {
            int fi = dir.get3DDataValue();
            if (!be.faceModes[fi].hasOutput()) continue;

            ItemStack up = be.upgradeHandler.getStackInSlot(fi);
            int interval = INTERVAL, maxTransfer = MAX_TRANSFER;
            if (up.getItem() instanceof HubUpgradeItem u) { interval = u.getInterval(); maxTransfer = u.getMaxItems(); }

            if (++be.tickCounter[fi] < interval) continue;
            be.tickCounter[fi] = 0;

            IItemHandler source = itemHandlerAt(level, pos.relative(dir), dir.getOpposite());
            if (source == null) continue;

            List<ItemSink> sinks = be.collectItemSinks(level, fi); // fi исключается как приёмник (анти-петля)
            if (sinks.isEmpty()) continue;
            if (be.roundRobin[fi] && sinks.size() > 1)
                java.util.Collections.rotate(sinks, -(be.rrIndex[fi] % sinks.size()));

            for (int si = 0; si < source.getSlots(); si++) {
                ItemStack peek = source.getStackInSlot(si);
                if (peek.isEmpty() || !be.matchesOutput(fi, peek)) continue; // фильтр ВЫВОД-грани
                ItemStack sim = source.extractItem(si, maxTransfer, true);
                if (sim.isEmpty()) continue;
                int canInsert = distribute(sinks, sim, true);
                if (canInsert <= 0) continue;
                ItemStack real = source.extractItem(si, Math.min(canInsert, maxTransfer), false);
                if (real.isEmpty()) continue;
                distribute(sinks, real, false);
                be.rrIndex[fi]++;
                return;
            }
        }
    }

    private interface ItemSink { int insert(ItemStack stack, boolean sim); }

    /** Приёмники: свои ВВОД-грани (локально) + PULL-коннекторы + ВВОД-грани хабов по сети. */
    private List<ItemSink> collectItemSinks(Level level, int excludeFace) {
        List<ItemSink> result = new ArrayList<>();

        for (Direction d : Direction.values()) {
            final int f = d.get3DDataValue();
            if (f == excludeFace) continue; // не кормить обратно грань-источник (дуал-грань)
            if (getMode(d).hasInput())
                result.add((s, sim) -> insertInputFace(level, f, s, sim));
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>(linkedPositions);
        visited.addAll(linkedPositions);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            if (!level.isLoaded(cur)) continue;
            BlockEntity be = level.getBlockEntity(cur);
            if (be instanceof HubBlockEntity h && !h.fluid) {
                for (Direction d : Direction.values())
                    if (h.getMode(d).hasInput()) {
                        final int f = d.get3DDataValue();
                        result.add((s, sim) -> h.insertInputFace(level, f, s, sim));
                    }
            } else if (be instanceof LogisticsNodeBlockEntity node) {
                for (BlockPos lp : node.linkedPositions) if (visited.add(lp)) queue.add(lp);
            }
        }
        return result;
    }

    private static int distribute(List<ItemSink> sinks, ItemStack stack, boolean sim) {
        ItemStack rem = stack.copy(); int total = 0;
        for (ItemSink s : sinks) {
            if (rem.isEmpty()) break;
            int ins = s.insert(rem, sim);
            if (ins > 0) { total += ins; rem.shrink(ins); }
        }
        return total;
    }

    /** ВВОД: положить в подключённый инвентарь с учётом фильтра/лимита/резерва. Возвращает вставленное. */
    private int insertInputFace(Level level, int face, ItemStack stack, boolean sim) {
        if (!matchesInput(face, stack)) return 0;
        Direction d = Direction.from3DDataValue(face);
        IItemHandler dest = itemHandlerAt(level, worldPosition.relative(d), d.getOpposite());
        if (dest == null) return 0;

        int cap = capForItem(face, stack.getItem()); // -1 = без лимита
        int amount = stack.getCount();
        if (cap >= 0) {
            int cur = countInHandler(dest, stack);
            amount = Math.min(amount, Math.max(0, cap - cur));
            if (amount <= 0) return 0;
            return insertManaged(dest, stack, amount, 0, sim);
        }
        return insertManaged(dest, stack, amount, reservedEmptySlots(face, dest), sim);
    }

    /** Вставка до amount: сперва в слоты с тем же предметом, затем в пустые, оставив reservedEmpty пустых. */
    private static int insertManaged(IItemHandler dest, ItemStack stack, int amount, int reservedEmpty, boolean sim) {
        int inserted = 0;
        for (int i = 0; i < dest.getSlots() && inserted < amount; i++) {
            ItemStack in = dest.getStackInSlot(i);
            if (in.isEmpty() || !ItemStack.isSameItemSameTags(in, stack)) continue;
            ItemStack portion = stack.copy();
            portion.setCount(Math.min(amount - inserted, stack.getMaxStackSize()));
            ItemStack rem = dest.insertItem(i, portion, sim);
            inserted += portion.getCount() - rem.getCount();
        }
        int empties = 0;
        for (int i = 0; i < dest.getSlots(); i++) if (dest.getStackInSlot(i).isEmpty()) empties++;
        int usable = Math.max(0, empties - reservedEmpty);
        for (int i = 0; i < dest.getSlots() && inserted < amount && usable > 0; i++) {
            if (!dest.getStackInSlot(i).isEmpty()) continue;
            ItemStack portion = stack.copy();
            portion.setCount(Math.min(amount - inserted, stack.getMaxStackSize()));
            ItemStack rem = dest.insertItem(i, portion, sim);
            int put = portion.getCount() - rem.getCount();
            if (put > 0) { inserted += put; usable--; }
        }
        return inserted;
    }

    private static int countInHandler(IItemHandler h, ItemStack stack) {
        int n = 0;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, stack)) n += s.getCount();
        }
        return n;
    }

    /** Резерв пустых слотов: по 1 на каждый лимитный фильтр-предмет, которого сейчас нет в dest. */
    private int reservedEmptySlots(int face, IItemHandler dest) {
        int reserved = 0;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            int idx = face * FILTER_SLOTS + i;
            ItemStack f = filterHandler.getStackInSlot(idx);
            if (f.isEmpty() || filterCaps[idx] <= 0) continue;
            if (countInHandler(dest, f) == 0) reserved++;
        }
        return reserved;
    }

    /** Лимит для предмета на грани: >=0 — число, -1 — без лимита. */
    private int capForItem(int face, net.minecraft.world.item.Item item) {
        for (int i = 0; i < FILTER_SLOTS; i++) {
            int idx = face * FILTER_SLOTS + i;
            ItemStack f = filterHandler.getStackInSlot(idx);
            if (!f.isEmpty() && f.getItem() == item) return filterCaps[idx] > 0 ? filterCaps[idx] : -1;
        }
        return -1;
    }

    /** Фильтр ВВОДА грани (что хаб кладёт). */
    public boolean matchesInput(int face, ItemStack stack)  { return matches(face, stack, filterHandler); }
    /** Фильтр ВЫВОДА грани (что хаб тянет). */
    public boolean matchesOutput(int face, ItemStack stack) { return matches(face, stack, filterOutHandler); }

    /** whitelist/blacklist + опц. NBT; пустой фильтр = всё проходит. */
    private boolean matches(int face, ItemStack stack, ItemStackHandler h) {
        boolean any = false, matched = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack f = h.getStackInSlot(face * FILTER_SLOTS + i);
            if (f.isEmpty()) continue;
            any = true;
            if (f.getItem() == stack.getItem() && (!matchNbt[face] || ItemStack.isSameItemSameTags(f, stack))) { matched = true; break; }
        }
        if (!any) return true;
        return whitelist[face] ? matched : !matched;
    }

    // ---- жидкостный перенос (fluid-хаб) ----

    public boolean matchesFluidIn(int face, FluidStack f)  { return matchesFluid(face, f, fluidFilterIn); }
    public boolean matchesFluidOut(int face, FluidStack f) { return matchesFluid(face, f, fluidFilterOut); }

    private boolean matchesFluid(int face, FluidStack fluid, FluidStack[] arr) {
        boolean any = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            FluidStack f = arr[face * FILTER_SLOTS + i];
            if (f == null || f.isEmpty()) continue;
            any = true;
            if (f.getFluid() != fluid.getFluid()) continue;
            if (matchNbt[face]) {
                CompoundTag ft = f.getTag(), st = fluid.getTag();
                if (ft == null && st != null) continue;
                if (ft != null && !ft.equals(st)) continue;
            }
            return whitelist[face];
        }
        if (!any) return true;
        return !whitelist[face];
    }

    private interface FluidSink { int fill(FluidStack fluid, boolean sim); }

    private static void fluidTick(Level level, BlockPos pos, HubBlockEntity be) {
        for (Direction dir : Direction.values()) {
            int fi = dir.get3DDataValue();
            if (!be.faceModes[fi].hasOutput()) continue;

            ItemStack up = be.upgradeHandler.getStackInSlot(fi);
            int interval = INTERVAL, maxMb = MAX_MB;
            if (up.getItem() instanceof HubUpgradeItem u) { interval = u.getInterval(); maxMb = u.getMaxMb(); }

            if (++be.tickCounter[fi] < interval) continue;
            be.tickCounter[fi] = 0;

            IFluidHandler source = fluidHandlerAt(level, pos.relative(dir), dir.getOpposite());
            if (source == null) continue;

            List<FluidSink> sinks = be.collectFluidSinks(level, fi);
            if (sinks.isEmpty()) continue;
            if (be.roundRobin[fi] && sinks.size() > 1)
                java.util.Collections.rotate(sinks, -(be.rrIndex[fi] % sinks.size()));

            for (int t = 0; t < source.getTanks(); t++) {
                FluidStack f = source.getFluidInTank(t);
                if (f.isEmpty() || !be.matchesFluidOut(fi, f)) continue; // фильтр ВЫВОД-грани
                FluidStack sim = source.drain(new FluidStack(f.getFluid(), maxMb), IFluidHandler.FluidAction.SIMULATE);
                if (sim.isEmpty()) continue;
                int canFill = distributeFluid(sinks, sim, true);
                if (canFill <= 0) continue;
                FluidStack real = source.drain(new FluidStack(f.getFluid(), Math.min(canFill, maxMb)), IFluidHandler.FluidAction.EXECUTE);
                if (real.isEmpty()) continue;
                distributeFluid(sinks, real, false);
                be.rrIndex[fi]++;
                return;
            }
        }
    }

    /** Приёмники жидкости: свои ВВОД-грани (кроме источника) + PULL-жид-коннекторы + ВВОД-грани fluid-хабов по сети. */
    private List<FluidSink> collectFluidSinks(Level level, int excludeFace) {
        List<FluidSink> result = new ArrayList<>();

        for (Direction d : Direction.values()) {
            final int f = d.get3DDataValue();
            if (f == excludeFace) continue;
            if (getMode(d).hasInput())
                result.add((fs, sim) -> fillInputFaceFluid(level, f, fs, sim));
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>(linkedPositions);
        visited.addAll(linkedPositions);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            if (!level.isLoaded(cur)) continue;
            BlockEntity be = level.getBlockEntity(cur);
            if (be instanceof HubBlockEntity h && h.fluid) {
                for (Direction d : Direction.values())
                    if (h.getMode(d).hasInput()) {
                        final int f = d.get3DDataValue();
                        result.add((fs, sim) -> h.fillInputFaceFluid(level, f, fs, sim));
                    }
            } else if (be instanceof LogisticsNodeBlockEntity node) {
                for (BlockPos lp : node.linkedPositions) if (visited.add(lp)) queue.add(lp);
            }
        }
        return result;
    }

    /** ВВОД жидкости: залить в подключённый бак с учётом входного фильтра. */
    private int fillInputFaceFluid(Level level, int face, FluidStack fluid, boolean sim) {
        if (!matchesFluidIn(face, fluid)) return 0;
        Direction d = Direction.from3DDataValue(face);
        IFluidHandler dest = fluidHandlerAt(level, worldPosition.relative(d), d.getOpposite());
        if (dest == null) return 0;
        return dest.fill(fluid, sim ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
    }

    private static int distributeFluid(List<FluidSink> sinks, FluidStack fluid, boolean sim) {
        FluidStack rem = fluid.copy(); int total = 0;
        for (FluidSink s : sinks) {
            if (rem.isEmpty()) break;
            int f = s.fill(rem, sim);
            if (f > 0) { total += f; rem = new FluidStack(rem.getFluid(), rem.getAmount() - f); }
        }
        return total;
    }

    @Nullable
    private static IFluidHandler fluidHandlerAt(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        var cap = be.getCapability(ForgeCapabilities.FLUID_HANDLER, side);
        return cap.isPresent() ? cap.resolve().orElse(null) : null;
    }

    /** Старые сейвы (без BOTH): у OUTPUT-грани фильтр был во filterHandler — копируем его в выходной. */
    private void migrateOutputFilter() {
        for (int face = 0; face < 6; face++) {
            if (!faceModes[face].hasOutput()) continue;
            for (int i = 0; i < FILTER_SLOTS; i++) {
                int idx = face * FILTER_SLOTS + i;
                ItemStack f = filterHandler.getStackInSlot(idx);
                if (!f.isEmpty()) filterOutHandler.setStackInSlot(idx, f.copy());
            }
        }
    }

    private static CompoundTag writeFluidFilters(FluidStack[] arr) {
        CompoundTag t = new CompoundTag();
        for (int i = 0; i < arr.length; i++)
            if (arr[i] != null && !arr[i].isEmpty()) t.put("f" + i, arr[i].writeToNBT(new CompoundTag()));
        return t;
    }
    private static void readFluidFilters(FluidStack[] arr, CompoundTag t) {
        Arrays.fill(arr, FluidStack.EMPTY);
        for (int i = 0; i < arr.length; i++)
            if (t.contains("f" + i)) arr[i] = FluidStack.loadFluidStackFromNBT(t.getCompound("f" + i));
    }

    private static byte[] bools6(boolean[] a) { byte[] r = new byte[6]; for (int i = 0; i < 6; i++) r[i] = (byte) (a[i] ? 1 : 0); return r; }
    private static void read6(boolean[] a, byte[] r) { if (r.length == 6) for (int i = 0; i < 6; i++) a[i] = r[i] != 0; }

    @Nullable
    private static IItemHandler itemHandlerAt(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, side);
        return cap.isPresent() ? cap.resolve().orElse(null) : null;
    }

    private static int insertUpTo(IItemHandler h, ItemStack stack, boolean sim) {
        ItemStack rem = stack.copy(); int before = rem.getCount();
        for (int i = 0; i < h.getSlots(); i++) { rem = h.insertItem(i, rem, sim); if (rem.isEmpty()) break; }
        return before - rem.getCount();
    }

    // ---- меню / сохранение ----

    @Override public Component getDisplayName() {
        return Component.translatable(fluid ? "block.industrial_core.fluid_hub" : "block.industrial_core.item_hub");
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new HubMenu(id, inv, this);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putLongArray("links", linkedPositions.stream().mapToLong(BlockPos::asLong).toArray());
        byte[] modes = new byte[6];
        for (int i = 0; i < 6; i++) modes[i] = (byte) faceModes[i].ordinal();
        tag.putByteArray("modes", modes);
        tag.put("upgrades", upgradeHandler.serializeNBT());
        tag.put("filter", filterHandler.serializeNBT());
        tag.put("filterOut", filterOutHandler.serializeNBT());
        tag.put("ffIn", writeFluidFilters(fluidFilterIn));
        tag.put("ffOut", writeFluidFilters(fluidFilterOut));
        tag.putIntArray("caps", filterCaps.clone());
        tag.putByteArray("wl", bools6(whitelist));
        tag.putByteArray("nbt", bools6(matchNbt));
        tag.putByteArray("rr", bools6(roundRobin));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedPositions.clear();
        for (long l : tag.getLongArray("links")) linkedPositions.add(BlockPos.of(l));
        byte[] modes = tag.getByteArray("modes");
        if (modes.length == 6)
            for (int i = 0; i < 6; i++) faceModes[i] = HubFaceMode.values()[modes[i]];
        else Arrays.fill(faceModes, HubFaceMode.OFF);
        if (tag.contains("upgrades")) upgradeHandler.deserializeNBT(tag.getCompound("upgrades"));
        if (tag.contains("filter")) filterHandler.deserializeNBT(tag.getCompound("filter"));
        if (tag.contains("filterOut")) filterOutHandler.deserializeNBT(tag.getCompound("filterOut"));
        else migrateOutputFilter(); // старый сейв: фильтр OUTPUT-грани лежал в filterHandler
        readFluidFilters(fluidFilterIn, tag.getCompound("ffIn"));
        readFluidFilters(fluidFilterOut, tag.getCompound("ffOut"));
        int[] caps = tag.getIntArray("caps");
        if (caps.length == filterCaps.length) System.arraycopy(caps, 0, filterCaps, 0, caps.length);
        else Arrays.fill(filterCaps, 0);
        if (tag.contains("wl")) read6(whitelist, tag.getByteArray("wl"));
        if (tag.contains("nbt")) read6(matchNbt, tag.getByteArray("nbt"));
        if (tag.contains("rr")) read6(roundRobin, tag.getByteArray("rr"));
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = new CompoundTag(); saveAdditional(t); return t; }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
