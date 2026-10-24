package com.jykito.industrialcore.block.custom.wind;
import com.jykito.industrialcore.block.BlockUse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lattice mast section of a wind turbine. No block entity: the model is static. The lattice narrows towards the top,
 * so each section takes its model by {@link #LEVEL}, the number of sections under it (capped at the narrowest model).
 */
public class WindTurbineMastBlock extends Block {

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, WindTurbine.MODEL_LEVELS - 1);

    /** Outline per level: the lattice is 14 px wide at the base and loses 0.6 px per section. */
    private static final VoxelShape[] SHAPES = new VoxelShape[WindTurbine.MODEL_LEVELS];
    static {
        for (int l = 0; l < SHAPES.length; l++) {
            double half = 7 - 0.3 * l;
            SHAPES[l] = Block.box(8 - half, 0, 8 - half, 8 + half, 16, 8 + half);
        }
    }

    public WindTurbineMastBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    private static int levelOver(BlockState below) {
        return below.getBlock() instanceof WindTurbineMastBlock ? Math.min(WindTurbine.MODEL_LEVELS - 1, below.getValue(LEVEL) + 1) : 0;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(LEVEL, levelOver(ctx.getLevel().getBlockState(ctx.getClickedPos().below())));
    }

    /** A section below changed: take the new level; the change runs up the mast through the same call. */
    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dir == Direction.DOWN ? state.setValue(LEVEL, levelOver(neighbor)) : state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(LEVEL)];
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return BlockUse.toItemResult(use(state, level, pos, player, hand, hit));
    }

    /** Formerly Block.use (1.20.1); called from useItemOn for both hands, as before. */
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return WindTurbine.showStatus(level, pos, player, hand);
    }
}
