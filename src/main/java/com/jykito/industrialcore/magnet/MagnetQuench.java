package com.jykito.industrialcore.magnet;


import net.neoforged.fml.common.EventBusSubscriber;
import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Quench: field collapse at 100% instability, a splitting wave from the centre out to radius R.
 * Everything in the radius -> air (except bedrock), entities evaporate, particles spiral into the centre.
 * Detached from the BlockEntity (ticks on LevelTickEvent), because the wave removes the magnet itself too.
 */
@EventBusSubscriber(modid = IndustrialCore.MODID)
public class MagnetQuench {

    private static final float STEP = 0.7f; // radius growth per tick (slower = prettier + less lag)
    private static final List<Quench> ACTIVE = new ArrayList<>();

    public static void start(ServerLevel level, Vec3 center, int radius) {
        ACTIVE.add(new Quench(level, center, radius));
        com.jykito.industrialcore.advancement.VoltexEventTrigger.award(level, center, radius,
                com.jykito.industrialcore.advancement.VoltexEventTrigger.MAGNET_QUENCH);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post e) {
        if (e.getLevel().isClientSide() || ACTIVE.isEmpty()) return;
        Iterator<Quench> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Quench q = it.next();
            if (q.level == e.getLevel() && q.advance()) it.remove();
        }
    }

    private static class Quench {
        final ServerLevel level;
        final Vec3 center;
        final int radius;
        float front = 0f;

        Quench(ServerLevel level, Vec3 center, int radius) {
            this.level = level; this.center = center; this.radius = radius;
        }

        /** One wave step: splits the spherical shell [front..next]. Returns true when it reached the edge. */
        boolean advance() {
            float next = Math.min(radius, front + STEP);
            double inner = front * front, outer = next * next;
            int r = (int) Math.ceil(next);
            int cx = (int) Math.floor(center.x), cy = (int) Math.floor(center.y), cz = (int) Math.floor(center.z);
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            var mote = ModParticles.ENERGY_MOTE.get();
            int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

            // We walk only the SHELL: for a column (dx,dz) we compute the narrow dy band on the sphere right away
            // (dy² ∈ [inner-h2, outer-h2]) instead of iterating the whole cube dy=-r..r. The exact d2 filter is kept
            // (guard band ±1 against float errors) -> exactly the same blocks are removed, but with ~r² iterations instead of ~r³.
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    double h2 = (double) dx * dx + (double) dz * dz;
                    if (h2 > outer) continue;                        // the whole column is outside the sphere
                    int hi = (int) Math.floor(Math.sqrt(outer - h2)) + 1;
                    int lo = (inner - h2 <= 0.0) ? 0 : Math.max(0, (int) Math.floor(Math.sqrt(inner - h2)) - 1);
                    for (int k = lo; k <= hi; k++) {
                        int dyEnd = k, dyStep = (k == 0) ? 1 : 2 * k; // dy = ±k (k=0 once)
                        for (int dy = -k; dy <= dyEnd; dy += dyStep) {
                            double d2 = h2 + (double) dy * dy;
                            if (d2 < inner || d2 > outer) continue;  // the same shell filter as before
                            int y = cy + dy;
                            if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) continue;
                            m.set(cx + dx, y, cz + dz);
                            BlockState bs = level.getBlockState(m);
                            if (bs.isAir() || bs.is(Blocks.BEDROCK)) continue; // bedrock stays
                            level.setBlock(m, air, flags);
                            if (level.random.nextInt(90) == 0)       // the front drops motes that spiral into the centre
                                level.sendParticles(mote, m.getX() + 0.5, y + 0.5, m.getZ() + 0.5, 0,
                                        center.x, center.y, center.z, 1.0);
                        }
                    }
                }

            // entities inside the front -> evaporation
            AABB box = new AABB(center.x - next, center.y - next, center.z - next,
                                center.x + next, center.y + next, center.z + next);
            for (Entity ent : level.getEntities((Entity) null, box, en -> en.isAlive())) {
                if (ent.distanceToSqr(center.x, center.y, center.z) > outer) continue;
                if (ent instanceof Player p) { if (!p.isCreative() && !p.isSpectator()) p.kill(); }
                else ent.discard();
            }

            front = next;
            return front >= radius;
        }
    }
}
