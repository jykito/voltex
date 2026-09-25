package com.jykito.industrialcore.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Стеклянная панель криокамеры.
 * <p>
 * ⚠ НЕ наследуемся от {@code IronBarsBlock}: у ванильной панели логика «цепляйся к любому
 * твёрдому блоку» вшита намертво, и панель липла бы к камню, чужим машинам и обычному стеклу.
 * Здесь соединение — белый список: только блоки самой камеры (см. {@link #connects}).
 * <p>
 * Форма панели зависит от соседей, поэтому {@code getShape} зовётся коллизиями, рейкастом и
 * поиском пути помногу раз за кадр — все 16 комбинаций посчитаны заранее в {@link #SHAPES}.
 */
public class CryoGlassBlock extends Block {

    /**
     * Ось одиночной панели. Соседей нет — ориентироваться не по чему, поэтому запоминаем,
     * куда смотрел игрок при постановке, и разворачиваем панель лицом к нему.
     * Как только сосед появится, ориентацию задаёт связь, а не это свойство.
     */
    public static final EnumProperty<Direction.Axis> AXIS = EnumProperty.create(
            "axis", Direction.Axis.class, Direction.Axis.X, Direction.Axis.Z);

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");

    /**
     * Вертикальные стыки. Геометрию не меняют — только текстуру: когда панель стоит на панели,
     * шов между ними должен пропасть. Поэтому и живут отдельно от четырёх горизонтальных.
     */
    public static final BooleanProperty UP   = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = new EnumMap<>(Direction.class);
    static {
        PROPERTY_BY_DIRECTION.put(Direction.NORTH, NORTH);
        PROPERTY_BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        PROPERTY_BY_DIRECTION.put(Direction.EAST,  EAST);
        PROPERTY_BY_DIRECTION.put(Direction.WEST,  WEST);
        PROPERTY_BY_DIRECTION.put(Direction.UP,    UP);
        PROPERTY_BY_DIRECTION.put(Direction.DOWN,  DOWN);
    }

    /** Порядок бит маски: north, south, east, west. */
    private static final Direction[] HORIZONTAL = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

    /** Плита модели: 16×16×2 поперёк блока. Пока модель одна, дальше формы разойдутся по вариантам. */
    private static final VoxelShape PANEL_NS = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape PANEL_EW = Block.box(7, 0, 0, 9, 16, 16);

    /**
     * Косая панель формой не выражается: {@code VoxelShape} умеет только прямоугольники по осям,
     * диагоналей в движке нет вообще. Поэтому диагональ набирается ЛЕСЕНКОЙ из мелких боксов —
     * иначе на углу выделение выглядит буквой Г и не совпадает с тем, что видно.
     * <p>
     * База — угол СЕВЕРО-ВОСТОК: отрезок из середины северной грани (8,0) в середину
     * восточной (16,8), то есть по линии {@code z = x - 8}, толщиной 2 поперёк.
     */
    private static VoxelShape cornerNE() {
        VoxelShape shape = Shapes.empty();
        final double halfBand = Math.sqrt(2);           // 2 поперёк наклона = 2.83 вдоль Z, половина от неё
        for (int i = 0; i < 8; i++) {
            double x0 = 8 + i, x1 = x0 + 1;
            double zc = (x0 + x1) / 2 - 8;              // центр полосы на этом шаге
            double z0 = Math.max(0, zc - halfBand), z1 = Math.min(16, zc + halfBand);
            shape = Shapes.or(shape, Block.box(x0, 0, z0, x1, 16, z1));
        }
        return shape;
    }

    /** Поворот формы на 90° по часовой (вид сверху): (x,z) -> (16-z, x). */
    private static VoxelShape rotateCW(VoxelShape src) {
        VoxelShape[] out = { Shapes.empty() };
        src.forAllBoxes((x0, y0, z0, x1, y1, z1) ->
                out[0] = Shapes.or(out[0], Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1)));
        return out[0];
    }

    private static final VoxelShape[] SHAPES = new VoxelShape[16];
    static {
        // углы: индекс 0 = северо-восток, дальше по часовой, как в блокстейте
        VoxelShape[] corner = new VoxelShape[4];
        corner[0] = cornerNE();
        for (int i = 1; i < 4; i++) corner[i] = rotateCW(corner[i - 1]);

        for (int mask = 0; mask < 16; mask++) {
            boolean n = (mask & 1) != 0, s = (mask & 2) != 0, e = (mask & 4) != 0, w = (mask & 8) != 0;
            boolean ns = n || s, ew = e || w;
            VoxelShape shape;

            if (ns && ew) {
                // смежная пара = угол, ориентация та же, что у модели в блокстейте
                if (n && e)      shape = corner[0];
                else if (e && s) shape = corner[1];
                else if (s && w) shape = corner[2];
                else             shape = corner[3];
            } else if (ns) {
                shape = PANEL_EW;                        // стена идёт север-юг
            } else if (ew) {
                shape = PANEL_NS;                        // стена идёт восток-запад
            } else {
                shape = PANEL_NS;                        // одиночная панель
            }
            SHAPES[mask] = shape.optimize();
        }
    }

    public CryoGlassBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false)
                .setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        for (int i = 0; i < HORIZONTAL.length; i++)
            if (state.getValue(PROPERTY_BY_DIRECTION.get(HORIZONTAL[i]))) mask |= 1 << i;
        // одиночная панель формы не имеет от связей — берём по оси, куда её поставил игрок
        if (mask == 0) return state.getValue(AXIS) == Direction.Axis.X ? PANEL_EW : PANEL_NS;
        return SHAPES[mask];
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Панель ставим лицом к игроку: её плоскость поперёк взгляда, то есть ось совпадает
        // с осью направления взгляда. Смотрит на север или юг — панель идёт восток-запад.
        BlockState state = defaultBlockState()
                .setValue(AXIS, ctx.getHorizontalDirection().getAxis());
        for (Direction d : Direction.values())
            state = state.setValue(PROPERTY_BY_DIRECTION.get(d), connects(ctx.getLevel(), ctx.getClickedPos(), d));
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty prop = PROPERTY_BY_DIRECTION.get(dir);
        return prop == null ? state : state.setValue(prop, connects(level, pos, dir));
    }

    /**
     * Белый список: панель цепляется ТОЛЬКО к блокам криокамеры.
     * ⚠ Сюда же дописывать структурный блок, внутренний холодильник и ядро, когда появятся —
     * иначе панель у стены камеры не увидит угол и не станет косой.
     */
    private static boolean connects(BlockGetter level, BlockPos pos, Direction dir) {
        return level.getBlockState(pos.relative(dir)).getBlock() instanceof CryoGlassBlock;
    }
}
