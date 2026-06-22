package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.entity.MinerCasingBlockEntity;
import com.jykito.industrialcore.block.entity.MinerCoreBlockEntity;
import com.jykito.industrialcore.block.custom.MinerCoreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MinerCasingBlock extends BaseEntityBlock {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public MinerCasingBlock(BlockBehaviour.Properties props) {
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
        return new MinerCasingBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (level.getBlockEntity(pos.offset(dx, dy, dz)) instanceof MinerCoreBlockEntity core) {
                            if (!level.getBlockState(core.getBlockPos()).getValue(MinerCoreBlock.FORMED)) {
                                player.sendSystemMessage(Component.translatable("msg.industrial_core.miner_not_formed"));
                                return InteractionResult.CONSUME;
                            }
                            NetworkHooks.openScreen((ServerPlayer) player, core, core.getBlockPos());
                            return InteractionResult.CONSUME;
                        }
                    }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
