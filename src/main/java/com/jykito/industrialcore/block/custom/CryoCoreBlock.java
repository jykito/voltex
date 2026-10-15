package com.jykito.industrialcore.block.custom;
import com.mojang.serialization.MapCodec;
import com.jykito.industrialcore.block.BlockUse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;

import com.jykito.industrialcore.block.entity.CryoCoreBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Cryo chamber core: the controlling block of the multiblock.
 * <p>
 * RMB opens the panel. Sneaking gives a structure report: formed, or the first place that does not match.
 * The report stayed a separate action because finding one wrong panel in a 5x5x5 shell
 * by eye is painful, and the panel cannot tell you.
 */
public class CryoCoreBlock extends HorizontalDirectionalBlock implements EntityBlock {

    /** Broken core: its contents fall out, and the casings, which forward its handlers, are unlinked before the entity goes. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CryoCoreBlockEntity core) {
            core.dropContents();
            core.unlinkCasings();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** 1.21: block codec. Vanilla does not call it for mod blocks; there is one block instance, so unit is exact. */
    @Override
    protected MapCodec<? extends CryoCoreBlock> codec() { return MapCodec.unit(this); }

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public CryoCoreBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LIT);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // facing the player, like the other machines of the mod
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return BlockUse.toItemResult(use(state, level, pos, player, hand, hit));
    }

    /** Formerly Block.use (1.20.1); called from useItemOn for both hands, as before. */
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CryoCoreBlockEntity be) {
            Component miss = be.firstMismatch(level, pos, state);
            // An unformed chamber does not open the panel: an empty window explains nothing, but
            // the first mismatch does. Sneaking gives the report on a formed one too.
            if (miss != null) {
                player.displayClientMessage(
                        Component.translatable("msg.industrial_core.cryo.missing", miss).withStyle(ChatFormatting.RED), false);
            } else if (player.isShiftKeyDown()) {
                player.displayClientMessage(
                        Component.translatable("msg.industrial_core.cryo.formed").withStyle(ChatFormatting.GREEN), false);
            } else {
                ((net.minecraft.server.level.ServerPlayer) player).openMenu(be, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CryoCoreBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // the structure check is server side; createTickerHelper is not available here:
        // it lives in BaseEntityBlock, and we extend HorizontalDirectionalBlock for FACING
        if (level.isClientSide || type != ModBlockEntities.CRYO_CORE_BE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<CryoCoreBlockEntity>) CryoCoreBlockEntity::serverTick;
    }
}
