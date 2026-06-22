package com.jykito.industrialcore.block.custom.logistics;

import com.jykito.industrialcore.block.entity.FluidConnectorBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.item.custom.LogisticsLinkerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FluidConnectorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty PULL = BooleanProperty.create("pull");

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, box(4, 4,  0, 12, 12,  3),
            Direction.SOUTH, box(4, 4, 13, 12, 12, 16),
            Direction.EAST,  box(13, 4, 4, 16, 12, 12),
            Direction.WEST,  box( 0, 4, 4,  3, 12, 12),
            Direction.UP,    box(4, 13, 4, 12, 16, 12),
            Direction.DOWN,  box(4,  0, 4, 12,  3, 12)
    );

    public FluidConnectorBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PULL, false));
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPES.get(s.getValue(FACING));
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite()).setValue(PULL, false);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.relative(state.getValue(FACING));
        return !level.getBlockState(support).isAir();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == state.getValue(FACING) && !state.canSurvive(level, pos))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighborState, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof LogisticsLinkerItem) return InteractionResult.PASS;

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidConnectorBlockEntity be)
            NetworkHooks.openScreen((ServerPlayer) player, be, pos);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof FluidConnectorBlockEntity be) {
                be.clearAllLinks(level);
                var up = be.upgradeHandler.getStackInSlot(0);
                if (!up.isEmpty())
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), up);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidConnectorBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.FLUID_CONNECTOR_BE.get(), FluidConnectorBlockEntity::serverTick);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        b.add(FACING, PULL);
    }
}
