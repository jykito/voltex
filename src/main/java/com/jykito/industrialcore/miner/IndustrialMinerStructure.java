package com.jykito.industrialcore.miner;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.multiblock.WandBuild;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Схема промышленного шахтёра 5×5×6 под его модель (tools/texture_viewer/industrial_miner_def.js): четыре ноги по углам,
 * под днищем пусто (там крутится бур), пол 5×5, стены с ядром в середине фасада, крыша 5×5, мачта в два блока.
 * Собранная структура прячется (каркас невидим), её рисует IndustrialMinerBER.
 * <p>
 * Слои снизу вверх, ядро в слое {@link #CORE_LAYER}; строки от фасада (0, с ядром) вглубь; столбцы поперёк фасада.
 * F — каркас, K — ядро, _ — должно быть пусто или заменяемо (трава), . — не проверяется.
 */
public final class IndustrialMinerStructure {

    private static final String[][] LAYERS = {
            {"F___F", "_____", "_____", "_____", "F___F"},
            {"FFFFF", "FFFFF", "FFFFF", "FFFFF", "FFFFF"},
            {"FFKFF", "F...F", "F...F", "F...F", "FFFFF"},
            {"FFFFF", "FFFFF", "FFFFF", "FFFFF", "FFFFF"},
            {".....", ".....", "..F..", ".....", "....."},
            {".....", ".....", "..F..", ".....", "....."}};
    public static final int CORE_LAYER = 2;

    private IndustrialMinerStructure() {}

    /** Клетка схемы в мире: слой y, строка r (вглубь от фасада), столбец c — при ядре в core, смотрящем в facing. */
    public static BlockPos at(BlockPos core, Direction facing, int y, int r, int c) {
        return core.relative(facing.getOpposite(), r).relative(facing.getClockWise(), c - 2).above(y - CORE_LAYER);
    }

    /** Центр основания (середина нижнего слоя) — от него BER отсчитывает модель. */
    public static BlockPos base(BlockPos core, Direction facing) {
        return at(core, facing, 0, 2, 2);
    }

    /** Первая неверная клетка: что там должно быть (F — каркас, _ — пусто). */
    public record Problem(BlockPos pos, char expected) {}

    public static @Nullable Problem check(Level level, BlockPos core, Direction facing) {
        for (int y = 0; y < LAYERS.length; y++)
            for (int r = 0; r < 5; r++)
                for (int c = 0; c < 5; c++) {
                    char ch = LAYERS[y][r].charAt(c);
                    if (ch == '.' || ch == 'K') continue;
                    BlockPos p = at(core, facing, y, r, c);
                    BlockState s = level.getBlockState(p);
                    if (ch == 'F' && !s.is(ModBlocks.INDUSTRIAL_MINER_FRAME.get())) return new Problem(p, 'F');
                    if (ch == '_' && !s.isAir() && !s.canBeReplaced()) return new Problem(p, '_');
                }
        return null;
    }

    /**
     * Достройка структурной палочкой: недостающий каркас ставится (в выживании — из инвентаря), место под днищем
     * в творческом расчищается, в выживании занятая клетка только засчитывается — чужое палочка не ломает.
     */
    public static void build(Level level, BlockPos core, Direction facing, WandBuild wb) {
        BlockState frame = ModBlocks.INDUSTRIAL_MINER_FRAME.get().defaultBlockState();
        for (int y = 0; y < LAYERS.length; y++)
            for (int r = 0; r < 5; r++)
                for (int c = 0; c < 5; c++) {
                    char ch = LAYERS[y][r].charAt(c);
                    if (ch == '.' || ch == 'K') continue;
                    BlockPos p = at(core, facing, y, r, c);
                    if (!level.isLoaded(p)) continue;
                    BlockState s = level.getBlockState(p);
                    if (ch == 'F' && !s.is(frame.getBlock())) wb.place(level, p, frame);
                    else if (ch == '_' && !s.isAir() && !s.canBeReplaced()) {
                        if (wb.free()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                        else wb.markBlocked();
                    }
                }
    }

    /** Все клетки каркаса. */
    public static List<BlockPos> frames(BlockPos core, Direction facing) {
        List<BlockPos> out = new ArrayList<>(72);
        for (int y = 0; y < LAYERS.length; y++)
            for (int r = 0; r < 5; r++)
                for (int c = 0; c < 5; c++)
                    if (LAYERS[y][r].charAt(c) == 'F') out.add(at(core, facing, y, r, c));
        return out;
    }
}
