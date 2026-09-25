package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.HeatPipeBlock;
import com.jykito.industrialcore.heat.HeatBuffer;
import com.jykito.industrialcore.heat.HeatCapability;
import com.jykito.industrialcore.heat.IHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HeatPipeBlockEntity extends BlockEntity {

    public static final int MAX_HEAT = 4000;
    private static final int THROUGHPUT = 128;    // макс переток за тик на одного соседа (T1)
    private static final int THROUGHPUT_T2 = 512; // T2 — выше пропуск, без потерь
    private static final int HOT_ON = 300, HOT_OFF = 50; // гистерезис blockstate-свечения (редкий toggle → мало ремеша)

    private final HeatBuffer buffer = new HeatBuffer(MAX_HEAT);
    private LazyOptional<IHeatHandler> lazyHeat = LazyOptional.empty();

    /** Вручную отключённые стороны (ключом): не растят рукав, не проводят и не принимают тепло. */
    private final boolean[] disabled = new boolean[6];

    /** Блокнот под позицию соседа: перезаписывается, а не создаётся заново на каждую сторону. */
    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    // Блок T2 после регистрации не меняется — достаём из RegistryObject один раз,
    // иначе .get() дёргался бы на каждую трубу каждый тик. Соседа-трубу узнаём
    // по классу (оба тира — HeatPipeBlock), поэтому второй ссылки не нужно.
    private static net.minecraft.world.level.block.Block PIPE_T2;
    private static void ensurePipeBlocks() {
        if (PIPE_T2 == null) PIPE_T2 = com.jykito.industrialcore.block.ModBlocks.HEAT_PIPE_T2.get();
    }

    // клиентское сглаживание яркости свечения (данные синкаются ступеньками — тут догоняем плавно)
    private float clientHeatPrev = 0f;
    private float clientHeatCurr = 0f;
    private boolean clientInit = false;

    public HeatPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAT_PIPE_BE.get(), pos, state);
    }

    @Override public void onLoad() { super.onLoad(); lazyHeat = LazyOptional.of(() -> buffer); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyHeat.invalidate(); }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == HeatCapability.HEAT && (side == null || !disabled[side.get3DDataValue()])) return lazyHeat.cast();
        return super.getCapability(cap, side);
    }

    /**
     * Тик, на котором сеть уже посчитана. Обработка идёт СЕТЬЮ, а не по соседям, поэтому первая
     * же труба считает всю сеть, а остальные в этот тик пропускают ход.
     */
    private long networkTick = -1;

    /**
     * Теплосеть считается целиком, одним котлом.
     * <p>
     * Раньше труба знала только шесть соседей и растекалась по градиенту, как теплопроводность.
     * Из-за симметрии приходилось передавать соседу лишь ПОЛОВИНУ разницы, иначе трубы гоняли бы
     * тепло друг в друга. Побочный эффект был неприятный: чтобы держать поток F, между трубами
     * нужен был перепад 2F, перепады складывались по трассе и упирались в размер буфера. Линия
     * из пяти труб физически не могла протащить больше 500 в тик, сколько бы веток ты ни развёл.
     * <p>
     * Теперь переливов между трубами нет вовсе. Всё тепло сети — общий котёл, и он раздаётся
     * потребителям по {@code THROUGHPUT} на каждое соединение. Длина трассы перестала значить
     * что-либо, «512 на соединение» стало буквальной правдой, а осцилляции неоткуда взяться.
     * <p>
     * По стоимости это дешевле прежнего: раньше каждая труба каждый тик лезла к шести соседям
     * за капабилити, теперь сеть обходится один раз.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, HeatPipeBlockEntity pipe) {
        long now = level.getGameTime();
        if (pipe.networkTick == now) return;   // сеть уже посчитана другой трубой в этот тик

        ensurePipeBlocks();

        java.util.List<HeatPipeBlockEntity> members = new java.util.ArrayList<>();
        java.util.List<IHeatHandler> sinks = new java.util.ArrayList<>();
        java.util.List<Integer> sinkRates = new java.util.ArrayList<>();

        collectNetwork(level, pipe, now, members, sinks, sinkRates);

        // 1. утечка: она у каждой трубы своя, T2 не течёт
        int pool = 0;
        for (HeatPipeBlockEntity m : members) {
            int h = m.buffer.getHeat();
            if (h > 0 && !m.getBlockState().is(PIPE_T2)) {
                int leak = h / 800;                    // ~0.125%/тик у T1
                if (leak > 0) { m.buffer.extract(leak); h -= leak; }
            }
            pool += h;
        }

        // 2. раздача потребителям: сперва поровну, потом остаток по кругу — чтобы одна машина
        //    в начале списка не выгребала весь котёл, пока остальные стоят
        int before = pool;
        if (pool > 0 && !sinks.isEmpty()) {
            int n = sinks.size();
            int[] given = new int[n];
            int share = pool / n;
            for (int i = 0; i < n && pool > 0; i++) {
                int want = Math.min(sinkRates.get(i), Math.min(share, pool));
                if (want <= 0) continue;
                int acc = sinks.get(i).receiveHeat(want, false);
                given[i] = acc; pool -= acc;
            }
            for (int i = 0; i < n && pool > 0; i++) {
                int want = Math.min(sinkRates.get(i) - given[i], pool);
                if (want <= 0) continue;
                pool -= sinks.get(i).receiveHeat(want, false);
            }
        }
        boolean flowing = before - pool > 0;

        // Сводка за тик — её читает тепловой щуп. Считаем здесь, потому что сеть уже обойдена
        // и все числа под рукой; отдельный обход ради диагностики был бы чистой тратой.
        int capacity = 0;
        for (int r : sinkRates) capacity += r;
        Survey survey = new Survey(members.size(), before, sinks.size(), before - pool, capacity);

        // 3. остаток раскладываем по трубам поровну — сеть должна выглядеть однородной
        int n = members.size();
        int each = pool / n, rem = pool % n;
        for (int i = 0; i < n; i++) {
            HeatPipeBlockEntity m = members.get(i);
            int val = each + (i < rem ? 1 : 0);
            if (m.buffer.getHeat() != val) { m.buffer.setHeat(val); m.setChanged(); }
            m.updateGlow(level, val, flowing);
            m.survey = survey;
        }
    }

    /**
     * Что сеть сделала за последний тик. Живёт только в памяти: диагностика, не состояние мира.
     *
     * @param pipes     труб в сети
     * @param pool      тепла было в котле до раздачи
     * @param sinks     сколько соединений с потребителями
     * @param delivered сколько реально раздали
     * @param capacity  сколько МОГЛИ бы раздать (сумма пропусков по соединениям)
     */
    public record Survey(int pipes, int pool, int sinks, int delivered, int capacity) {}

    private Survey survey = new Survey(0, 0, 0, 0, 0);

    public Survey getSurvey() { return survey; }

    /** Обход связной сети вширь: заодно собирает потребителей на её границе. */
    private static void collectNetwork(Level level, HeatPipeBlockEntity start, long now,
                                       java.util.List<HeatPipeBlockEntity> members,
                                       java.util.List<IHeatHandler> sinks,
                                       java.util.List<Integer> sinkRates) {
        java.util.ArrayDeque<HeatPipeBlockEntity> queue = new java.util.ArrayDeque<>();
        start.networkTick = now;
        queue.add(start);
        members.add(start);

        while (!queue.isEmpty()) {
            HeatPipeBlockEntity cur = queue.poll();
            BlockState cs = cur.getBlockState();
            int tp = cs.is(PIPE_T2) ? THROUGHPUT_T2 : THROUGHPUT;

            for (Direction d : Direction.values()) {
                // Сторона без соединения ничего не проводит, а знает об этом блокстейт — значит
                // в чанк за соседом идти незачем. Прямая труба соединена двумя сторонами из шести.
                if (!cs.getValue(HeatPipeBlock.PROPERTY_BY_DIRECTION.get(d))) continue;
                if (cur.disabled[d.get3DDataValue()]) continue;

                cur.scratch.setWithOffset(cur.worldPosition, d);
                // Обход идёт вширь и может уйти далеко, поэтому за границу загруженного не лезем:
                // иначе длинная трасса тянула бы за собой чанки.
                if (!level.isLoaded(cur.scratch)) continue;
                BlockEntity nbe = level.getBlockEntity(cur.scratch);
                if (nbe == null) continue;

                if (nbe instanceof HeatPipeBlockEntity nb) {
                    if (nb.networkTick == now) continue;                       // уже в сети
                    if (nb.disabled[d.getOpposite().get3DDataValue()]) continue; // сосед закрыл свою сторону
                    nb.networkTick = now;
                    members.add(nb);
                    queue.add(nb);
                    continue;
                }

                IHeatHandler sink = nbe.getCapability(HeatCapability.HEAT, d.getOpposite()).resolve().orElse(null);
                if (sink == null) continue;
                sinks.add(sink);
                sinkRates.add(tp);   // пропуск задаёт труба, которая касается потребителя
            }
        }
    }

    /** Тиков свечения после последней передачи — чтобы линия не мигала между тиками. */
    private int glowGrace = 0;

    /**
     * Свечение по ПОТОКУ, а не по остатку.
     * <p>
     * Раньше труба светилась от накопленного тепла, но с сетевой раздачей потребитель выгребает
     * котёл до нуля — работающая на полную линия оставалась тёмной, а светилась только та, у
     * которой тепло девать некуда. Теперь горит то, по чему тепло идёт.
     * <p>
     * Грейс в двадцать тиков нужен, чтобы редкие пустые тики не дёргали блокстейт: каждое
     * переключение это ремеш чанка.
     */
    private void updateGlow(Level level, int heat, boolean flowing) {
        if (flowing) glowGrace = 20; else if (glowGrace > 0) glowGrace--;

        BlockState st = getBlockState();
        boolean hot = st.getValue(HeatPipeBlock.HOT);
        boolean want = glowGrace > 0 || heat >= HOT_ON;
        if (!want && hot && heat > HOT_OFF) return;   // остывает — держим до нижнего порога
        if (want != hot) level.setBlock(worldPosition, st.setValue(HeatPipeBlock.HOT, want), 2);
    }

    /** Клиент: плавно тянем показываемый нагрев к синхронизированному значению (убирает «ступеньки»). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, HeatPipeBlockEntity pipe) {
        float target = pipe.buffer.getHeat();
        if (!pipe.clientInit) { // на прогрузке чанка сразу показываем текущий нагрев, без ре-налива
            pipe.clientInit = true;
            pipe.clientHeatPrev = pipe.clientHeatCurr = target;
            return;
        }
        pipe.clientHeatPrev = pipe.clientHeatCurr;
        pipe.clientHeatCurr += (target - pipe.clientHeatCurr) * 0.12f; // экспоненциальный догон (~1с до полного)
        if (Math.abs(target - pipe.clientHeatCurr) < 0.5f) pipe.clientHeatCurr = target;
    }

    /** Сглаженный нагрев для рендера свечения (с интерполяцией между тиками). */
    public float getRenderHeat(float partialTick) {
        return Mth.lerp(partialTick, clientHeatPrev, clientHeatCurr);
    }

    public int getHeat() { return buffer.getHeat(); }

    public boolean isDisabled(Direction d) { return disabled[d.get3DDataValue()]; }

    /** Ключом: переключить сторону вкл/выкл. Возвращает новое состояние (true = отключено). */
    public boolean toggleDisabled(Direction d) {
        int i = d.get3DDataValue();
        disabled[i] = !disabled[i];
        setChanged();
        return disabled[i];
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("heat", buffer.getHeat());
        byte[] dis = new byte[6];
        for (int i = 0; i < 6; i++) dis[i] = (byte) (disabled[i] ? 1 : 0);
        tag.putByteArray("disabled", dis);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        buffer.setHeat(tag.getInt("heat"));
        byte[] dis = tag.getByteArray("disabled");
        if (dis.length == 6) for (int i = 0; i < 6; i++) disabled[i] = dis[i] != 0;
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = new CompoundTag(); saveAdditional(t); return t; }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
