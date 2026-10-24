package com.jykito.industrialcore.block.custom.wire;

import com.jykito.industrialcore.block.custom.ConnectedShapes;
import com.jykito.industrialcore.block.custom.cover.Covers;
import net.minecraft.world.level.block.PipeBlock;
import com.jykito.industrialcore.block.entity.BaseWireBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class BaseWireBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
    private final WireTier tier;

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;

    // 4x4 duct up to the face; at a fork, corner or end a 6x6 node (model base_wire_center, T1 busway)
    private static final ConnectedShapes SHAPES = new ConnectedShapes(6, 10, 6, 10, 6, 5, 11);

    public BaseWireBlock(WireTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false)
                .setValue(WATERLOGGED, false).setValue(Covers.COVERED, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BaseWireBlockEntity(pos, state);
    }

    /** WATERLOGGED does not affect the shape and is not part of the mask; a covered cable is a full block. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Covers.shape(state, SHAPES.get(state, PROPERTY_BY_DIRECTION));
    }

    // a cover blocks light and mines like the block it shows
    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return Covers.isCovered(state) ? 15 : super.getLightBlock(state, level, pos); }
    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !Covers.isCovered(state) && super.propagatesSkylightDown(state, level, pos); }
    @Override public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return Covers.destroyProgress(state, player, level, pos, super.getDestroyProgress(state, player, level, pos));
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        Covers.forgetInCreative(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) Covers.dropOnRemove(level, pos);
        super.onRemove(state, level, pos, newState, moving);
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
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, WATERLOGGED, Covers.COVERED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public WireTier getTier() {
        return tier;
    }
}
