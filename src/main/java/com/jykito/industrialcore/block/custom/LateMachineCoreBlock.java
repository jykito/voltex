package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.entity.LateMachineCoreBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.multiblock.LateMultiblockPattern;
import com.jykito.industrialcore.multiblock.LateRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Late multiblock core: the face turns to the player on placement, LIT enables the face animation.
 * The structure shape and recipe type are set at registration, the logic is shared
 * ({@link LateMachineCoreBlockEntity}).
 */
public class LateMachineCoreBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final LateMultiblockPattern pattern;
    private final java.util.List<LateRecipe.Mode> modes;

    /** Core with a single recipe set: no modes, and no button in the GUI. */
    public LateMachineCoreBlock(Properties props, LateMultiblockPattern pattern, LateRecipe.Source recipes) {
        this(props, pattern, new LateRecipe.Mode("single", recipes));
    }

    /** Core with modes: the player switches them with a button in the GUI, each has its own recipes. */
    public LateMachineCoreBlock(Properties props, LateMultiblockPattern pattern, LateRecipe.Mode... modes) {
        super(props);
        this.pattern = pattern;
        this.modes = java.util.List.of(modes);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    public LateMultiblockPattern pattern() { return pattern; }
    public java.util.List<LateRecipe.Mode> modes() { return modes; }
    /** Recipes of a mode; an index outside the list (for example after the block changed) means the first mode. */
    public LateRecipe.Source recipes(int mode) { return modes.get(mode >= 0 && mode < modes.size() ? mode : 0).source(); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** The strain in the washer slot drops separately: the loot table only drops the block itself. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof LateMachineCoreBlockEntity be)
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.getNanites().getStackInSlot(0));
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LateMachineCoreBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.LATE_MACHINE_CORE_BE.get()) return null;
        return (l, p, s, be) -> LateMachineCoreBlockEntity.serverTick(l, p, s, (LateMachineCoreBlockEntity) be);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LateMachineCoreBlockEntity be)
            NetworkHooks.openScreen((ServerPlayer) player, be, pos);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
