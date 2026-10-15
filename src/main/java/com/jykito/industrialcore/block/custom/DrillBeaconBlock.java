package com.jykito.industrialcore.block.custom;
import com.mojang.serialization.MapCodec;
import com.jykito.industrialcore.block.BlockUse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;

import com.jykito.industrialcore.block.entity.DrillBeaconBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Drill beacon: a tripod with a probe. Drawn entirely by DrillBeaconBER (the crystal spins, waves run over the ground while scanning). */
public class DrillBeaconBlock extends BaseEntityBlock {

    /** 1.21: block codec. Vanilla does not call it for mod blocks; there is one block instance, so unit is exact. */
    @Override
    protected MapCodec<? extends DrillBeaconBlock> codec() { return MapCodec.unit(this); }

    private static final VoxelShape SHAPE = Block.box(1.5, 0, 1.5, 14.5, 24, 14.5);

    public DrillBeaconBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DrillBeaconBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.DRILL_BEACON_BE.get(),
                (lvl, pos, st, be) -> be.tick(lvl));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return BlockUse.toItemResult(use(state, level, pos, player, hand, hit));
    }

    /** Formerly Block.use (1.20.1); called from useItemOn for both hands, as before. */
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DrillBeaconBlockEntity be)
            ((ServerPlayer) player).openMenu(be, pos);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** When broken the lens drops and the area leaves the registry (the miner stops mining it). */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof DrillBeaconBlockEntity be) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.getLens().getStackInSlot(0));
            be.unpublish();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
