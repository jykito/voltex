package com.jykito.industrialcore.block.custom;
import com.mojang.serialization.MapCodec;
import com.jykito.industrialcore.block.BlockUse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;

import com.jykito.industrialcore.block.entity.IndustrialMinerCoreBlockEntity;
import com.jykito.industrialcore.block.entity.IndustrialMinerFrameBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Industrial miner frame. Invisible in a formed structure (the core model draws it), passes energy, fluid
 * and items to the core and opens its window; pipes and cables can attach to any frame block.
 */
public class IndustrialMinerFrameBlock extends BaseEntityBlock {

    /** 1.21: block codec. Vanilla does not call it for mod blocks; there is one block instance, so unit is exact. */
    @Override
    protected MapCodec<? extends IndustrialMinerFrameBlock> codec() { return MapCodec.unit(this); }

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public IndustrialMinerFrameBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IndustrialMinerFrameBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return BlockUse.toItemResult(use(state, level, pos, player, hand, hit));
    }

    /** Formerly Block.use (1.20.1); called from useItemOn for both hands, as before. */
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(FORMED)) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IndustrialMinerFrameBlockEntity frame) {
            IndustrialMinerCoreBlockEntity core = frame.core();
            if (core != null) ((ServerPlayer) player).openMenu(core, core.getBlockPos());
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** A block of a formed frame was broken: the structure unforms at once, without waiting for the core check. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && state.getValue(FORMED)
                && level.getBlockEntity(pos) instanceof IndustrialMinerFrameBlockEntity frame) {
            IndustrialMinerCoreBlockEntity core = frame.core();
            if (core != null) core.unform();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
