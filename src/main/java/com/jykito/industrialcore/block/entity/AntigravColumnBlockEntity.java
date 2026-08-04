package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.heat.HeatBuffer;
import com.jykito.industrialcore.heat.HeatCapability;
import com.jykito.industrialcore.heat.IHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Антиграв-колонна: подставка (блок-модель, коннект теплотрубы снизу) + парящий пилон (BER).
 * Принимает тепло из теплосети в буфер; выемки/углы пилона светятся по нагреву (аддитив в BER).
 * Активные колонны рядом с магнитом гасят нестабильность (тепло→стабильность, реестр COLUMNS).
 */
public class AntigravColumnBlockEntity extends BlockEntity {

    public static final int MAX_HEAT = 4000;
    private static final int ACTIVE_MIN = 200; // ниже — «холодная», в стабильность не идёт

    private final HeatBuffer buffer = new HeatBuffer(MAX_HEAT);
    private LazyOptional<IHeatHandler> lazyHeat = LazyOptional.empty();
    private int lastSyncHeat = -1000;

    // клиентское сглаживание свечения (тепло синкается ступеньками — догоняем плавно)
    private float clientHeatPrev = 0f, clientHeatCurr = 0f;
    private boolean clientInit = false;

    // «работает на магнит»: магнит помечает колонну каждый тик, флаг декеймится; синк на клиент для BER-эффекта
    private static final int ENGAGED_HOLD = 10;
    private int engagedTicks = 0;
    private boolean lastEngagedSync = false;
    private float clientEngaged = 0f;                     // сглаженный 0..1 для эффекта
    private float clientAngle = 0f, clientAnglePrev = 0f; // накопленный угол вращения (быстрее при работе)

    // серверный реестр колонн — магнит по нему считает охлаждение в радиусе (тепло→стабильность)
    public static final Set<AntigravColumnBlockEntity> COLUMNS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public AntigravColumnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANTIGRAV_COLUMN_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyHeat = LazyOptional.of(() -> buffer);
        if (level != null && !level.isClientSide()) COLUMNS.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        lazyHeat.invalidate();
        COLUMNS.remove(this);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyHeat.invalidate();
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == HeatCapability.HEAT && side == Direction.DOWN) return lazyHeat.cast(); // тепло ТОЛЬКО снизу
        return super.getCapability(cap, side);
    }

    public int getHeat() { return buffer.getHeat(); }
    public float getHeatFraction() { return buffer.getHeat() / (float) MAX_HEAT; }
    /** Идёт ли в стабильность магнита (вклад масштабируется долей тепла). */
    public boolean isActive() { return buffer.getHeat() >= ACTIVE_MIN; }

    /** Магнит зовёт каждый тик, пока колонна ему помогает (в радиусе + активна) → визуальный эффект. */
    public void markEngaged() { engagedTicks = ENGAGED_HOLD; }
    public boolean isEngaged() { return engagedTicks > 0; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AntigravColumnBlockEntity be) {
        if (be.engagedTicks > 0) be.engagedTicks--;                       // магнит перестал звать → затухает
        boolean eng = be.isEngaged();
        int cur = be.buffer.getHeat();
        if (Math.abs(cur - be.lastSyncHeat) >= 150 || eng != be.lastEngagedSync) { // синк свечения/«работы» на клиент
            be.lastSyncHeat = cur; be.lastEngagedSync = eng;
            level.sendBlockUpdated(pos, state, state, 2);
        }
        if (cur > 0) { // очень мягкая утечка — тепло держится ровнее (меньше мигания при слабой подпитке)
            be.buffer.extract(Math.max(1, cur / 2500));
            be.setChanged();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AntigravColumnBlockEntity be) {
        float target = be.buffer.getHeat();
        if (!be.clientInit) { be.clientInit = true; be.clientHeatPrev = be.clientHeatCurr = target; }
        be.clientHeatPrev = be.clientHeatCurr;
        be.clientHeatCurr += (target - be.clientHeatCurr) * 0.07f; // мягкий догон — гасит мигание при неровной подпитке
        if (Math.abs(target - be.clientHeatCurr) < 0.5f) be.clientHeatCurr = target;
        // «работа на магнит»: плавный уровень эффекта + накопление угла (быстрее при работе)
        be.clientEngaged += ((be.isEngaged() ? 1f : 0f) - be.clientEngaged) * 0.1f;
        be.clientAnglePrev = be.clientAngle;
        be.clientAngle = (be.clientAngle + 1.5f + be.clientEngaged * 3.0f) % 360f; // 1.5°/тик → до 4.5 при работе
    }

    /** Сглаженный нагрев для свечения (интерполяция между тиками). */
    public float getRenderHeat(float partialTick) { return Mth.lerp(partialTick, clientHeatPrev, clientHeatCurr); }

    /** Плавный угол вращения пилона (быстрее, когда колонна работает на магнит; с разматыванием 360→0). */
    public float getRenderAngle(float partialTick) {
        float a0 = clientAnglePrev, a1 = clientAngle;
        if (a1 < a0) a1 += 360f;
        return (a0 + (a1 - a0) * partialTick) % 360f;
    }
    /** 0..1 — насколько колонна «работает на магнит» (для эффекта в BER). */
    public float getEngagedLevel() { return clientEngaged; }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2); // пилон ~2 блока — расширяем bbox чтоб не срезался
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("heat", buffer.getHeat());
        tag.putBoolean("eng", isEngaged());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        buffer.setHeat(tag.getInt("heat"));
        engagedTicks = tag.getBoolean("eng") ? ENGAGED_HOLD : 0;
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = new CompoundTag(); saveAdditional(t); return t; }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
