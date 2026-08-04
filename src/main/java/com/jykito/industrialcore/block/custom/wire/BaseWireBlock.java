package com.jykito.industrialcore.block.custom.wire;

import com.jykito.industrialcore.block.entity.BaseWireBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BaseWireBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
    private final WireTier tier;

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = new HashMap<>() {{
        put(Direction.NORTH, NORTH);
        put(Direction.SOUTH, SOUTH);
        put(Direction.EAST, EAST);
        put(Direction.WEST, WEST);
        put(Direction.UP, UP);
        put(Direction.DOWN, DOWN);
    }};

    private static final VoxelShape CENTER_SHAPE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape UP_SHAPE = Block.box(5, 11, 5, 11, 16, 11);
    private static final VoxelShape DOWN_SHAPE = Block.box(5, 0, 5, 11, 5, 11);
    private static final VoxelShape NORTH_SHAPE = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 5, 5, 5, 11, 11);

    public BaseWireBlock(WireTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false)
                .setValue(WATERLOGGED, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BaseWireBlockEntity(pos, state);
    }

    /**
     * Готовые формы на все 64 комбинации соединений. Форму раньше собирали заново на КАЖДЫЙ
     * вызов getShape, а зовут его коллизии, луч прицела, пасфайндинг и обновления соседей —
     * то есть очень часто, а кабелей на базе больше всего. Считаем один раз при загрузке.
     * WATERLOGGED на форму не влияет и в маску не входит.
     */
    private static final VoxelShape[] SHAPES = new VoxelShape[64];
    static {
        VoxelShape[] byDir = {
                DOWN_SHAPE, UP_SHAPE, NORTH_SHAPE, SOUTH_SHAPE, WEST_SHAPE, EAST_SHAPE
        }; // порядок как у Direction.get3DDataValue()
        for (int mask = 0; mask < SHAPES.length; mask++) {
            VoxelShape shape = CENTER_SHAPE;
            for (int i = 0; i < 6; i++)
                if ((mask & (1 << i)) != 0) shape = Shapes.or(shape, byDir[i]);
            SHAPES[mask] = shape.optimize();
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Map.Entry<Direction, BooleanProperty> e : PROPERTY_BY_DIRECTION.entrySet())
            if (state.getValue(e.getValue())) mask |= 1 << e.getKey().get3DDataValue();
        return SHAPES[mask];
    }

    private boolean canConnectTo(LevelAccessor level, BlockPos currentPos, Direction direction) {
        BlockPos neighborPos = currentPos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);

        if (neighborState.getBlock() instanceof BaseWireBlock) {
            return true;
        }

        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be != null) {
            var capability = be.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite());
            return capability.isPresent();
        }
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = level.getFluidState(pos);
        BlockState state = this.defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(level, pos, direction));
        }
        return state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), canConnectTo(level, currentPos, direction));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public WireTier getTier() {
        return tier;
    }
}
