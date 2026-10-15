package com.jykito.industrialcore.block.custom.logistics;
import com.mojang.serialization.MapCodec;
import com.jykito.industrialcore.block.BlockUse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;

import com.jykito.industrialcore.block.entity.LogisticsNodeBlockEntity;
import com.jykito.industrialcore.item.custom.LogisticsLinkerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class LogisticsNodeBlock extends BaseEntityBlock {

    /** 1.21: block codec. Vanilla does not call it for mod blocks; there is one block instance, so unit is exact. */
    @Override
    protected MapCodec<? extends LogisticsNodeBlock> codec() { return MapCodec.unit(this); }
    public LogisticsNodeBlock(Properties props) { super(props); }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return BlockUse.toItemResult(use(state, level, pos, player, hand, hit));
    }

    /** Formerly Block.use (1.20.1); called from useItemOn for both hands, as before. */
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof LogisticsLinkerItem) return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LogisticsNodeBlockEntity node)
                sp.openMenu(node, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof LogisticsNodeBlockEntity be)
                be.clearAllLinks(level);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsNodeBlockEntity(pos, state);
    }
}
