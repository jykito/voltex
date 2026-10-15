package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Electromagnet core: controller of the accelerator multiblock.
 * When the 7x7x8 structure forms (FORMED) the block model hides and the BER draws the body (OBJ) over the skeleton.
 */
public class ElectromagnetCoreBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty   FORMED = BooleanProperty.create("formed");

    public ElectromagnetCoreBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH).setValue(FORMED, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // formed -> hide the block model, the BER draws the body over the skeleton; otherwise the core block is visible
        return state.getValue(FORMED) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectromagnetCoreBlockEntity(pos, state);
    }

    // RMB on a formed core -> open the accelerator GUI (skeleton blocks do not open the menu)
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(FORMED)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ElectromagnetCoreBlockEntity mag)
                net.minecraftforge.network.NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, mag, buf -> {
                    buf.writeBlockPos(pos);   // core (data/BE)
                    buf.writeBlockPos(pos);   // anchor = the core itself
                });
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    // ── Redstone output: a comparator reads the resonance error (7 = on target, >7 overshoot, <7 undershoot) ──
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return state.getValue(FORMED);
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ElectromagnetCoreBlockEntity be ? be.getComparatorSignal() : 0;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            // client tick: sound and spin-up only (hum, column passes, powerup)
            return createTickerHelper(type, ModBlockEntities.ELECTROMAGNET_CORE_BE.get(),
                    (lvl, pos, st, be) -> ((ElectromagnetCoreBlockEntity) be).clientTick(lvl, pos, st));
        }
        return createTickerHelper(type, ModBlockEntities.ELECTROMAGNET_CORE_BE.get(),
                (lvl, pos, st, be) -> ((ElectromagnetCoreBlockEntity) be).tick(lvl, pos, st));
    }

    // the core of a formed multiblock was broken -> make the skeleton visible again
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && state.getValue(FORMED)) {
            ElectromagnetCoreBlockEntity.setShellFormed(level, pos, state.getValue(FACING), false);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
