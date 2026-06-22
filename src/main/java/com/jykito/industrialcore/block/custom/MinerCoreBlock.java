package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.entity.MinerCoreBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MinerCoreBlock extends BaseEntityBlock {
    public static final BooleanProperty   LIT    = BlockStateProperties.LIT;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty   FORMED = BooleanProperty.create("formed");

    public MinerCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, false).setValue(FACING, Direction.NORTH).setValue(FORMED, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, LIT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MinerCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MINER_CORE_BE.get(),
                (lvl, pos, st, be) -> ((MinerCoreBlockEntity) be).tick(lvl, pos, st));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MinerCoreBlockEntity core) {
                if (!state.getValue(MinerCoreBlock.FORMED)) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("msg.industrial_core.miner_not_formed"));
                    return InteractionResult.CONSUME;
                }
                NetworkHooks.openScreen((ServerPlayer) player, core, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MinerCoreBlockEntity core) {
                net.minecraftforge.items.ItemStackHandler inv = core.getItemHandler();
                for (int i = 0; i < inv.getSlots(); i++)
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i));
                net.minecraftforge.items.ItemStackHandler out = core.getOutputHandler();
                for (int i = 0; i < out.getSlots(); i++)
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), out.getStackInSlot(i));

                MinerCoreBlockEntity.setCasingFormed(level, pos, false);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
