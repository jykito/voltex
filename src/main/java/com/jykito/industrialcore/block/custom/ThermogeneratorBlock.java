package com.jykito.industrialcore.block.custom;
import com.mojang.serialization.MapCodec;
import com.jykito.industrialcore.capability.Caps;

import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.block.entity.ThermogeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class ThermogeneratorBlock extends BaseEntityBlock {

    /** 1.21: block codec. Vanilla does not call it for mod blocks; there is one block instance, so unit is exact. */
    @Override
    protected MapCodec<? extends ThermogeneratorBlock> codec() { return MapCodec.unit(this); }
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ThermogeneratorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    /**
     * The face turns by itself to a neighbour that can accept heat: heat only goes
     * forward, and you no longer need to aim the side when placing. The target is found by capability,
     * not by a specific block: the thermal station, heat pipe and machines like the liquefier
     * all fit the rule, and new consumers are picked up automatically.
     * <p>
     * The player's view has priority: if there is a consumer both where you look and to the side,
     * your choice wins. With no consumers nearby the old rule by view applies.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction look = context.getNearestLookingDirection().getOpposite();
        Direction target = acceptsHeat(context, look) ? look : null;
        if (target == null) {
            for (Direction d : Direction.values()) {
                if (acceptsHeat(context, d)) { target = d; break; }
            }
        }
        return this.defaultBlockState().setValue(FACING, target != null ? target : look);
    }

    /** Whether something behind this side accepts heat. */
    private static boolean acceptsHeat(BlockPlaceContext context, Direction side) {
        BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos().relative(side));
        return be != null
                && Caps.get(be, Caps.HEAT, side.getOpposite()).isPresent();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThermogeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.THERMOGENERATOR_BE.get(),
                (lvl, pos, st, be) -> ((ThermogeneratorBlockEntity) be).tick(lvl, pos, st));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ThermogeneratorBlockEntity) {
                Caps.get(blockEntity, Caps.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                    }
                });
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);

    }
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack heldItem, BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return com.jykito.industrialcore.block.BlockUse.toItemResult(use(state, level, pos, player, hand, hit));
    }

    /** Formerly Block.use (1.20.1); called from useItemOn for both hands, as before. */
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof com.jykito.industrialcore.block.entity.ThermogeneratorBlockEntity) {
                (((net.minecraft.server.level.ServerPlayer) player)).openMenu((com.jykito.industrialcore.block.entity.ThermogeneratorBlockEntity) entity, pos);
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
    }
}
