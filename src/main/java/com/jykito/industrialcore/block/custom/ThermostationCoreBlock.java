package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.block.entity.ThermostationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/** Thermal station core: bottom of the tower, holds the buffer and logic. Connected texture upwards (UP); on/off is ACTIVE (animation / halt). */
public class ThermostationCoreBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty UP     = BooleanProperty.create("up");     // a tower block above (expander)
    public static final BooleanProperty DOWN   = BooleanProperty.create("down");   // shared property for the expander (NOT registered on the core)
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active"); // working (animation) / halted idle (static)

    public ThermostationCoreBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(UP, false).setValue(ACTIVE, false));
    }

    /** The block belongs to a thermal station tower (core or expander). */
    public static boolean isTower(BlockState s) {
        return s.is(ModBlocks.THERMOSTATION_CORE.get()) || s.is(ModBlocks.THERMOSTATION_EXPANDER.get());
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level l = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(UP, isTower(l.getBlockState(pos.above())));   // connects vertically only (columns)
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState nbr, LevelAccessor l, BlockPos pos, BlockPos nbrPos) {
        if (dir == Direction.UP) return state.setValue(UP, isTower(nbr));
        return state;
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ThermostationBlockEntity be)
            NetworkHooks.openScreen((ServerPlayer) player, be, pos);          // only the core opens the GUI
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new ThermostationBlockEntity(pos, s); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        if (l.isClientSide) return null;
        return createTickerHelper(t, ModBlockEntities.THERMOSTATION_BE.get(), ThermostationBlockEntity::serverTick);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, UP, ACTIVE); }
}
