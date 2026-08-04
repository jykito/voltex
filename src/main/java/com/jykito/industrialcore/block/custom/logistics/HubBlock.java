package com.jykito.industrialcore.block.custom.logistics;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.HubBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.item.custom.LogisticsLinkerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class HubBlock extends BaseEntityBlock {
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
        PROPERTY_BY_DIRECTION.put(Direction.EAST, EAST);
        PROPERTY_BY_DIRECTION.put(Direction.WEST, WEST);
        PROPERTY_BY_DIRECTION.put(Direction.UP, UP);
        PROPERTY_BY_DIRECTION.put(Direction.DOWN, DOWN);
    }

    private static final VoxelShape CORE = box(1, 1, 1, 15, 15, 15);
    private static final Map<Direction, VoxelShape> STUBS = Map.of(
            Direction.NORTH, box(4, 4,  0, 12, 12,  3),
            Direction.SOUTH, box(4, 4, 13, 12, 12, 16),
            Direction.EAST,  box(13, 4, 4, 16, 12, 12),
            Direction.WEST,  box( 0, 4, 4,  3, 12, 12),
            Direction.UP,    box(4, 13, 4, 12, 16, 12),
            Direction.DOWN,  box(4,  0, 4, 12,  3, 12)
    );

    public HubBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    /** Готовые формы на все 64 комбинации: getShape зовут коллизии и луч прицела, пересобирать каждый раз незачем. */
    private static final VoxelShape[] SHAPES = new VoxelShape[64];
    static {
        for (int mask = 0; mask < SHAPES.length; mask++) {
            VoxelShape shape = CORE;
            for (Direction d : Direction.values())
                if ((mask & (1 << d.get3DDataValue())) != 0) shape = Shapes.or(shape, STUBS.get(d));
            SHAPES[mask] = shape.optimize();
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
        return updateConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty prop = PROPERTY_BY_DIRECTION.get(dir);
        return prop == null ? state : state.setValue(prop, !neighborState.isAir());
    }

    private static BlockState updateConnections(BlockState state, LevelReader level, BlockPos pos) {
        for (Map.Entry<Direction, BooleanProperty> e : PROPERTY_BY_DIRECTION.entrySet())
            state = state.setValue(e.getValue(), !level.getBlockState(pos.relative(e.getKey())).isAir());
        return state;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof LogisticsLinkerItem) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HubBlockEntity be)
            NetworkHooks.openScreen((ServerPlayer) player, be, pos);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof HubBlockEntity be) {
                be.clearAllLinks(level);
                for (int i = 0; i < 6; i++) {
                    ItemStack up = be.upgradeHandler.getStackInSlot(i);
                    if (!up.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), up);
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HubBlockEntity(pos, state, state.is(ModBlocks.FLUID_HUB.get()));
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.ITEM_HUB_BE.get(), HubBlockEntity::serverTick);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }
}
