package com.jykito.industrialcore.multiblock;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.custom.MechanismUpgradeBlock;
import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed late multiblock shape in core axes: d inwards from the face, s sideways, h up.
 * The core stands where the schema has K (usually the first row; rows before it stick out forward), so one schema
 * fits any rotation.
 * <p>
 * Schema symbols: C casing (any port can stand in its place), G structural glass,
 * M mechanism block (an upgrade block can stand in its place), K core, "." not checked.
 * There are no other substitutions: the shape is fixed.
 * <p>
 * {@link #advancedTier()}: a tier two machine, advanced casing and advanced glass instead of the regular ones.
 * {@link #requiresHeat()}: the machine needs at least one heat port.
 */
public final class LateMultiblockPattern {

    public enum Cell { CASING, GLASS, MECHANISM, CORE, ANY }

    /** What is wrong with the structure. The ordinal goes to the client GUI. */
    public enum Problem { NONE, WRONG_BLOCK, NO_INPUT, NO_OUTPUT, NO_ENERGY, NO_HEAT }

    public record Result(Problem problem, Cell expected, BlockPos at,
                         List<BlockPos> inputs, List<BlockPos> outputs, List<BlockPos> energy, List<BlockPos> heat,
                         int parallelBonus, int speedBlocks, int efficiencyBlocks) {
        public boolean formed() { return problem == Problem.NONE; }
    }

    private final Cell[][][] cells;   // [layer][d][s]
    private final int coreLayer, coreRow, half;
    private boolean advanced, needsHeat;

    /**
     * @param coreLayer index of the layer (from the bottom) the core is in
     * @param layers    layers bottom to top; within a layer rows from the face inwards, symbols left to right.
     *                  The core is in the coreLayer row that has K; rows before it stick out in front of the core (the magnet slab).
     */
    public LateMultiblockPattern(int coreLayer, String[]... layers) {
        this.coreLayer = coreLayer;
        int kRow = 0, k = -1;
        for (int d = 0; d < layers[coreLayer].length && k < 0; d++) {
            k = layers[coreLayer][d].indexOf('K');
            if (k >= 0) kRow = d;
        }
        this.coreRow = kRow;
        this.half = k >= 0 ? k : layers[0][0].length() / 2;
        this.cells = new Cell[layers.length][][];
        for (int h = 0; h < layers.length; h++) {
            cells[h] = new Cell[layers[h].length][];
            for (int d = 0; d < layers[h].length; d++) {
                String row = layers[h][d];
                cells[h][d] = new Cell[row.length()];
                for (int s = 0; s < row.length(); s++) cells[h][d][s] = parse(row.charAt(s));
            }
        }
    }

    /** Tier two: advanced casing and glass. */
    public LateMultiblockPattern advancedTier() { advanced = true; return this; }

    /** Without a heat port the structure is not formed. */
    public LateMultiblockPattern requiresHeat() { needsHeat = true; return this; }

    public boolean needsHeat() { return needsHeat; }

    private Block casing() { return advanced ? ModBlocks.MULTIBLOCK_CASING_ADVANCED.get() : ModBlocks.MULTIBLOCK_CASING.get(); }
    private Block glass()  { return advanced ? ModBlocks.STRUCTURAL_GLASS_ADVANCED.get() : ModBlocks.STRUCTURAL_GLASS.get(); }

    private static Cell parse(char c) {
        return switch (c) {
            case 'C' -> Cell.CASING;
            case 'G' -> Cell.GLASS;
            case 'M' -> Cell.MECHANISM;
            case 'K' -> Cell.CORE;
            case '.' -> Cell.ANY;
            default -> throw new IllegalArgumentException("unknown schema symbol: " + c);
        };
    }

    /** World position of a schema cell. The "right" s axis is taken looking out of the machine, as for the cryo chamber. */
    public BlockPos toWorld(BlockPos core, Direction facing, int h, int d, int s) {
        return core.relative(facing.getOpposite(), d - coreRow).relative(facing.getClockWise(), s - half).above(h - coreLayer);
    }

    public Result validate(Level level, BlockPos core, Direction facing) {
        List<BlockPos> inputs = new ArrayList<>(), outputs = new ArrayList<>(), energy = new ArrayList<>(), heat = new ArrayList<>();
        int parallelBonus = 0, speedBlocks = 0, efficiencyBlocks = 0;
        for (int h = 0; h < cells.length; h++) {
            for (int d = 0; d < cells[h].length; d++) {
                for (int s = 0; s < cells[h][d].length; s++) {
                    Cell want = cells[h][d][s];
                    if (want == Cell.ANY || want == Cell.CORE) continue;
                    BlockPos p = toWorld(core, facing, h, d, s);
                    if (!level.isLoaded(p)) return fail(want, p);
                    BlockState st = level.getBlockState(p);
                    Block b = st.getBlock();
                    boolean ok = switch (want) {
                        case CASING -> {
                            if (b instanceof MultiblockPortBlock port) {
                                switch (port.kind()) {
                                    case INPUT  -> inputs.add(p);
                                    case OUTPUT -> outputs.add(p);
                                    case ENERGY -> energy.add(p);
                                    case HEAT   -> heat.add(p);
                                }
                                yield true;
                            }
                            yield b == casing();
                        }
                        case GLASS -> b == glass();
                        case MECHANISM -> {
                            if (b instanceof MechanismUpgradeBlock up) {
                                switch (up.kind()) {
                                    case PARALLEL -> parallelBonus += up.value();
                                    case SPEED    -> speedBlocks++;
                                    case EFFICIENCY -> efficiencyBlocks++;
                                }
                                yield true;
                            }
                            yield b == ModBlocks.MECHANISM_BLOCK.get();
                        }
                        default -> true;
                    };
                    if (!ok) return fail(want, p);
                }
            }
        }
        Problem missing = inputs.isEmpty() ? Problem.NO_INPUT : outputs.isEmpty() ? Problem.NO_OUTPUT
                : energy.isEmpty() ? Problem.NO_ENERGY : needsHeat && heat.isEmpty() ? Problem.NO_HEAT : Problem.NONE;
        return new Result(missing, Cell.ANY, null, inputs, outputs, energy, heat, parallelBonus, speedBlocks, efficiencyBlocks);
    }

    /**
     * Completes the structure around the core. Places only into empty (replaceable) cells and leaves correct ones alone:
     * ports in place of casing and upgrades in place of mechanisms stay. It does not place ports, that is the player's choice.
     * In creative the blocks are free, otherwise they are taken from the inventory one by one.
     */
    public void build(Level level, BlockPos core, Direction facing, WandBuild wb) {
        for (int h = 0; h < cells.length; h++) {
            for (int d = 0; d < cells[h].length; d++) {
                for (int s = 0; s < cells[h][d].length; s++) {
                    Cell want = cells[h][d][s];
                    if (want == Cell.ANY || want == Cell.CORE) continue;
                    BlockPos p = toWorld(core, facing, h, d, s);
                    if (!level.isLoaded(p)) continue;
                    if (matches(want, level.getBlockState(p).getBlock())) continue;
                    Block put = switch (want) {
                        case GLASS -> glass();
                        case MECHANISM -> ModBlocks.MECHANISM_BLOCK.get();
                        default -> casing();
                    };
                    wb.place(level, p, put.defaultBlockState());
                }
            }
        }
    }

    /** The same criterion as {@link #validate}: a port is fine instead of casing, an upgrade instead of a mechanism. */
    private boolean matches(Cell want, Block b) {
        return switch (want) {
            case CASING -> b instanceof MultiblockPortBlock || b == casing();
            case GLASS -> b == glass();
            case MECHANISM -> b instanceof MechanismUpgradeBlock || b == ModBlocks.MECHANISM_BLOCK.get();
            default -> true;
        };
    }

    private static Result fail(Cell want, BlockPos p) {
        return new Result(Problem.WRONG_BLOCK, want, p, List.of(), List.of(), List.of(), List.of(), 0, 0, 0);
    }
}
