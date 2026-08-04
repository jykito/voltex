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

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeatPipeBlockEntity pipe) {
        int h = pipe.buffer.getHeat();
        // статик-свечение: переключаем blockstate hot с гистерезисом (toggle редкий → ремеш только на нагрев/остывание)
        boolean hot = state.getValue(HeatPipeBlock.HOT);
        if (!hot && h >= HOT_ON) level.setBlock(pos, state.setValue(HeatPipeBlock.HOT, true), 2);
        else if (hot && h <= HOT_OFF) level.setBlock(pos, state.setValue(HeatPipeBlock.HOT, false), 2);
        if (h <= 0) return;
        boolean changed = false;

        ensurePipeBlocks();
        boolean t2 = state.is(PIPE_T2); // T2: без потерь + выше пропуск

        // утечка в среду — потеря на расстоянии (T2 — без потерь)
        int leak = t2 ? 0 : h / 800; // ~0.125%/тик у T1
        if (leak > 0) { pipe.buffer.extract(leak); h -= leak; changed = true; }

        // проводимость: труба↔труба — уравнивание (диффузия, без осцилляции);
        //               труба→потребитель — НАСОС (наполняем машину до полного, темпом пропуска)
        int tp = t2 ? THROUGHPUT_T2 : THROUGHPUT;
        for (Direction d : Direction.values()) {
            if (h <= 0) break;
            if (pipe.disabled[d.get3DDataValue()]) continue; // отключённая сторона не проводит
            pipe.scratch.setWithOffset(pos, d);
            BlockEntity nbe = level.getBlockEntity(pipe.scratch);
            if (nbe == null) continue;
            IHeatHandler n = nbe.getCapability(HeatCapability.HEAT, d.getOpposite()).resolve().orElse(null);
            if (n == null) continue;
            BlockState ns = nbe.getBlockState();   // состояние уже лежит в найденной сущности, второй раз чанк не трогаем
            boolean nbrPipe = ns.getBlock() instanceof HeatPipeBlock;  // оба тира — этот класс, реестр не нужен
            int flow;
            if (nbrPipe) {
                int nh = n.getHeat();
                if (nh >= h) continue;
                flow = Math.min((h - nh) / 2, tp);   // между трубами — уравнивание
            } else {
                flow = Math.min(h, tp);               // в потребителя — насос (до пропуска, машина сама режет по свободному месту)
            }
            if (flow <= 0) continue;
            int accepted = n.receiveHeat(flow, false);
            if (accepted > 0) { pipe.buffer.extract(accepted); h -= accepted; changed = true; }
        }

        if (changed) pipe.setChanged();
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
