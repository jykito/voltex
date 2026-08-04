package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity;
import com.jykito.industrialcore.block.entity.MagnetPortBlockEntity;
import com.jykito.industrialcore.item.custom.MagnetCoilItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Скелет-блок мультиблока (структурный / держатель). При FORMED становится НЕВИДИМЫМ,
 * чтобы корпус-OBJ занял его место без z-fighting. Коллизия/выделение/дроп сохраняются.
 * Держатель катушки при FORMED обрабатывает вставку/снятие катушки (делегирует ядру).
 */
public class MultiblockShellBlock extends BaseEntityBlock {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public MultiblockShellBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
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
        return new MagnetPortBlockEntity(pos, state); // порт: коннект логистики к платформе → ядро (non-tick)
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // катушку обрабатывает только держатель при собранном мультиблоке; структурные — мимо
        if (!state.getValue(FORMED) || !state.is(ModBlocks.COIL_HOLDER.get())) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos corePos = findCore(level, pos);
        if (corePos == null || !(level.getBlockEntity(corePos) instanceof ElectromagnetCoreBlockEntity core))
            return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {                      // Shift+ПКМ → снять катушку в инвентарь
            ItemStack removed = core.removeCoil();
            if (removed.isEmpty()) return InteractionResult.CONSUME;
            if (!player.getInventory().add(removed)) player.drop(removed, false);
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);        // ПКМ катушкой → поставить
        if (held.getItem() instanceof MagnetCoilItem && core.getCoilTier() == 0 && core.installCoil(held)) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    /** Ядро от держателя: держатели на оси катушки — ядро + rot(0,3,facing) + (0, dy, 0), dy=1(низ)/5(верх). */
    private static BlockPos findCore(Level level, BlockPos holder) {
        for (Direction f : Direction.Plane.HORIZONTAL) {
            int[] r = ElectromagnetCoreBlockEntity.rot(0, 3, f);
            for (int dy : new int[]{1, 5}) {
                BlockPos c = holder.offset(-r[0], -dy, -r[1]);
                BlockState cs = level.getBlockState(c);
                if (cs.getBlock() instanceof com.jykito.industrialcore.block.custom.ElectromagnetCoreBlock
                        && cs.getValue(com.jykito.industrialcore.block.custom.ElectromagnetCoreBlock.FORMED)
                        && cs.getValue(com.jykito.industrialcore.block.custom.ElectromagnetCoreBlock.FACING) == f) {
                    return c;
                }
            }
        }
        return null;
    }
}
