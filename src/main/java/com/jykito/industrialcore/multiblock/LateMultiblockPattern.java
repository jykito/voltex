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
 * Жёсткая форма лейт-мультиблока в осях ядра: d вглубь от лица, s вбок, h вверх.
 * Ядро стоит там, где в схеме K (обычно в первой строке; строки перед ним — выступ вперёд), поэтому одна схема
 * подходит для любого поворота.
 * <p>
 * Символы схемы: C обшивка (на её месте можно поставить любой порт), G структурное стекло,
 * M блок механизма (на его месте можно поставить блок улучшения), K ядро, «.» не проверяется.
 * Других замен нет: форма целостная.
 * <p>
 * {@link #advancedTier()} — машина второго тира: продвинутая обшивка и продвинутое стекло вместо обычных.
 * {@link #requiresHeat()} — машине нужен хотя бы один тепловой порт.
 */
public final class LateMultiblockPattern {

    public enum Cell { CASING, GLASS, MECHANISM, CORE, ANY }

    /** Что не так со структурой. Порядок ordinal уходит клиенту в GUI. */
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
     * @param coreLayer индекс слоя (снизу), в котором стоит ядро
     * @param layers    слои снизу вверх; в слое строки от лица вглубь, символы слева направо.
     *                  Ядро — в строке слоя coreLayer, где стоит K; строки до неё выступают перед ядром (плита магнита).
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

    /** Второй тир: продвинутая обшивка и стекло. */
    public LateMultiblockPattern advancedTier() { advanced = true; return this; }

    /** Без теплового порта структура не собрана. */
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
            default -> throw new IllegalArgumentException("неизвестный символ схемы: " + c);
        };
    }

    /** Позиция клетки схемы в мире. Ось s «вправо» берётся глядя из машины наружу, как у криокамеры. */
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
     * Достраивает структуру вокруг ядра. Ставит только в пустые (заменяемые) клетки, правильные не трогает:
     * порты на месте обшивки и улучшения на месте механизма остаются. Порты не ставит — это выбор игрока.
     * В творческом режиме блоки бесплатны, иначе по одному списываются из инвентаря.
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

    /** Тот же критерий, что у {@link #validate}: порт годится вместо обшивки, улучшение вместо механизма. */
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
