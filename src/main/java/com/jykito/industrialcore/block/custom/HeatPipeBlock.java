package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.HeatPipeBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.heat.HeatCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class HeatPipeBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");
    public static final BooleanProperty HOT   = BooleanProperty.create("hot"); // статик-свечение по нагреву (blockstate)

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = new EnumMap<>(Direction.class);
    static {
        PROPERTY_BY_DIRECTION.put(Direction.NORTH, NORTH);
        PROPERTY_BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        PROPERTY_BY_DIRECTION.put(Direction.EAST, EAST);
        PROPERTY_BY_DIRECTION.put(Direction.WEST, WEST);
        PROPERTY_BY_DIRECTION.put(Direction.UP, UP);
        PROPERTY_BY_DIRECTION.put(Direction.DOWN, DOWN);
    }

    private static final VoxelShape CORE = box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> STUBS = Map.of(
            Direction.NORTH, box(5, 5,  0, 11, 11,  4),
            Direction.SOUTH, box(5, 5, 12, 11, 11, 16),
            Direction.EAST,  box(12, 5, 5, 16, 11, 11),
            Direction.WEST,  box( 0, 5, 5,  4, 11, 11),
            Direction.UP,    box(5, 12, 5, 11, 16, 11),
            Direction.DOWN,  box(5,  0, 5, 11,  4, 11)
    );

    public HeatPipeBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false).setValue(HOT, false));
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    /**
     * Готовые формы на все 64 комбинации соединений.
     * <p>
     * Раньше форма собиралась заново на КАЖДЫЙ вызов getShape (до шести Shapes.or с аллокацией),
     * а зовут его постоянно: коллизии сущностей, луч прицела каждый кадр, пасфайндинг, обновления
     * соседей. В профиле это давало больше, чем весь тик теплосети. Комбинаций всего 64 —
     * считаем один раз при загрузке класса. Форма от нагрева не зависит, HOT в маску не входит.
     */
    private static final VoxelShape[] SHAPES = new VoxelShape[64];
    static {
        for (int mask = 0; mask < SHAPES.length; mask++) {
            VoxelShape shape = CORE;
            for (Direction d : Direction.values())
                if ((mask & (1 << d.get3DDataValue())) != 0) shape = Shapes.or(shape, STUBS.get(d));
            SHAPES[mask] = shape.optimize();   // разово упрощаем — все последующие проверки дешевле
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        for (Map.Entry<Direction, BooleanProperty> e : PROPERTY_BY_DIRECTION.entrySet())
            if (state.getValue(e.getValue())) mask |= 1 << e.getKey().get3DDataValue();
        return SHAPES[mask];
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = defaultBlockState();
        for (Map.Entry<Direction, BooleanProperty> e : PROPERTY_BY_DIRECTION.entrySet())
            state = state.setValue(e.getValue(), connects(ctx.getLevel(), ctx.getClickedPos(), e.getKey()));
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty prop = PROPERTY_BY_DIRECTION.get(dir);
        return prop == null ? state : state.setValue(prop, connects(level, pos, dir));
    }

    /** Соединяемся с другой теплотрубой или с любым блоком, у которого есть тепло-capability. */
    private static boolean connects(BlockGetter level, BlockPos pos, Direction dir) {
        // сторона отключена ключом на этой трубе
        if (level.getBlockEntity(pos) instanceof HeatPipeBlockEntity self && self.isDisabled(dir)) return false;
        BlockPos np = pos.relative(dir);
        if (level.getBlockState(np).is(ModBlocks.HEAT_PIPE.get())) {
            // соседняя труба отключила свою сторону к нам — не соединяемся (симметрично)
            return !(level.getBlockEntity(np) instanceof HeatPipeBlockEntity npe && npe.isDisabled(dir.getOpposite()));
        }
        BlockEntity be = level.getBlockEntity(np);
        return be != null && be.getCapability(HeatCapability.HEAT, dir.getOpposite()).isPresent();
    }

    /** Пересчитать соединения блока (после переключения стороны ключом). */
    public static void recomputeConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof HeatPipeBlock)) return;
        for (Map.Entry<Direction, BooleanProperty> e : PROPERTY_BY_DIRECTION.entrySet())
            state = state.setValue(e.getValue(), connects(level, pos, e.getKey()));
        level.setBlock(pos, state, 3);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatPipeBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.HEAT_PIPE_BE.get(),
                level.isClientSide ? HeatPipeBlockEntity::clientTick : HeatPipeBlockEntity::serverTick);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, HOT);
    }
}
