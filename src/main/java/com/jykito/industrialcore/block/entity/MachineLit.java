package com.jykito.industrialcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Анти-мигание LIT у машин. Быстрые рецепты на 1 тик роняют working=false и дёргают
 * свет/текстуру вкл-выкл. Хелпер держит LIT включённым ещё GRACE тиков после остановки,
 * так что краткие паузы между рецептами не мигают. Состояние грейса — в WeakHashMap по BE
 * (чисто визуально, не персистится; ключ слабый — чистится при выгрузке/GC блока).
 * Зовётся КАЖДЫЙ тик (без if-гарда), сам решает когда менять блок. Возвращает дебаунс-состояние LIT.
 */
public final class MachineLit {
    private MachineLit() {}

    public static final int GRACE = 20; // тиков «дотягивания» света после остановки (1 сек)

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
