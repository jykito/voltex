package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.entity.IndustrialMinerCoreBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.miner.IndustrialMinerStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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

/**
 * Industrial miner core: the middle of the front on the second layer of the structure (see IndustrialMinerStructure).
 * A formed core is not drawn as a block; IndustrialMinerBER draws the model of the whole machine.
 */
public class IndustrialMinerCoreBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public IndustrialMinerCoreBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false).setValue(LIT, false));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
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
        return new IndustrialMinerCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.INDUSTRIAL_MINER_CORE_BE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    /** Formed: the window; not formed: which cell is in the way, so you do not hunt for it across the build. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof IndustrialMinerCoreBlockEntity core)) return InteractionResult.PASS;
        if (state.getValue(FORMED)) {
            NetworkHooks.openScreen((ServerPlayer) player, core, pos);
            return InteractionResult.CONSUME;
        }
        IndustrialMinerStructure.Problem p = IndustrialMinerStructure.check(level, pos, state.getValue(FACING));
        if (p == null) player.sendSystemMessage(Component.translatable("msg.industrial_core.industrial_miner.forming"));
        else player.sendSystemMessage(Component.translatable(p.expected() == 'F'
                        ? "msg.industrial_core.industrial_miner.need_frame" : "msg.industrial_core.industrial_miner.blocked",
                p.pos().getX(), p.pos().getY(), p.pos().getZ()));
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof IndustrialMinerCoreBlockEntity core) {
            var out = core.getOutput();
            for (int i = 0; i < out.getSlots(); i++) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), out.getStackInSlot(i));
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), core.getNanites().getStackInSlot(0));
            core.unform();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
