package com.jykito.industrialcore.block.custom;

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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Каркас промышленного шахтёра. В собранной структуре невидим (её рисует модель ядра), отдаёт энергию, жидкость
 * и предметы ядру и открывает его окно — трубы и кабели можно подводить к любому блоку каркаса.
 */
public class IndustrialMinerFrameBlock extends BaseEntityBlock {

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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(FORMED)) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IndustrialMinerFrameBlockEntity frame) {
            IndustrialMinerCoreBlockEntity core = frame.core();
            if (core != null) NetworkHooks.openScreen((ServerPlayer) player, core, core.getBlockPos());
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Сломали блок собранного каркаса — структура разбирается сразу, не дожидаясь проверки ядра. */
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
