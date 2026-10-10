package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.IndustrialMinerCoreBlock;
import com.jykito.industrialcore.block.custom.IndustrialMinerFrameBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.menu.IndustrialMinerMenu;
import com.jykito.industrialcore.miner.DrillSites;
import com.jykito.industrialcore.miner.IndustrialMinerStructure;
import com.jykito.industrialcore.item.custom.NaniteStrainItem;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.industrialcore.nanite.NaniteTrait;
import com.jykito.industrialcore.nanite.OreMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Промышленный шахтёр: добывает по составу привязанных буровых участков (до {@link #MAX_SITES} маяков).
 * <p>
 * Цикл {@link #CYCLE} тиков, за цикл до {@link #PARALLEL} руд; на каждую руду {@link #ENERGY_PER_ORE} FE (идут потиково,
 * сколько руд тянет энергия в этот тик) и {@link #FLUID_PER_ORE} мБ буровой жижи. Параллель делится между включёнными
 * участками поровну, внутри участка руда выбирается по её доле в слое. Мир не трогается — жилы не истощаются.
 * <p>
 * Штамм нанитов в слоте ядра: руда материала, к которому у штамма сродство, выходит с прибавкой (0.84 — ещё 0.84 руды
 * сверху в среднем), жижа тратится по множителю штамма. Нестабильный штамм изнашивается: после цикла с шансом
 * {@code (1 - стабильность) × }{@link #NANITE_WEAR_CHANCE} все его сродства теряют долю.
 */
public class IndustrialMinerCoreBlockEntity extends BlockEntity implements MenuProvider {

    public static final int CYCLE = 100;
    public static final int PARALLEL = 64;
    public static final int ENERGY_PER_ORE = 50_000;
    public static final int FLUID_PER_ORE = 250;
    public static final int MAX_SITES = 4;
    public static final int OUTPUT_SLOTS = 27;
    private static final int ENERGY_PER_ORE_TICK = ENERGY_PER_ORE / CYCLE;
    public static final float NANITE_WEAR_CHANCE = 0.05f, NANITE_WEAR = 0.97f;

    /** Что мешает работать — для строки статуса в окне. */
    public enum Status { WORKING, NOT_FORMED, NO_SITES, NO_DATA, NO_ENERGY, NO_FLUID, OUTPUT_FULL }

    /** Привязанный участок: где маяк и включён ли он. */
    public record Link(ResourceKey<Level> dim, BlockPos pos, boolean enabled) {}

    /** Сводка участка для окна (считается на сервере, уходит клиенту в теге обновления). */
    public record SiteView(ResourceKey<Level> dim, BlockPos pos, boolean enabled, boolean hasData, long total, int avgY,
                           List<Item> top, List<Integer> topPercent) {}

    private final MachineEnergy energy = MachineEnergy.consumer(2_000_000, 131_072);

    private final FluidTank tank = new FluidTank(64_000) {
        @Override public boolean isFluidValid(FluidStack stack) { return stack.getFluid() == ModFluids.SOURCE_DRILL_FLUID.get(); }
        @Override protected void onContentsChanged() { setChanged(); }
    };

    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        // сейвы с 9 слотами выхода: размер из NBT не опускает ниже OUTPUT_SLOTS, стопки остаются на своих местах
        @Override public void setSize(int size) { super.setSize(Math.max(size, OUTPUT_SLOTS)); }
    };

    /** Снаружи из выхода только забирают. */
    private final IItemHandler outputExternal = new IItemHandler() {
        @Override public int getSlots() { return output.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return output.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return output.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return output.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    };

    /** Штамм нанитов-добытчиков; только из окна, автоматизация его не видит. */
    private final ItemStackHandler nanites = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { genome = null; setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof NaniteStrainItem && NaniteGenome.kindOf(stack) == NaniteGenome.Kind.MINER;
        }
        @Override public int getSlotLimit(int slot) { return 1; }
    };
    /** Геном штамма из слота, читается при первом обращении после смены слота. */
    private @Nullable NaniteGenome genome;

    private final LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> energy);
    private final LazyOptional<IFluidHandler> lazyFluid = LazyOptional.of(() -> tank);
    private final LazyOptional<IItemHandler> lazyItems = LazyOptional.of(() -> outputExternal);

    private final List<Link> links = new ArrayList<>();
    private boolean formed;
    private int progress, minParallel = PARALLEL, lastParallel, lastOres;
    private Status status = Status.NOT_FORMED;
    private int checkCd, validCd, refreshCd, pushCd;

    /** Пулы включённых участков с данными: накопленные веса для выбора руды. Обновляются раз в 2 с. */
    private record Pool(Item[] items, String[] materials, long[] cumulative, int avgY) {}
    private final List<Pool> pools = new ArrayList<>();
    /** Соседи структуры снаружи: (позиция, сторона соседа) — для выхода предметов и тепла. */
    private final List<BlockPos> outside = new ArrayList<>();
    private final List<Direction> outsideSide = new ArrayList<>();

    /** Клиент: сводки участков из тега обновления и угол бура для BER. */
    private List<SiteView> clientSites = List.of();
    public float clientDrillAngle, clientLastTime = -1;

    public IndustrialMinerCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_MINER_CORE_BE.get(), pos, state);
    }

    public boolean isFormed() { return formed; }
    public ItemStackHandler getOutput() { return output; }
    public ItemStackHandler getNanites() { return nanites; }

    private NaniteGenome genome() {
        if (genome == null) genome = NaniteGenome.read(nanites.getStackInSlot(0));
        return genome;
    }

    /** Буровой жижи на руду с учётом штамма. */
    private int fluidPerOre() {
        return Math.max(1, Math.round(FLUID_PER_ORE * genome().fluid()));
    }
    public List<SiteView> getClientSites() { return clientSites; }
    private Direction facing() { return getBlockState().getValue(IndustrialMinerCoreBlock.FACING); }

    // ───────── сборка ─────────

    private void form(Level level) {
        formed = true;
        BlockPos core = getBlockPos();
        for (BlockPos p : IndustrialMinerStructure.frames(core, facing())) {
            BlockState s = level.getBlockState(p);
            level.setBlock(p, s.setValue(IndustrialMinerFrameBlock.FORMED, true), 3);
            if (level.getBlockEntity(p) instanceof IndustrialMinerFrameBlockEntity f) f.setCore(core);
        }
        level.setBlock(core, getBlockState().setValue(IndustrialMinerCoreBlock.FORMED, true), 3);
        collectOutside();
        refresh();
        setChanged();
    }

    /** Разобрать: каркас снова видим, ядро — обычный блок. Зовут проверка ядра, слом каркаса и слом ядра. */
    public void unform() {
        if (!formed || level == null) return;
        formed = false;
        status = Status.NOT_FORMED;
        lastParallel = 0;
        for (BlockPos p : IndustrialMinerStructure.frames(getBlockPos(), facing())) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof IndustrialMinerFrameBlock && s.getValue(IndustrialMinerFrameBlock.FORMED))
                level.setBlock(p, s.setValue(IndustrialMinerFrameBlock.FORMED, false), 3);
            if (level.getBlockEntity(p) instanceof IndustrialMinerFrameBlockEntity f) f.setCore(null);
        }
        BlockState own = level.getBlockState(getBlockPos());
        if (own.getBlock() instanceof IndustrialMinerCoreBlock)
            level.setBlock(getBlockPos(), own.setValue(IndustrialMinerCoreBlock.FORMED, false).setValue(IndustrialMinerCoreBlock.LIT, false), 3);
        setChanged();
    }

    private void collectOutside() {
        outside.clear(); outsideSide.clear();
        Set<BlockPos> inside = new HashSet<>(IndustrialMinerStructure.frames(getBlockPos(), facing()));
        inside.add(getBlockPos());
        for (BlockPos p : inside)
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (inside.contains(n)) continue;
                outside.add(n); outsideSide.add(d.getOpposite());
            }
    }

    // ───────── тик ─────────

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!formed) {
            if (--checkCd <= 0) {
                checkCd = 40;
                if (IndustrialMinerStructure.check(level, pos, facing()) == null) form(level);
            }
            return;
        }
        if (--validCd <= 0) {
            validCd = 10;
            if (IndustrialMinerStructure.check(level, pos, facing()) != null) { unform(); return; }
        }
        if (--refreshCd <= 0) { refreshCd = 40; refresh(); }
        if (--pushCd <= 0) { pushCd = 20; pushOutput(level); }

        status = pools.isEmpty() ? (links.isEmpty() ? Status.NO_SITES : Status.NO_DATA)
                : energy.getEnergyStored() < ENERGY_PER_ORE_TICK ? Status.NO_ENERGY
                : tank.getFluidAmount() < fluidPerOre() ? Status.NO_FLUID
                : outputFull() ? Status.OUTPUT_FULL : Status.WORKING;

        boolean working = status == Status.WORKING;
        if (working) {
            int p = Math.min(PARALLEL, energy.getEnergyStored() / ENERGY_PER_ORE_TICK);
            energy.consumeEnergy(p * ENERGY_PER_ORE_TICK);
            minParallel = Math.min(minParallel, p);
            lastParallel = p;
            if (++progress >= CYCLE) {
                int perOre = fluidPerOre();
                int n = Math.min(minParallel, tank.getFluidAmount() / perOre);
                int made = mine(level, n);
                if (made > 0) tank.drain(made * perOre, IFluidHandler.FluidAction.EXECUTE);
                if (made > 0) wearNanites(level);
                lastOres = made;
                progress = 0;
                minParallel = PARALLEL;
            }
            setChanged();
        } else {
            lastParallel = 0;
        }
        if (state.getValue(IndustrialMinerCoreBlock.LIT) != working)
            level.setBlock(pos, state.setValue(IndustrialMinerCoreBlock.LIT, working), 3);
    }

    /**
     * n руд поровну по участкам, внутри участка — по доле руды в слое. Возвращает, сколько руд добыто
     * (по ним тратится жижа); прибавка от штамма нанитов идёт сверху и жижи не просит.
     */
    private int mine(Level level, int n) {
        int made = 0, k = pools.size();
        NaniteGenome g = genome();
        for (int i = 0; i < k; i++) {
            Pool pool = pools.get(i);
            int share = n / k + (i < n % k ? 1 : 0);
            long total = pool.cumulative()[pool.cumulative().length - 1];
            for (int j = 0; j < share; j++) {
                long r = (long) (level.random.nextDouble() * total);
                int idx = Arrays.binarySearch(pool.cumulative(), r + 1);
                if (idx < 0) idx = -idx - 1;
                idx = Math.min(idx, pool.items().length - 1);
                float bonus = g.effect(pool.materials()[idx]);
                if (g.has(NaniteTrait.RESONATOR) && level.random.nextFloat() < NaniteTrait.RESONATOR_CHANCE) bonus *= 2;   // резонанс: прибавка дважды
                int count = 1 + (int) bonus + (level.random.nextFloat() < bonus - (int) bonus ? 1 : 0);
                ItemStack ore = new ItemStack(pool.items()[idx], count);
                for (int s = 0; s < output.getSlots() && !ore.isEmpty(); s++) ore = output.insertItem(s, ore, false);
                if (!ore.isEmpty()) return made;   // выход забит — дальше не мельчим
                made++;
            }
        }
        return made;
    }

    /** Износ штамма за цикл: чем ниже стабильность, тем чаще сродства теряют долю. */
    private void wearNanites(Level level) {
        ItemStack stack = nanites.getStackInSlot(0);
        if (stack.isEmpty()) return;
        NaniteGenome g = genome();
        if (!g.wears() || level.random.nextFloat() >= (1f - g.stability()) * NANITE_WEAR_CHANCE) return;
        NaniteGenome worn = g.copy();
        worn.decay(NANITE_WEAR);
        ItemStack out = stack.copy();
        worn.write(out);
        nanites.setStackInSlot(0, out);
    }

    private boolean outputFull() {
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack s = output.getStackInSlot(i);
            if (s.isEmpty() || s.getCount() < s.getMaxStackSize()) return false;
        }
        return true;
    }

    private void pushOutput(Level level) {
        boolean any = false;
        for (int i = 0; i < output.getSlots(); i++) if (!output.getStackInSlot(i).isEmpty()) { any = true; break; }
        if (!any) return;
        for (int i = 0; i < outside.size(); i++) {
            BlockEntity be = level.getBlockEntity(outside.get(i));
            if (be == null || be instanceof IndustrialMinerFrameBlockEntity || be == this) continue;
            IItemHandler inv = be.getCapability(ForgeCapabilities.ITEM_HANDLER, outsideSide.get(i)).resolve().orElse(null);
            if (inv == null) continue;
            for (int s = 0; s < output.getSlots(); s++) {
                ItemStack stack = output.getStackInSlot(s);
                if (stack.isEmpty()) continue;
                ItemStack rest = stack.copy();
                for (int t = 0; t < inv.getSlots() && !rest.isEmpty(); t++) rest = inv.insertItem(t, rest, false);
                int moved = stack.getCount() - rest.getCount();
                if (moved > 0) output.extractItem(s, moved, false);
            }
        }
    }

    // ───────── участки ─────────

    /** Пулы и сводки по реестру участков; если сводка для окна изменилась — отправить клиенту. */
    private void refresh() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return;
        DrillSites reg = DrillSites.get(sl.getServer());
        pools.clear();
        List<SiteView> views = new ArrayList<>(links.size());
        for (Link l : links) {
            DrillSites.Site site = reg.get(l.dim(), l.pos());
            boolean has = site != null && site.total() > 0;
            if (l.enabled() && has) {
                Item[] items = new Item[site.weights().size()];
                String[] materials = new String[items.length];
                long[] cum = new long[items.length];
                long acc = 0; int i = 0;
                for (Map.Entry<Item, Long> e : site.weights().entrySet()) {
                    items[i] = e.getKey(); materials[i] = OreMaterial.ore(e.getKey());
                    acc += e.getValue(); cum[i++] = acc;
                }
                if (acc > 0) pools.add(new Pool(items, materials, cum, site.avgY()));
            }
            views.add(view(l, site));
        }
        if (!views.equals(clientSites)) {
            clientSites = views;
            sync();
        }
    }

    private static SiteView view(Link l, @Nullable DrillSites.Site site) {
        List<Item> top = new ArrayList<>(3);
        List<Integer> pct = new ArrayList<>(3);
        if (site != null && site.total() > 0) {
            site.weights().entrySet().stream()
                    .sorted(Map.Entry.<Item, Long>comparingByValue().reversed()).limit(3)
                    .forEach(e -> { top.add(e.getKey()); pct.add((int) Math.round(100.0 * e.getValue() / site.total())); });
        }
        return new SiteView(l.dim(), l.pos(), l.enabled(), site != null, site == null ? 0 : site.total(), site == null ? 0 : site.avgY(), top, pct);
    }

    public enum LinkResult { ADDED, EXISTS, FULL }

    public LinkResult addSite(ResourceKey<Level> dim, BlockPos pos) {
        for (Link l : links) if (l.dim().equals(dim) && l.pos().equals(pos)) return LinkResult.EXISTS;
        if (links.size() >= MAX_SITES) return LinkResult.FULL;
        links.add(new Link(dim, pos, true));
        refresh();
        setChanged();
        return LinkResult.ADDED;
    }

    /** Кнопки окна: 0..3 — включить/выключить участок, 4..7 — отвязать. */
    public void onButton(int id) {
        int i = id % MAX_SITES;
        if (i >= links.size()) return;
        if (id < MAX_SITES) { Link l = links.get(i); links.set(i, new Link(l.dim(), l.pos(), !l.enabled())); }
        else links.remove(i);
        refresh();
        setChanged();
    }

    // ───────── окно ─────────

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> energy.getEnergyStored() & 0xFFFF;
                case 1 -> (energy.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energy.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energy.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> tank.getFluidAmount() & 0xFFFF;
                case 5 -> (tank.getFluidAmount() >> 16) & 0xFFFF;
                case 6 -> tank.getCapacity() & 0xFFFF;
                case 7 -> (tank.getCapacity() >> 16) & 0xFFFF;
                case 8 -> progress;
                case 9 -> CYCLE;
                case 10 -> lastParallel;
                case 11 -> lastOres;
                case 12 -> status.ordinal();
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 13; }
    };

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.industrial_miner_core"); }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new IndustrialMinerMenu(id, inv, this, data);
    }

    // ───────── возможности, синк, сохранение ─────────

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluid.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItems.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate(); lazyFluid.invalidate(); lazyItems.invalidate();
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(5);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    private static CompoundTag writeLink(ResourceKey<Level> dim, BlockPos pos, boolean enabled) {
        CompoundTag t = new CompoundTag();
        t.putString("dim", dim.location().toString());
        t.putLong("pos", pos.asLong());
        t.putBoolean("on", enabled);
        return t;
    }

    private static ResourceKey<Level> dimOf(CompoundTag t) {
        return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(t.getString("dim")));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("energy", energy.getEnergyStored());
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
        tag.put("output", output.serializeNBT());
        tag.put("nanites", nanites.serializeNBT());
        tag.putInt("progress", progress);
        tag.putBoolean("formed", formed);
        ListTag l = new ListTag();
        for (Link k : links) l.add(writeLink(k.dim(), k.pos(), k.enabled()));
        tag.put("links", l);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getInt("energy"));
        tank.readFromNBT(tag.getCompound("tank"));
        output.deserializeNBT(tag.getCompound("output"));
        if (tag.contains("nanites")) nanites.deserializeNBT(tag.getCompound("nanites"));
        genome = null;
        progress = tag.getInt("progress");
        formed = tag.getBoolean("formed");
        links.clear();
        for (Tag t : tag.getList("links", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            links.add(new Link(dimOf(c), BlockPos.of(c.getLong("pos")), c.getBoolean("on")));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (formed && level != null && !level.isClientSide()) collectOutside();
    }

    /** Клиенту — только сводки участков (энергия и прочее идут через окно). */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (SiteView v : clientSites) {
            CompoundTag t = writeLink(v.dim(), v.pos(), v.enabled());
            t.putBoolean("has", v.hasData());
            t.putLong("total", v.total());
            t.putInt("y", v.avgY());
            ListTag top = new ListTag();
            for (int i = 0; i < v.top().size(); i++) {
                CompoundTag o = new CompoundTag();
                o.putString("i", String.valueOf(ForgeRegistries.ITEMS.getKey(v.top().get(i))));
                o.putInt("p", v.topPercent().get(i));
                top.add(o);
            }
            t.put("top", top);
            list.add(t);
        }
        tag.put("views", list);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        clientSites = readViews(tag.getList("views", Tag.TAG_COMPOUND));
    }

    private static List<SiteView> readViews(ListTag list) {
        List<SiteView> out = new ArrayList<>(list.size());
        for (Tag t : list) {
            CompoundTag c = (CompoundTag) t;
            List<Item> top = new ArrayList<>();
            List<Integer> pct = new ArrayList<>();
            for (Tag o : c.getList("top", Tag.TAG_COMPOUND)) {
                CompoundTag oc = (CompoundTag) o;
                top.add(ForgeRegistries.ITEMS.getValue(new ResourceLocation(oc.getString("i"))));
                pct.add(oc.getInt("p"));
            }
            out.add(new SiteView(dimOf(c), BlockPos.of(c.getLong("pos")), c.getBoolean("on"), c.getBoolean("has"),
                    c.getLong("total"), c.getInt("y"), top, pct));
        }
        return out;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** По умолчанию пакет зовёт load() и обнулил бы клиентские поля — тут приходят только сводки. */
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) handleUpdateTag(pkt.getTag());
    }
}
