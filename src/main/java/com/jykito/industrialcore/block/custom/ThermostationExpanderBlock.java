package com.jykito.industrialcore.block.custom;
import com.mojang.serialization.MapCodec;

import com.jykito.industrialcore.block.entity.ThermostationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/** Thermal station expander: +2 thermogenerator faces, +1 cooling at the back; FACING is inherited from the block below (the core). */
public class ThermostationExpanderBlock extends BaseEntityBlock {

    /** 1.21: block codec. Vanilla does not call it for mod blocks; there is one block instance, so unit is exact. */
    @Override
    protected MapCodec<? extends ThermostationExpanderBlock> codec() { return MapCodec.unit(this); }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty UP   = ThermostationCoreBlock.UP;
    public static final BooleanProperty DOWN = ThermostationCoreBlock.DOWN;

    public ThermostationExpanderBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(UP, false).setValue(DOWN, false));
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level l = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState below = l.getBlockState(pos.below());
        // take the orientation from the tower below (core/expander), it snaps to the shared FACING; otherwise from the player
        Direction facing = ThermostationCoreBlock.isTower(below) && below.hasProperty(FACING)
                ? below.getValue(FACING)
                : ctx.getHorizontalDirection().getOpposite();
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(UP,   ThermostationCoreBlock.isTower(l.getBlockState(pos.above())))
                .setValue(DOWN, ThermostationCoreBlock.isTower(below));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState nbr, LevelAccessor l, BlockPos pos, BlockPos nbrPos) {
        if (dir == Direction.UP)   return state.setValue(UP, ThermostationCoreBlock.isTower(nbr));
        if (dir == Direction.DOWN) return state.setValue(DOWN, ThermostationCoreBlock.isTower(nbr));
        return state;
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new ThermostationBlockEntity(pos, s); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, UP, DOWN); }
}
