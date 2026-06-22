package com.jykito.industrialcore.block;

import com.jykito.industrialcore.block.entity.NuclearReactorBlockEntity;
import com.jykito.industrialcore.block.entity.ReactorChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class ReactorChamberBlock extends Block implements EntityBlock {

    public ReactorChamberBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorChamberBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockEntity be = level.getBlockEntity(pos.offset(dx, dy, dz));
                        if (be instanceof NuclearReactorBlockEntity reactor) {
                            if (!NuclearReactorBlockEntity.checkStructure(level, reactor.getBlockPos())) {
                                player.sendSystemMessage(Component.translatable("msg.industrial_core.reactor_not_formed"));
                                return InteractionResult.CONSUME;
                            }
                            NetworkHooks.openScreen((ServerPlayer) player, reactor, reactor.getBlockPos());
                            return InteractionResult.CONSUME;
                        }
                    }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
