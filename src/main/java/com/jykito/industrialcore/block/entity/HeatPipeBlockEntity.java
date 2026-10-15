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
    private static final int THROUGHPUT = 128;    // max flow per tick per neighbour (T1)
    private static final int THROUGHPUT_T2 = 512; // T2: higher throughput, no losses
    private static final int HOT_ON = 300, HOT_OFF = 50; // hysteresis of the blockstate glow (rare toggle -> little remeshing)

    private final HeatBuffer buffer = new HeatBuffer(MAX_HEAT);
    private LazyOptional<IHeatHandler> lazyHeat = LazyOptional.empty();

    /** Sides disabled by hand (with the wrench): they grow no arm, do not conduct and do not accept heat. */
    private final boolean[] disabled = new boolean[6];

    /** Scratch pad for the neighbour position: overwritten instead of created anew for each side. */
    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    // The T2 block does not change after registration, so we take it from RegistryObject once,
    // otherwise .get() would be called for every pipe every tick. A neighbouring pipe is recognised
    // by class (both tiers are HeatPipeBlock), so no second reference is needed.
    private static net.minecraft.world.level.block.Block PIPE_T2;
    private static void ensurePipeBlocks() {
        if (PIPE_T2 == null) PIPE_T2 = com.jykito.industrialcore.block.ModBlocks.HEAT_PIPE_T2.get();
    }

    // client smoothing of glow brightness (data syncs in steps, here we catch up smoothly)
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
     * Tick at which the network has already been computed. Processing goes by NETWORK, not by neighbours, so the first
     * pipe computes the whole network and the others skip their turn this tick.
     */
    private long networkTick = -1;

    /**
     * The heat network is computed as a whole, as one pool.
     * <p>
     * Before, a pipe only knew its six neighbours and spread along the gradient, like heat conduction.
     * Because of the symmetry only HALF of the difference could be passed to a neighbour, otherwise the pipes would
     * bounce heat back and forth. The side effect was nasty: to carry a flow F, the pipes
     * needed a 2F drop, the drops added up along the route and ran into the buffer size. A line
     * of five pipes physically could not carry more than 500 per tick, however many branches you laid.
     * <p>
     * Now there is no flow between pipes at all. All the heat of the network is a shared pool, and it is handed out
     * to consumers at {@code THROUGHPUT} per connection. Route length no longer matters
     * at all, "512 per connection" became literally true, and oscillations have nowhere to come from.
     * <p>
     * It is also cheaper: before, every pipe went to six neighbours every tick
     * for capabilities; now the network is walked once.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, HeatPipeBlockEntity pipe) {
        long now = level.getGameTime();
        if (pipe.networkTick == now) return;   // the network was already computed by another pipe this tick

        ensurePipeBlocks();

        java.util.List<HeatPipeBlockEntity> members = new java.util.ArrayList<>();
        java.util.List<IHeatHandler> sinks = new java.util.ArrayList<>();
        java.util.List<Integer> sinkRates = new java.util.ArrayList<>();

        collectNetwork(level, pipe, now, members, sinks, sinkRates);

        // 1. leak: each pipe has its own, T2 does not leak
        int pool = 0;
        for (HeatPipeBlockEntity m : members) {
            int h = m.buffer.getHeat();
            if (h > 0 && !m.getBlockState().is(PIPE_T2)) {
                int leak = h / 800;                    // ~0.125%/tick on T1
                if (leak > 0) { m.buffer.extract(leak); h -= leak; }
            }
            pool += h;
        }

        // 2. hand out to consumers: first evenly, then the remainder round-robin, so that one machine
        //    at the start of the list does not drain the whole pool while the others stand
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

        // Per-tick summary, read by the heat probe. Computed here because the network has already been walked
        // and all numbers are at hand; a separate walk just for diagnostics would be pure waste.
        int capacity = 0;
        for (int r : sinkRates) capacity += r;
        Survey survey = new Survey(members.size(), before, sinks.size(), before - pool, capacity);

        // 3. spread the remainder evenly over the pipes: the network should look uniform
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
     * What the network did in the last tick. Lives only in memory: diagnostics, not world state.
     *
     * @param pipes     pipes in the network
     * @param pool      heat in the pool before handing out
     * @param sinks     number of connections to consumers
     * @param delivered how much was actually handed out
     * @param capacity  how much COULD have been handed out (sum of throughput per connection)
     */
    public record Survey(int pipes, int pool, int sinks, int delivered, int capacity) {}

    private Survey survey = new Survey(0, 0, 0, 0, 0);

    public Survey getSurvey() { return survey; }

    /** Breadth-first walk of the connected network: it also collects the consumers on its border. */
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
                // A side without a connection conducts nothing, and the blockstate knows it, so
                // there is no reason to go into the chunk behind the neighbour. A straight pipe is connected on two sides out of six.
                if (!cs.getValue(HeatPipeBlock.PROPERTY_BY_DIRECTION.get(d))) continue;
                if (cur.disabled[d.get3DDataValue()]) continue;

                cur.scratch.setWithOffset(cur.worldPosition, d);
                // The walk is breadth-first and can go far, so we do not cross the loaded border:
                // otherwise a long route would drag chunks along with it.
                if (!level.isLoaded(cur.scratch)) continue;
                BlockEntity nbe = level.getBlockEntity(cur.scratch);
                if (nbe == null) continue;

                if (nbe instanceof HeatPipeBlockEntity nb) {
                    if (nb.networkTick == now) continue;                       // already in the network
                    if (nb.disabled[d.getOpposite().get3DDataValue()]) continue; // the neighbour closed its side
                    nb.networkTick = now;
                    members.add(nb);
                    queue.add(nb);
                    continue;
                }

                IHeatHandler sink = nbe.getCapability(HeatCapability.HEAT, d.getOpposite()).resolve().orElse(null);
                if (sink == null) continue;
                sinks.add(sink);
                sinkRates.add(tp);   // throughput is set by the pipe touching the consumer
            }
        }
    }

    /** Glow ticks after the last transfer, so the line does not flicker between ticks. */
    private int glowGrace = 0;

    /**
     * Glow by FLOW, not by what is left.
     * <p>
     * Before, a pipe glowed from stored heat, but with network distribution the consumer drains
     * the pool to zero: a line running at full rate stayed dark, and only the one
     * with nowhere to put its heat glowed. Now what heat flows through is lit.
     * <p>
     * The twenty-tick grace is there so rare empty ticks do not toggle the blockstate: every
     * toggle is a chunk remesh.
     */
    private void updateGlow(Level level, int heat, boolean flowing) {
        if (flowing) glowGrace = 20; else if (glowGrace > 0) glowGrace--;

        BlockState st = getBlockState();
        boolean hot = st.getValue(HeatPipeBlock.HOT);
        boolean want = glowGrace > 0 || heat >= HOT_ON;
        if (!want && hot && heat > HOT_OFF) return;   // cooling down: hold until the lower threshold
        if (want != hot) level.setBlock(worldPosition, st.setValue(HeatPipeBlock.HOT, want), 2);
    }

    /** Client: smoothly pull the displayed heat towards the synced value (removes the "steps"). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, HeatPipeBlockEntity pipe) {
        float target = pipe.buffer.getHeat();
        if (!pipe.clientInit) { // when the chunk loads, show the current heat right away, without refilling
            pipe.clientInit = true;
            pipe.clientHeatPrev = pipe.clientHeatCurr = target;
            return;
        }
        pipe.clientHeatPrev = pipe.clientHeatCurr;
        pipe.clientHeatCurr += (target - pipe.clientHeatCurr) * 0.12f; // exponential catch-up (~1s to full)
        if (Math.abs(target - pipe.clientHeatCurr) < 0.5f) pipe.clientHeatCurr = target;
    }

    /** Smoothed heat for rendering the glow (interpolated between ticks). */
    public float getRenderHeat(float partialTick) {
        return Mth.lerp(partialTick, clientHeatPrev, clientHeatCurr);
    }

    public int getHeat() { return buffer.getHeat(); }

    public boolean isDisabled(Direction d) { return disabled[d.get3DDataValue()]; }

    /** With the wrench: toggle a side on/off. Returns the new state (true = disabled). */
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
