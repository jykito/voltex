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

/** Радиатор термостанции: ставится на зад расширителя; авто-ориентация рёбрами НАРУЖУ от башни, задником к ней. */
public class ThermostationRadiatorBlock extends HorizontalDirectionalBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING; // куда смотрят рёбра

    public ThermostationRadiatorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // форма-панель по FACING: тело радиатора у стенки-крепления, рёбра смотрят наружу (глубина ~6px)
    private static final VoxelShape S_NORTH = Block.box(0, 0, 10, 16, 16, 16); // тело у южной стены
    private static final VoxelShape S_SOUTH = Block.box(0, 0, 0, 16, 16, 6);   // у северной
    private static final VoxelShape S_EAST  = Block.box(0, 0, 0, 6, 16, 16);   // у западной
    private static final VoxelShape S_WEST  = Block.box(10, 0, 0, 16, 16, 16); // у восточной

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
        // если рядом башня — рёбра смотрят ОТ неё (задник к башне); иначе по игроку
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (ThermostationCoreBlock.isTower(ctx.getLevel().getBlockState(ctx.getClickedPos().relative(d))))
                return defaultBlockState().setValue(FACING, d.getOpposite());
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
}
