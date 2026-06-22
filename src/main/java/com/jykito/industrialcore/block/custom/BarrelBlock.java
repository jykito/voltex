package com.jykito.industrialcore.block.custom;

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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public class BarrelBlock extends BaseEntityBlock {

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
