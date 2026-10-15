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
 * Industrial miner schema 5x5x6 matching its model (tools/texture_viewer/industrial_miner_def.js): four legs at the corners,
 * empty under the bottom (the drill spins there), a 5x5 floor, walls with the core in the middle of the front, a 5x5 roof, a two-block mast.
 * A formed structure hides (the frame is invisible), IndustrialMinerBER draws it.
 * <p>
 * Layers bottom to top, the core in layer {@link #CORE_LAYER}; rows from the front (0, with the core) inwards; columns across the front.
 * F frame, K core, _ must be empty or replaceable (grass), . not checked.
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

    /** A schema cell in the world: layer y, row r (inwards from the front), column c, with the core at core facing facing. */
    public static BlockPos at(BlockPos core, Direction facing, int y, int r, int c) {
        return core.relative(facing.getOpposite(), r).relative(facing.getClockWise(), c - 2).above(y - CORE_LAYER);
    }

    /** Centre of the base (middle of the bottom layer): the BER positions the model from it. */
    public static BlockPos base(BlockPos core, Direction facing) {
        return at(core, facing, 0, 2, 2);
    }

    /** The first wrong cell: what must be there (F frame, _ empty). */
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
     * Completion with the structure wand: the missing frame is placed (in survival from the inventory), the space under the bottom
     * is cleared in creative, in survival an occupied cell is just counted; the wand does not break other blocks.
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

    /** All frame cells. */
    public static List<BlockPos> frames(BlockPos core, Direction facing) {
        List<BlockPos> out = new ArrayList<>(72);
        for (int y = 0; y < LAYERS.length; y++)
            for (int r = 0; r < 5; r++)
                for (int c = 0; c < 5; c++)
                    if (LAYERS[y][r].charAt(c) == 'F') out.add(at(core, facing, y, r, c));
        return out;
    }
}
