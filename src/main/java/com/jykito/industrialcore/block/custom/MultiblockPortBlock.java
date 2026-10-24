package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.EnergyPortBlockEntity;
import com.jykito.industrialcore.block.entity.HeatPortBlockEntity;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Late multiblock port: all exchange between the structure and the world goes through it. It does nothing by itself
 * and does not tick; it is storage the core takes from and puts into.
 * <p>
 * Next to advanced casing or glass the port switches to the dark texture by itself ({@link #DARK}),
 * on regular casing it stays light. This only affects the look: the core finds ports by block, not by state.
 */
public class MultiblockPortBlock extends BaseEntityBlock {

    public enum Kind { INPUT, OUTPUT, ENERGY, HEAT }

    public static final BooleanProperty DARK = BooleanProperty.create("dark");
    /**
     * Hidden inside a machine drawn by one model (a concealed pattern, API): the port is not drawn but stays solid,
     * clickable and working. The machine core sets it when the structure forms and clears it when it breaks apart.
     */
    public static final BooleanProperty CONCEALED = BooleanProperty.create("concealed");

    private final Kind kind;

    public MultiblockPortBlock(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(DARK, false).setValue(CONCEALED, false));
    }

    public Kind kind() { return kind; }

    @Override public RenderShape getRenderShape(BlockState s) { return s.getValue(CONCEALED) ? RenderShape.INVISIBLE : RenderShape.MODEL; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(DARK, CONCEALED); }

    /** Dark look if at least one neighbour is advanced casing or advanced glass. */
    private static boolean nearAdvanced(BlockGetter level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.is(ModBlocks.MULTIBLOCK_CASING_ADVANCED.get()) || n.is(ModBlocks.STRUCTURAL_GLASS_ADVANCED.get())) return true;
        }
        return false;
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(DARK, nearAdvanced(ctx.getLevel(), ctx.getClickedPos()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(DARK, nearAdvanced(level, pos));
    }

    /** The wand places the port via setBlock without a placement context, so the look is computed here. */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (level.isClientSide) return;
        boolean dark = nearAdvanced(level, pos);
        if (state.getValue(DARK) != dark) level.setBlock(pos, state.setValue(DARK, dark), Block.UPDATE_CLIENTS);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case ENERGY -> new EnergyPortBlockEntity(pos, state);
            case HEAT -> new HeatPortBlockEntity(pos, state);
            default -> new ItemFluidPortBlockEntity(pos, state);
        };
    }

    /** Input and output tick for auto-exchange with neighbours, the heat port ticks to cool down; the connector needs no tick. */
    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || kind == Kind.ENERGY) return null;
        if (kind == Kind.HEAT) return createTickerHelper(type, ModBlockEntities.HEAT_PORT_BE.get(), HeatPortBlockEntity::serverTick);
        return createTickerHelper(type, ModBlockEntities.ITEM_FLUID_PORT_BE.get(), ItemFluidPortBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (kind == Kind.HEAT) return InteractionResult.PASS;   // no window: heat is shown in the core
        // bucket and other containers: fill the input, drain the output, outer side
        if (kind != Kind.ENERGY && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
            return InteractionResult.sidedSuccess(level.isClientSide());
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider be)
            NetworkHooks.openScreen((ServerPlayer) player, be, pos);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ItemFluidPortBlockEntity port) port.dropContents(level, pos);
            else if (be instanceof EnergyPortBlockEntity port)
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), port.getUpgrade().getStackInSlot(0));
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
