package com.jykito.industrialcore.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Thermal station radiator: goes on the back of an expander; auto-orients with the fins facing AWAY from the tower, back towards it. */
public class ThermostationRadiatorBlock extends HorizontalDirectionalBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING; // where the fins face

    public ThermostationRadiatorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // panel shape by FACING: the radiator body sits against the mounting wall, fins face outwards (depth ~6px)
    private static final VoxelShape S_NORTH = Block.box(0, 0, 10, 16, 16, 16); // body at the south wall
    private static final VoxelShape S_SOUTH = Block.box(0, 0, 0, 16, 16, 6);   // at the north
    private static final VoxelShape S_EAST  = Block.box(0, 0, 0, 6, 16, 16);   // at the west
    private static final VoxelShape S_WEST  = Block.box(10, 0, 0, 16, 16, 16); // at the east

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case EAST  -> S_EAST;
            case SOUTH -> S_SOUTH;
            case WEST  -> S_WEST;
            default    -> S_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // if a tower is next to it, the fins face AWAY from it (back to the tower); otherwise from the player
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (ThermostationCoreBlock.isTower(ctx.getLevel().getBlockState(ctx.getClickedPos().relative(d))))
                return defaultBlockState().setValue(FACING, d.getOpposite());
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
}
