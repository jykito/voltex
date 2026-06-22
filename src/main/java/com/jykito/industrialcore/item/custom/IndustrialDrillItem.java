package com.jykito.industrialcore.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class IndustrialDrillItem extends EnergyDrillItem {

    public static final int CAPACITY    = 50_000;
    public static final int COST_NORMAL = 200;
    public static final int COST_3x3    = 300;

    public IndustrialDrillItem(Properties properties) {
        super(Tiers.NETHERITE, CAPACITY, 18.0f, properties);
    }

    public static int getMode(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("drill_mode") : 0;
    }

    public static void cycleMode(ItemStack stack) {
        stack.getOrCreateTag().putInt("drill_mode", (getMode(stack) + 1) % 2);
    }

    @Override
    protected int getMinCost(ItemStack stack) {
        return getMode(stack) == 1 ? COST_3x3 : COST_NORMAL;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                             BlockPos pos, LivingEntity entity) {
        if (level.isClientSide()) return true;
        if (state.getDestroySpeed(level, pos) == 0.0f) return true;

        if (getMode(stack) == 1 && entity instanceof Player player) {

            if (getStoredEnergy(stack) < COST_3x3) return true;
            drainEnergy(stack, COST_3x3);
            for (BlockPos bp : get3x3Positions(pos, player)) {
                if (getStoredEnergy(stack) < COST_3x3) break;
                BlockState bs = level.getBlockState(bp);
                if (bs.isAir() || bs.getDestroySpeed(level, bp) < 0f) continue;

                if (!bs.is(BlockTags.MINEABLE_WITH_PICKAXE) && !bs.is(BlockTags.MINEABLE_WITH_SHOVEL)) continue;
                drainEnergy(stack, COST_3x3);
                level.destroyBlock(bp, true, entity);
            }
        } else {
            drainEnergy(stack, COST_NORMAL);
        }
        return true;
    }

    private static List<BlockPos> get3x3Positions(BlockPos center, Player player) {
        List<BlockPos> out = new ArrayList<>(8);

        double relY = (center.getY() + 0.5) - player.getY();

        if (relY < -0.2 || relY > 1.8) {

            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    out.add(center.offset(dx, 0, dz));
                }
        } else {

            Direction facing = Direction.fromYRot(player.getYRot());
            Direction right   = facing.getClockWise();
            for (int dy = -1; dy <= 1; dy++)
                for (int dr = -1; dr <= 1; dr++) {
                    if (dy == 0 && dr == 0) continue;
                    out.add(center.relative(right, dr).relative(Direction.UP, dy));
                }
        }
        return out;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String modeKey = getMode(stack) == 1
                ? "tooltip.industrial_core.drill_mode_3x3"
                : "tooltip.industrial_core.drill_mode_normal";
        tooltip.add(Component.translatable(modeKey).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.industrial_core.drill_mode_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
