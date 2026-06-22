package com.jykito.industrialcore.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class RubberLogBlock extends RotatedPillarBlock {
    public static final IntegerProperty LATEX_STATE = IntegerProperty.create("latex_state", 0, 2);
    public static final DirectionProperty LATEX_FACING = DirectionProperty.create("latex_facing", Direction.Plane.HORIZONTAL);

    public RubberLogBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(LATEX_STATE, 0)
                .setValue(LATEX_FACING, Direction.NORTH));
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LATEX_STATE) == 1;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LATEX_STATE) == 1 && random.nextInt(11) == 0) {
            level.setBlock(pos, state.setValue(LATEX_STATE, 2), 3);
        }
        super.randomTick(state, level, pos, random);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LATEX_STATE, LATEX_FACING);
    }
}
