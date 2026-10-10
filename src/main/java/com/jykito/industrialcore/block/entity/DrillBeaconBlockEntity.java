package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.item.custom.MinerLensItem;
import com.jykito.industrialcore.menu.DrillBeaconMenu;
import com.jykito.industrialcore.miner.DrillSites;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Буровой маяк: один раз сканирует 3×3 чанка под собой (от своей высоты до низа мира), запоминает руды послойно
 * (по секциям 16 блоков) и публикует состав выбранного слоя в реестр участков {@link DrillSites}.
 * <p>
 * Рудой считается блок из тега {@code forge:ores} — руды любых модов подхватываются сами. Скан идёт ~10 с
 * ({@link #SCAN_TICKS}), счётчиком палитры секции, без обхода каждого блока; незагруженный чанк он ждёт, а не грузит.
 * <p>
 * Линза открывает измерение: без линзы или с линзой Верхнего мира — только Верхний мир, Незер и Край — своими
 * линзами, универсальная — любое измерение, включая измерения других модов.
 */
public class DrillBeaconBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SCAN_TICKS = 200;

    public enum State { NO_LENS, SCANNING, WAITING, READY }

    private final ItemStackHandler lens = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return stack.getItem() instanceof MinerLensItem; }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override protected void onContentsChanged(int slot) { setChanged(); lensDirty = true; }
    };

    private boolean lensDirty = true, scanned;
    private State state = State.NO_LENS;
    private int sections, top, step, total, bandLo, bandHi, syncCd;
    /** Блоков руды по секциям (индекс от низа мира). */
    private final Map<Item, int[]> counts = new LinkedHashMap<>();

    /** Клиент: состав слоя для окна и состояние для BER. */
    private List<Item> clientItems = List.of();
    private List<Long> clientCounts = List.of();
    private long clientTotal;

    public DrillBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRILL_BEACON_BE.get(), pos, state);
    }

    public ItemStackHandler getLens() { return lens; }
    public State getState() { return state; }
    public List<Item> getClientItems() { return clientItems; }
    public List<Long> getClientCounts() { return clientCounts; }
    public long getClientTotal() { return clientTotal; }

    /** Пускает ли линза (или её отсутствие) в это измерение. */
    public static boolean lensAllows(ItemStack stack, ResourceKey<Level> dim) {
        MinerLensItem.LensType t = stack.getItem() instanceof MinerLensItem l ? l.getLensType() : null;
        if (t == MinerLensItem.LensType.ALL) return true;
        if (dim == Level.OVERWORLD) return t == null || t == MinerLensItem.LensType.OVERWORLD;
        if (dim == Level.NETHER) return t == MinerLensItem.LensType.NETHER;
        if (dim == Level.END) return t == MinerLensItem.LensType.END;
        return false;
    }

    public void tick(Level level) {
        if (lensDirty) {
            lensDirty = false;
            if (!lensAllows(lens.getStackInSlot(0), level.dimension())) {
                state = State.NO_LENS;
                unpublish();
                sync();
            } else if (scanned) {
                state = State.READY;
                publish();
            } else startScan(level);
        }
        if (state == State.SCANNING || state == State.WAITING) {
            scanTick(level);
            if (--syncCd <= 0) { syncCd = 20; sync(); }
        }
    }

    private void startScan(Level level) {
        sections = level.getSectionsCount();
        top = Math.max(0, Math.min(sections - 1, level.getSectionIndex(worldPosition.getY())));
        total = 9 * (top + 1);
        step = 0;
        counts.clear();
        scanned = false;
        state = State.SCANNING;
        sync();
    }

    private void scanTick(Level level) {
        int per = Math.max(1, (total + SCAN_TICKS - 1) / SCAN_TICKS);
        int baseX = (worldPosition.getX() >> 4) - 1, baseZ = (worldPosition.getZ() >> 4) - 1;
        for (int i = 0; i < per && step < total; i++) {
            final int s = step / 9, ci = step % 9;
            int cx = baseX + ci % 3, cz = baseZ + ci / 3;
            if (!level.hasChunk(cx, cz)) { state = State.WAITING; return; }
            state = State.SCANNING;
            LevelChunkSection sec = level.getChunk(cx, cz).getSection(s);
            if (!sec.hasOnlyAir()) sec.getStates().count((st, n) -> {
                if (!st.is(Tags.Blocks.ORES)) return;
                Item it = st.getBlock().asItem();
                if (it != Items.AIR) counts.computeIfAbsent(it, k -> new int[sections])[s] += n;
            });
            step++;
        }
        if (step >= total) {
            scanned = true;
            bandLo = 0; bandHi = top;
            state = State.READY;
            publish();
            setChanged();
        }
    }

    /** Состав выбранного слоя -> реестр участков и сводка для окна. */
    private void publish() {
        if (!(level instanceof ServerLevel sl) || !scanned) return;
        Map<Item, Long> w = new LinkedHashMap<>();
        long sum = 0;
        double ySum = 0;
        int minY = level.getMinBuildHeight();
        for (Map.Entry<Item, int[]> e : counts.entrySet()) {
            long n = 0;
            int[] a = e.getValue();
            for (int s = bandLo; s <= bandHi && s < a.length; s++) { n += a[s]; ySum += a[s] * (minY + 16.0 * s + 8); }
            if (n > 0) { w.put(e.getKey(), n); sum += n; }
        }
        int avgY = sum > 0 ? (int) Math.round(ySum / sum) : minY + 16 * bandLo;
        DrillSites.get(sl.getServer()).put(level.dimension(), worldPosition, new DrillSites.Site(w, avgY, sum));
        List<Map.Entry<Item, Long>> sorted = new ArrayList<>(w.entrySet());
        sorted.sort(Map.Entry.<Item, Long>comparingByValue().reversed());
        clientItems = new ArrayList<>(); clientCounts = new ArrayList<>();
        for (int i = 0; i < Math.min(6, sorted.size()); i++) { clientItems.add(sorted.get(i).getKey()); clientCounts.add(sorted.get(i).getValue()); }
        clientTotal = sum;
        sync();
    }

    public void unpublish() {
        if (level instanceof ServerLevel sl) DrillSites.get(sl.getServer()).remove(level.dimension(), worldPosition);
    }

    /** Кнопки окна: 0/1 — нижняя граница слоя вниз/вверх, 2/3 — верхняя, 4 — пересканировать. */
    public void onButton(int id) {
        if (level == null) return;
        if (id == 4) { if (state != State.NO_LENS) startScan(level); return; }
        if (!scanned) return;
        switch (id) {
            case 0 -> bandLo = Math.max(0, bandLo - 1);
            case 1 -> bandLo = Math.min(bandHi, bandLo + 1);
            case 2 -> bandHi = Math.max(bandLo, bandHi - 1);
            case 3 -> bandHi = Math.min(top, bandHi + 1);
            default -> { return; }
        }
        setChanged();
        if (state == State.READY) publish();
    }

    // ───────── окно ─────────

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> state.ordinal();
                case 1 -> step;
                case 2 -> total;
                case 3 -> bandLo;
                case 4 -> bandHi;
                case 5 -> top;
                case 6 -> level == null ? 0 : level.getMinBuildHeight();
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 7; }
    };

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.drill_beacon"); }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DrillBeaconMenu(id, inv, this, data);
    }

    // ───────── синк и сохранение ─────────

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(state == State.SCANNING ? 6.5 : 1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("lens", lens.serializeNBT());
        tag.putBoolean("scanned", scanned);
        tag.putInt("sections", sections);
        tag.putInt("top", top);
        tag.putInt("lo", bandLo);
        tag.putInt("hi", bandHi);
        ListTag list = new ListTag();
        counts.forEach((item, a) -> {
            CompoundTag c = new CompoundTag();
            c.putString("i", String.valueOf(ForgeRegistries.ITEMS.getKey(item)));
            c.putIntArray("n", a);
            list.add(c);
        });
        tag.put("counts", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lens.deserializeNBT(tag.getCompound("lens"));
        scanned = tag.getBoolean("scanned");
        sections = tag.getInt("sections");
        top = tag.getInt("top");
        bandLo = tag.getInt("lo");
        bandHi = tag.getInt("hi");
        counts.clear();
        for (Tag t : tag.getList("counts", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(c.getString("i")));
            if (item != null && item != Items.AIR) counts.put(item, c.getIntArray("n"));
        }
        lensDirty = true;   // после загрузки тик сам решит: опубликовать, сканировать или ждать линзу
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("state", state.ordinal());
        tag.putLong("total", clientTotal);
        ListTag list = new ListTag();
        for (int i = 0; i < clientItems.size(); i++) {
            CompoundTag c = new CompoundTag();
            c.putString("i", String.valueOf(ForgeRegistries.ITEMS.getKey(clientItems.get(i))));
            c.putLong("n", clientCounts.get(i));
            list.add(c);
        }
        tag.put("comp", list);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        state = State.values()[Math.max(0, Math.min(State.values().length - 1, tag.getInt("state")))];
        clientTotal = tag.getLong("total");
        List<Item> items = new ArrayList<>();
        List<Long> ns = new ArrayList<>();
        for (Tag t : tag.getList("comp", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            items.add(ForgeRegistries.ITEMS.getValue(new ResourceLocation(c.getString("i"))));
            ns.add(c.getLong("n"));
        }
        clientItems = items; clientCounts = ns;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) handleUpdateTag(pkt.getTag());
    }
}
