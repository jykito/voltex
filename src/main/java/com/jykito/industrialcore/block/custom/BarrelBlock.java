package com.jykito.industrialcore.block.custom;
import com.mojang.serialization.MapCodec;
import com.jykito.industrialcore.block.BlockUse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;

import com.jykito.industrialcore.block.entity.BarrelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public class BarrelBlock extends BaseEntityBlock {

    /** 1.21: block codec. Vanilla does not call it for mod blocks; there is one block instance, so unit is exact. */
    @Override
    protected MapCodec<? extends BarrelBlock> codec() { return MapCodec.unit(this); }

    public final int capacity;

    public BarrelBlock(int capacity, Properties props) {
        super(props);
        this.capacity = capacity;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BarrelBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return BlockUse.toItemResult(use(state, level, pos, player, hand, hit));
    }

    /** Formerly Block.use (1.20.1); called from useItemOn for both hands, as before. */
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {

            boolean did = FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection());
            if (!did && level.getBlockEntity(pos) instanceof BarrelBlockEntity be) {

                FluidStack f = be.getFluidView();
                Component msg = f.isEmpty()
                        ? Component.translatable("message.industrial_core.barrel_empty")
                        : Component.translatable("message.industrial_core.barrel_contents",
                                f.getDisplayName(), f.getAmount(), be.getCapacity());
                player.displayClientMessage(msg, true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
