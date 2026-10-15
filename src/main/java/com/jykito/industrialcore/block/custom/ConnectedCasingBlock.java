package com.jykito.industrialcore.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Connected casing without contents: the frame is drawn only along the outer outline of the masonry,
 * like {@link CryoCasingBlock}, but without a block entity or heat. Connects only to the same block.
 */
public class ConnectedCasingBlock extends Block {

    public ConnectedCasingBlock(Properties props) {
        super(props);
        BlockState s = this.stateDefinition.any();
        for (BooleanProperty p : PipeBlock.PROPERTY_BY_DIRECTION.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(PipeBlock.NORTH, PipeBlock.SOUTH, PipeBlock.EAST, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values())
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d), connects(ctx.getLevel(), ctx.getClickedPos(), d));
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), neighborState.is(this));
    }

    private boolean connects(BlockGetter level, BlockPos pos, Direction dir) {
        return level.getBlockState(pos.relative(dir)).is(this);
    }
}
