package com.jykito.industrialcore.block.custom.wind;

import com.jykito.industrialcore.block.entity.WindTurbineNacelleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Wind turbine rules, shared by the base, the mast sections, the nacelle, the click message and Jade.
 * <p>
 * The player builds it: a base, 2 to 10 mast sections on it, the nacelle on top and a rotor in the nacelle ({@link WindRotors}).
 * Output per tick is {@link #BASE_OUTPUT} × mast (0.5 for 2 sections up to 1.0 for 10) × site (base at Y 64 or lower 0.7 up to
 * Y 128 or higher 1.0) × weather (rain 1.5, thunder 2) × wind (0.6..1.4) × neighbours (0.7 for each other turbine closer than
 * 6 blocks) × rotor, capped at {@link #MAX_OUTPUT} × rotor. The nacelle needs open sky and free room for the rotor's reach.
 * <p>
 * The wind is the same for the whole dimension and is a pure function of the game time: the client turns the nacelles
 * after it and the server counts the output from it, no sync needed.
 */
public final class WindTurbine {

    public static final int BASE_OUTPUT = 180, MAX_OUTPUT = 500, MIN_SECTIONS = 2, MAX_SECTIONS = 10;
    /** Mast sections take a model by the number of sections under them; higher ones keep the narrowest. */
    public static final int MODEL_LEVELS = 10;
    /** Other turbines closer than this (horizontally, and within {@link #SHADOW_HEIGHT} up or down) take the wind. */
    private static final int SHADOW_RADIUS = 6, SHADOW_HEIGHT = 8;
    private static final float SHADOW = 0.7f;
    private static final double TAU = Math.PI * 2;

    /** Why the turbine stands still; OK when it works. */
    public enum Problem { OK, NO_NACELLE, NO_BASE, FEW_SECTIONS, NO_ROTOR, NO_SKY, BLOCKED }

    /** The state of one turbine: the problem, or the factors and the output; room is the side of the free square the rotor needs. */
    public record Status(Problem problem, int sections, float mast, float site, float weather, float wind, float neighbours, float rotor, int room, int output) {

        public static final Status NO_NACELLE = WindTurbine.problem(Problem.NO_NACELLE, 0, 0);

        public Component describe() {
            if (problem == Problem.OK)
                return Component.translatable("message.industrial_core.wind_turbine.output", output, f(mast), f(site), f(weather), f(wind), f(neighbours), f(rotor));
            return Component.translatable("message.industrial_core.wind_turbine." + problem.name().toLowerCase(Locale.ROOT), sections, MIN_SECTIONS, room);
        }

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putByte("p", (byte) problem.ordinal());
            t.putInt("n", sections);
            t.putFloat("m", mast);
            t.putFloat("s", site);
            t.putFloat("w", weather);
            t.putFloat("g", wind);
            t.putFloat("k", neighbours);
            t.putFloat("r", rotor);
            t.putInt("z", room);
            t.putInt("o", output);
            return t;
        }

        public static Status load(CompoundTag t) {
            Problem[] all = Problem.values();
            return new Status(all[Mth.clamp(t.getByte("p"), 0, all.length - 1)], t.getInt("n"), t.getFloat("m"), t.getFloat("s"),
                    t.getFloat("w"), t.getFloat("g"), t.getFloat("k"), t.getFloat("r"), t.getInt("z"), t.getInt("o"));
        }

        private static String f(float v) { return String.format(Locale.ROOT, "%.2f", v); }
    }

    private WindTurbine() {}

    private static Status problem(Problem p, int sections, int room) { return new Status(p, sections, 0, 0, 0, 0, 0, 0, room, 0); }

    /** How many blocks around the nacelle the rotor needs free: 2 for the basic rotor. */
    public static int reach(float radius) { return Math.max(2, (int) Math.ceil(radius - 0.5f)); }

    public static float mastFactor(int sections) { return 0.375f + 0.0625f * Math.min(sections, MAX_SECTIONS); }

    public static float siteFactor(int baseY) { return 0.7f + 0.3f * Mth.clamp((baseY - 64) / 64f, 0f, 1f); }

    public static float weatherFactor(Level level) { return level.isThundering() ? 2f : level.isRaining() ? 1.5f : 1f; }

    /** Wind strength 0.6..1.4: a slow swell over 5 minutes with gusts on top. */
    public static float wind(double time) {
        return (float) (1 + 0.28 * Math.sin(time * TAU / 6000) + 0.12 * Math.sin(time * TAU / 1700 + 1.7));
    }

    /** Wind direction around Y in radians: swings up to about 120 degrees either way over a few in-game hours. */
    public static float windDirection(double time) {
        return (float) (1.6 * Math.sin(time * TAU / 30000) + 0.5 * Math.sin(time * TAU / 7000 + 2));
    }

    /** The nacelle a turbine block belongs to: the block above the base and the run of mast sections, or the nacelle itself. */
    @Nullable
    public static BlockPos findNacelle(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos p = pos.mutable();
        if (level.getBlockState(p).getBlock() instanceof WindTurbineBaseBlock) p.move(Direction.UP);
        for (int i = 0; i < 64 && level.getBlockState(p).getBlock() instanceof WindTurbineMastBlock; i++) p.move(Direction.UP);
        return level.getBlockState(p).getBlock() instanceof WindTurbineNacelleBlock ? p.immutable() : null;
    }

    /**
     * Server: the state of the turbine with its nacelle at {@code nacelle} and {@code rotor} in it (null: none);
     * {@code base} receives the base position if there is one. Counted in double: a pack's rotor can be up to ×1000.
     */
    public static Status evaluate(Level level, BlockPos nacelle, @Nullable WindRotors.Spec rotor, BlockPos.MutableBlockPos base) {
        BlockPos.MutableBlockPos p = nacelle.mutable().move(Direction.DOWN);
        int n = 0;
        while (n < 64 && level.getBlockState(p).getBlock() instanceof WindTurbineMastBlock) { n++; p.move(Direction.DOWN); }
        if (!(level.getBlockState(p).getBlock() instanceof WindTurbineBaseBlock)) return problem(Problem.NO_BASE, n, 0);
        base.set(p);
        if (n < MIN_SECTIONS) return problem(Problem.FEW_SECTIONS, n, 0);
        if (rotor == null) return problem(Problem.NO_ROTOR, n, 0);
        if (!level.canSeeSky(nacelle.above())) return problem(Problem.NO_SKY, n, 0);
        int reach = reach(rotor.radius()), room = 2 * reach + 1;
        if (!rotorFree(level, nacelle, reach)) return problem(Problem.BLOCKED, n, room);
        float mast = mastFactor(n), site = siteFactor(p.getY()), weather = weatherFactor(level), wind = wind(level.getGameTime());
        float neighbours = (float) Math.pow(SHADOW, neighbours(level, nacelle));
        double raw = (double) BASE_OUTPUT * mast * site * weather * wind * neighbours * rotor.output();
        int out = (int) Math.min((double) MAX_OUTPUT * rotor.output(), Math.round(raw));
        return new Status(Problem.OK, n, mast, site, weather, wind, neighbours, rotor.output(), room, out);
    }

    /**
     * The rotor sweeps a circle and turns after the wind, so a square of {@code reach} blocks each way around the nacelle,
     * from {@code reach} below to one more above, has to be free of solid blocks (5×5 for the basic rotor). The column of the
     * mast and the nacelle itself does not count; unloaded chunks count as free.
     */
    private static boolean rotorFree(Level level, BlockPos nacelle, int reach) {
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int dy = -reach; dy <= reach + 1; dy++)
            for (int dx = -reach; dx <= reach; dx++)
                for (int dz = -reach; dz <= reach; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    q.setWithOffset(nacelle, dx, dy, dz);
                    if (level.hasChunkAt(q) && !level.getBlockState(q).getCollisionShape(level, q).isEmpty()) return false;
                }
        return true;
    }

    // ── loaded nacelles per dimension, for the wind shadow (server only) ──

    private static final Map<ResourceKey<Level>, Set<BlockPos>> NACELLES = new HashMap<>();

    public static void track(Level level, BlockPos pos, boolean loaded) {
        if (level.isClientSide) return;
        if (loaded) NACELLES.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
        else {
            Set<BlockPos> set = NACELLES.get(level.dimension());
            if (set != null) set.remove(pos);
        }
    }

    private static int neighbours(Level level, BlockPos pos) {
        int k = 0;
        for (BlockPos o : NACELLES.getOrDefault(level.dimension(), Set.of())) {
            if (o.equals(pos)) continue;
            int dx = o.getX() - pos.getX(), dz = o.getZ() - pos.getZ();
            if (dx * dx + dz * dz < SHADOW_RADIUS * SHADOW_RADIUS && Math.abs(o.getY() - pos.getY()) <= SHADOW_HEIGHT) k++;
        }
        return k;
    }

    /** Right click with an empty hand on any part: the state of the turbine in the action bar. */
    public static InteractionResult showStatus(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) player.displayClientMessage(statusAt(level, pos).describe(), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Server: the state of the turbine the block at {@code pos} belongs to. */
    public static Status statusAt(Level level, BlockPos pos) {
        BlockPos n = findNacelle(level, pos);
        return n != null && level.getBlockEntity(n) instanceof WindTurbineNacelleBlockEntity be ? be.status() : Status.NO_NACELLE;
    }
}
