package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.Config;
import com.jykito.industrialcore.block.entity.AntigravColumnBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Antigrav column. A vertical multiblock of 3 cells (LOWER/MIDDLE/UPPER): crosshair, outline and
 * collision over the whole pylon height (height x2.33 baked into the OBJ, world y≈0.26..2.30). BE, the
 * COLUMNS registry and the heat cap (from below) live only on the lower part; the middle and upper parts are dumb invisible blocks
 * that break together with the base. The pylon itself is drawn by the BER on the lower part's BE.
 */
public class AntigravColumnBlock extends BaseEntityBlock {

    public enum Part implements StringRepresentable {
        LOWER, MIDDLE, UPPER;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final int HEIGHT = 3;

    private static final VoxelShape SHAPE_LOWER = Shapes.or(
            Block.box(3, 0, 3, 13, 2, 13),      // base
            Block.box(5, 2, 5, 11, 16, 11));    // pylon in cell 0
    private static final VoxelShape SHAPE_MIDDLE = Block.box(5, 0, 5, 11, 16, 11); // full column
    private static final VoxelShape SHAPE_UPPER  = Block.box(5, 0, 5, 11, 7, 11);  // top (~y 2.0..2.4 with bobbing)

    public AntigravColumnBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(PART)) {
            case LOWER  -> SHAPE_LOWER;
            case MIDDLE -> SHAPE_MIDDLE;
            case UPPER  -> SHAPE_UPPER;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // the base is drawn by the block model on the lower part; the pylon by the BER; the middle and upper parts draw nothing
        return state.getValue(PART) == Part.LOWER ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    // ── BE and ticker ONLY on the lower part ──
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.LOWER ? new AntigravColumnBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(PART) != Part.LOWER) return null;
        return createTickerHelper(type, ModBlockEntities.ANTIGRAV_COLUMN_BE.get(),
                level.isClientSide()
                        ? (lvl, pos, st, be) -> AntigravColumnBlockEntity.clientTick(lvl, pos, st, be)
                        : (lvl, pos, st, be) -> AntigravColumnBlockEntity.serverTick(lvl, pos, st, be));
    }

    // ── placement: needs 2 free cells above; column cap per dimension ──
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (pos.getY() + HEIGHT - 1 >= level.getMaxBuildHeight()) return null;
        if (!level.getBlockState(pos.above()).canBeReplaced(ctx)
                || !level.getBlockState(pos.above(2)).canBeReplaced(ctx)) return null;

        int limit = Config.antigravColumnLimit;
        if (limit > 0 && !level.isClientSide()) { // FPS guard against clusters
            long count = AntigravColumnBlockEntity.COLUMNS.stream().filter(c -> c.getLevel() == level).count();
            if (count >= limit) {
                if (ctx.getPlayer() != null)
                    ctx.getPlayer().displayClientMessage(
                            Component.translatable("msg.industrial_core.antigrav_limit", limit), true);
                return null;
            }
        }
        return defaultBlockState().setValue(PART, Part.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            level.setBlock(pos.above(),  defaultBlockState().setValue(PART, Part.MIDDLE), Block.UPDATE_ALL);
            level.setBlock(pos.above(2), defaultBlockState().setValue(PART, Part.UPPER),  Block.UPDATE_ALL);
        }
    }

    // ── break together: removing any part removes the others without drops; only the broken part drops the item ──
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved && !state.is(newState.getBlock())) {
            BlockPos base = pos.below(state.getValue(PART).ordinal());
            for (int i = 0; i < HEIGHT; i++) {
                BlockPos pp = base.above(i);
                if (!pp.equals(pos) && level.getBlockState(pp).is(this)) level.removeBlock(pp, false);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
