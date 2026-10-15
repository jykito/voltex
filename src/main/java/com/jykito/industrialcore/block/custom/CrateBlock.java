package com.jykito.industrialcore.block.custom;
import com.mojang.serialization.MapCodec;
import com.jykito.industrialcore.block.BlockUse;
import net.minecraft.world.ItemInteractionResult;
import com.jykito.industrialcore.capability.Caps;

import com.jykito.industrialcore.block.entity.CrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CrateBlock extends BaseEntityBlock {

    /** 1.21: block codec. Vanilla does not call it for mod blocks; there is one block instance, so unit is exact. */
    @Override
    protected MapCodec<? extends CrateBlock> codec() { return MapCodec.unit(this); }

    public final int rows;

    public CrateBlock(int rows, Properties props) {
        super(props);
        this.rows = rows;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return BlockUse.toItemResult(use(state, level, pos, player, hand, hit));
    }

    /** Formerly Block.use (1.20.1); called from useItemOn for both hands, as before. */
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CrateBlockEntity be) {
            level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.6f, 0.75f);
            ((ServerPlayer) player).openMenu(be, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof CrateBlockEntity be) {
                Caps.get(be, Caps.ITEM_HANDLER).ifPresent(h -> {
                    for (int i = 0; i < h.getSlots(); i++) {
                        ItemStack s = h.getStackInSlot(i);
                        if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s);
                    }
                });
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
