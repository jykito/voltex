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
 * Multiblock skeleton block (structural / holder). When FORMED it becomes INVISIBLE
 * so the OBJ body takes its place without z-fighting. Collision, selection and drops stay.
 * When FORMED, the coil holder handles inserting and removing the coil (delegates to the core).
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
        return new MagnetPortBlockEntity(pos, state); // port: logistics connection to the platform -> core (non-tick)
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // only the holder of a formed multiblock handles the coil; structural blocks pass
        if (!state.getValue(FORMED) || !state.is(ModBlocks.COIL_HOLDER.get())) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos corePos = findCore(level, pos);
        if (corePos == null || !(level.getBlockEntity(corePos) instanceof ElectromagnetCoreBlockEntity core))
            return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {                      // Shift+RMB -> take the coil into the inventory
            ItemStack removed = core.removeCoil();
            if (removed.isEmpty()) return InteractionResult.CONSUME;
            if (!player.getInventory().add(removed)) player.drop(removed, false);
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);        // RMB with a coil -> insert it
        if (held.getItem() instanceof MagnetCoilItem && core.getCoilTier() == 0 && core.installCoil(held)) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    /** Core from a holder: holders sit on the coil axis, core + rot(0,3,facing) + (0, dy, 0), dy=1 (bottom) / 5 (top). */
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
