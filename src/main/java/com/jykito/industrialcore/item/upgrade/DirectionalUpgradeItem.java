package com.jykito.industrialcore.item.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DirectionalUpgradeItem extends MachineUpgradeItem {

    public enum Mode { ITEM_PUSH, ITEM_PULL, FLUID_PUSH, FLUID_PULL }

    private final Mode mode;

    public DirectionalUpgradeItem(Mode mode, Properties props) {
        super(props);
        this.mode = mode;
    }

    public Mode getMode() { return mode; }

    @Nullable
    public static Direction getStoredDirection(ItemStack stack) {
        if (!stack.hasTag()) return null;
        String name = stack.getTag().getString("direction");
        if (name.isEmpty()) return null;
        return Direction.byName(name);
    }

    public static float getDirectionPredicate(ItemStack stack) {
        Direction d = getStoredDirection(stack);
        if (d == null) return 0f;
        return switch (d) {
            case DOWN  -> 0.1f;
            case UP    -> 0.2f;
            case NORTH -> 0.3f;
            case SOUTH -> 0.4f;
            case WEST  -> 0.5f;
            case EAST  -> 0.6f;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Direction dir = getStoredDirection(stack);
        if (dir == null) {
            tooltip.add(Component.translatable("tooltip.industrial_core.direction_not_set").withStyle(ChatFormatting.GRAY));
        } else {
            Component dirName = Component.translatable("tooltip.industrial_core.dir_" + dir.getName());
            tooltip.add(Component.translatable("tooltip.industrial_core.direction_set", dirName).withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Direction face = ctx.getClickedFace();

        ctx.getItemInHand().getOrCreateTag().putString("direction", face.getName());

        if (!ctx.getLevel().isClientSide()) {
            Component dirName = Component.translatable("tooltip.industrial_core.dir_" + face.getName());
            ctx.getPlayer().displayClientMessage(
                Component.translatable("tooltip.industrial_core.direction_set", dirName), true);
        }

        return InteractionResult.SUCCESS;
    }
}
