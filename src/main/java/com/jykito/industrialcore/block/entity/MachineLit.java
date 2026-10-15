package com.jykito.industrialcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Anti-flicker for machine LIT. Fast 1-tick recipes drop working=false and toggle
 * the light/texture on and off. The helper keeps LIT on for GRACE more ticks after stopping,
 * so short pauses between recipes do not flicker. Grace state is in a WeakHashMap keyed by BE
 * (purely visual, not persisted; the key is weak, so it is cleaned when the block unloads or is collected).
 * Called EVERY tick (no if guard), it decides by itself when to change the block. Returns the debounced LIT state.
 */
public final class MachineLit {
    private MachineLit() {}

    public static final int GRACE = 20; // ticks the light lingers after stopping (1 sec)

    private static final Map<BlockEntity, int[]> GRACE_MAP = new WeakHashMap<>();

    public static boolean tick(BlockEntity be, Level level, BlockPos pos, BlockState state,
                               BooleanProperty lit, boolean working) {
        if (level == null || !state.hasProperty(lit)) return working;
        int[] g = GRACE_MAP.computeIfAbsent(be, k -> new int[1]);
        if (working) g[0] = GRACE;
        else if (g[0] > 0) g[0]--;
        boolean want = g[0] > 0;
        if (state.getValue(lit) != want) level.setBlock(pos, state.setValue(lit, want), 3);
        return want;
    }
}
