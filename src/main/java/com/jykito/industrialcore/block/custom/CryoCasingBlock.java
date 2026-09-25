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
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Структурный блок криокамеры.
 * <p>
 * Кладка должна читаться цельной плитой с рамкой ТОЛЬКО по внешнему периметру. Поэтому
 * рамка — не часть текстуры, а двенадцать брусков по рёбрам куба: ребро показывается,
 * если у обеих смежных с ним сторон нет соседа. Внутри кладки все рёбра гасятся сами.
 * <p>
 * Так вместо шестнадцати вариантов текстуры на грань нужны две текстуры на весь блок:
 * бесшовная заливка и материал рамки.
 * <p>
 * Корпус ещё и <b>принимает тепло за ядро</b>: у ядра шесть граней, а глубокой ступени нужно
 * несколько линий труб. Подробности в {@link com.jykito.industrialcore.block.entity.CryoCasingBlockEntity}.
 */
public class CryoCasingBlock extends Block implements net.minecraft.world.level.block.EntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = new EnumMap<>(Direction.class);
    static {
        PROPERTY_BY_DIRECTION.put(Direction.NORTH, NORTH);
        PROPERTY_BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        PROPERTY_BY_DIRECTION.put(Direction.EAST,  EAST);
        PROPERTY_BY_DIRECTION.put(Direction.WEST,  WEST);
        PROPERTY_BY_DIRECTION.put(Direction.UP,    UP);
        PROPERTY_BY_DIRECTION.put(Direction.DOWN,  DOWN);
    }

    public CryoCasingBlock(Properties props) {
        super(props);
        BlockState s = this.stateDefinition.any();
        for (BooleanProperty p : PROPERTY_BY_DIRECTION.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = defaultBlockState();
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
     * Рамка гасится ТОЛЬКО другим корпусом. Стекло, ядро и всё остальное соседями не считаются:
     * стык корпуса с ними должен быть обведён, кладка обводится только по своему контуру.
     * ⚠ Ядро сюда добавить, когда появится — юзер решил заводить его отдельно.
     */
    private static boolean connects(BlockGetter level, BlockPos pos, Direction dir) {
        return level.getBlockState(pos.relative(dir)).getBlock() instanceof CryoCasingBlock;
    }

    /** Сущность только переадресует тепло ядру и не тикает — тикера у блока намеренно нет. */
    @Nullable
    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.jykito.industrialcore.block.entity.CryoCasingBlockEntity(pos, state);
    }
}
