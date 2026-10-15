package com.jykito.industrialcore.block.custom.logistics;
import com.mojang.serialization.MapCodec;
import com.jykito.industrialcore.block.BlockUse;
import net.minecraft.world.ItemInteractionResult;

import com.jykito.industrialcore.block.custom.ConnectedShapes;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.PipeBlock;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class HubBlock extends BaseEntityBlock {

    /** 1.21: block codec. Vanilla does not call it for mod blocks; there is one block instance, so unit is exact. */
    @Override
    protected MapCodec<? extends HubBlock> codec() { return MapCodec.unit(this); }
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;

    // 12x12 box with corners, connections 6x6, 2 deep (models item_hub / *_hub_port)
    private static final ConnectedShapes SHAPES = new ConnectedShapes(2, 14, 5, 11, 2);

    public HubBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.get(state, PROPERTY_BY_DIRECTION);
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
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return BlockUse.toItemResult(use(state, level, pos, player, hand, hit));
    }

    /** Formerly Block.use (1.20.1); called from useItemOn for both hands, as before. */
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof LogisticsLinkerItem) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HubBlockEntity be)
            ((ServerPlayer) player).openMenu(be, pos);
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
