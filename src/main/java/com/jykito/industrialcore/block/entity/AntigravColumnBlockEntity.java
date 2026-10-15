package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Antigrav column: base (block model, heat pipe connection from below) + floating pylon (BER).
 * Takes heat from the heat network into a buffer; the pylon's grooves and corners glow by heat (additive in the BER).
 * Active columns next to the magnet dampen instability (heat -> stability, COLUMNS registry).
 */
public class AntigravColumnBlockEntity extends BlockEntity implements Caps.Provider  {

    public static final int MAX_HEAT = 4000;
    private static final int ACTIVE_MIN = 200; // below that it is "cold" and does not contribute to stability

    private final HeatBuffer buffer = new HeatBuffer(MAX_HEAT);
    private int lastSyncHeat = -1000;

    // client smoothing of the glow (heat syncs in steps, we catch up smoothly)
    private float clientHeatPrev = 0f, clientHeatCurr = 0f;
    private boolean clientInit = false;

    // "working for the magnet": the magnet marks the column every tick, the flag decays; synced to the client for the BER effect
    private static final int ENGAGED_HOLD = 10;
    private int engagedTicks = 0;
    private boolean lastEngagedSync = false;
    private float clientEngaged = 0f;                     // smoothed 0..1 for the effect
    private float clientAngle = 0f, clientAnglePrev = 0f; // accumulated rotation angle (faster while working)

    // server registry of columns: the magnet uses it to compute cooling in a radius (heat -> stability)
    public static final Set<AntigravColumnBlockEntity> COLUMNS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public AntigravColumnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANTIGRAV_COLUMN_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) COLUMNS.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        COLUMNS.remove(this);
    }

        @NotNull
    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == HeatCapability.HEAT && side == Direction.DOWN) return (T) buffer; // heat ONLY from below
        return null;
    }

    public int getHeat() { return buffer.getHeat(); }
    public float getHeatFraction() { return buffer.getHeat() / (float) MAX_HEAT; }
    /** Whether it contributes to the magnet's stability (the contribution scales with the heat fraction). */
    public boolean isActive() { return buffer.getHeat() >= ACTIVE_MIN; }

    /** The magnet calls this every tick while the column helps it (in radius + active) -> visual effect. */
    public void markEngaged() { engagedTicks = ENGAGED_HOLD; }
    public boolean isEngaged() { return engagedTicks > 0; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AntigravColumnBlockEntity be) {
        if (be.engagedTicks > 0) be.engagedTicks--;                       // the magnet stopped calling -> fades out
        boolean eng = be.isEngaged();
        int cur = be.buffer.getHeat();
        if (Math.abs(cur - be.lastSyncHeat) >= 150 || eng != be.lastEngagedSync) { // sync glow and "working" to the client
            be.lastSyncHeat = cur; be.lastEngagedSync = eng;
            level.sendBlockUpdated(pos, state, state, 2);
        }
        if (cur > 0) { // very gentle leak: heat holds steadier (less flicker with a weak supply)
            be.buffer.extract(Math.max(1, cur / 2500));
            be.setChanged();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AntigravColumnBlockEntity be) {
        float target = be.buffer.getHeat();
        if (!be.clientInit) { be.clientInit = true; be.clientHeatPrev = be.clientHeatCurr = target; }
        be.clientHeatPrev = be.clientHeatCurr;
        be.clientHeatCurr += (target - be.clientHeatCurr) * 0.07f; // gentle catch-up: removes flicker with an uneven supply
        if (Math.abs(target - be.clientHeatCurr) < 0.5f) be.clientHeatCurr = target;
        // "working for the magnet": smooth effect level + angle accumulation (faster while working)
        be.clientEngaged += ((be.isEngaged() ? 1f : 0f) - be.clientEngaged) * 0.1f;
        be.clientAnglePrev = be.clientAngle;
        be.clientAngle = (be.clientAngle + 1.5f + be.clientEngaged * 3.0f) % 360f; // 1.5°/tick -> up to 4.5 while working
    }

    /** Smoothed heat for the glow (interpolated between ticks). */
    public float getRenderHeat(float partialTick) { return Mth.lerp(partialTick, clientHeatPrev, clientHeatCurr); }

    /** Smooth pylon rotation angle (faster when the column works for the magnet; wraps 360 -> 0). */
    public float getRenderAngle(float partialTick) {
        float a0 = clientAnglePrev, a1 = clientAngle;
        if (a1 < a0) a1 += 360f;
        return (a0 + (a1 - a0) * partialTick) % 360f;
    }
    /** 0..1: how much the column is "working for the magnet" (for the BER effect). */
    public float getEngagedLevel() { return clientEngaged; }

    /** Render bounds of the renderer (see its getRenderBoundingBox). */
    public AABB renderBounds() {
        return new AABB(worldPosition).inflate(2); // the pylon is ~2 blocks tall, so the bbox is expanded to avoid clipping
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("heat", buffer.getHeat());
        tag.putBoolean("eng", isEngaged());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.setHeat(tag.getInt("heat"));
        engagedTicks = tag.getBoolean("eng") ? ENGAGED_HOLD : 0;
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { CompoundTag t = new CompoundTag(); saveAdditional(t, registries); return t; }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) { loadAdditional(tag, registries); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
