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
 * Антиграв-колонна. Вертикальный мультиблок из 3 клеток (LOWER/MIDDLE/UPPER): прицел, обводка и
 * коллизия по всей высоте пилона (высота ×2.33 запечена в OBJ, мир y≈0.26..2.30). BE, реестр
 * COLUMNS и heat-cap (снизу) только на нижней части; средняя/верхняя — тупые невидимые блоки,
 * которые ломаются вместе с базой. Сам пилон рисует BER на BE нижней части.
 */
public class AntigravColumnBlock extends BaseEntityBlock {

    public enum Part implements StringRepresentable {
        LOWER, MIDDLE, UPPER;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final int HEIGHT = 3;

    private static final VoxelShape SHAPE_LOWER = Shapes.or(
            Block.box(3, 0, 3, 13, 2, 13),      // подставка
            Block.box(5, 2, 5, 11, 16, 11));    // пилон в клетке 0
    private static final VoxelShape SHAPE_MIDDLE = Block.box(5, 0, 5, 11, 16, 11); // полный столб
    private static final VoxelShape SHAPE_UPPER  = Block.box(5, 0, 5, 11, 7, 11);  // верхушка (~y 2.0..2.4 с бобингом)

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
        // подставка рисуется блок-моделью на нижней части; пилон — BER; средняя/верхняя ничего не рисуют
        return state.getValue(PART) == Part.LOWER ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    // ── BE и тикер ТОЛЬКО на нижней части ──
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

    // ── установка: нужны 2 свободные клетки сверху; кап колонн на измерение ──
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (pos.getY() + HEIGHT - 1 >= level.getMaxBuildHeight()) return null;
        if (!level.getBlockState(pos.above()).canBeReplaced(ctx)
                || !level.getBlockState(pos.above(2)).canBeReplaced(ctx)) return null;

        int limit = Config.antigravColumnLimit;
        if (limit > 0 && !level.isClientSide()) { // FPS-защита от кластеров
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

    // ── ломка вместе: снос любой части сносит остальные без дропа; предмет даёт только сломанная часть ──
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
