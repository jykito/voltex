package com.jykito.industrialcore.item.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DirectionalUpgradeItem extends MachineUpgradeItem {

    public enum Mode { ITEM_PUSH, ITEM_PULL, FLUID_PUSH, FLUID_PULL }

    private final Mode mode;

    // Серверный детект даблклика по игроку. Метку времени в NBT НЕ пишем — иначе предметы перестанут стакаться.
    private static final Map<UUID, Long> LAST_CLICK = new HashMap<>();

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
        UpgradeTargets.append(tooltip, UpgradeTargets.forMode(mode));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        long now = level.getGameTime();
        UUID id = player != null ? player.getUUID() : null;
        Long last = id != null ? LAST_CLICK.get(id) : null;
        boolean doubleClick = last != null && (now - last) <= 7; // два ПКМ подряд (~0.35с) → сброс конфига

        if (doubleClick) {
            if (stack.hasTag()) stack.getTag().remove("direction");
            if (id != null) LAST_CLICK.remove(id);
            if (player != null)
                player.displayClientMessage(Component.translatable("tooltip.industrial_core.direction_not_set"), true);
        } else {
            Direction face = ctx.getClickedFace();
            stack.getOrCreateTag().putString("direction", face.getName());
            if (id != null) LAST_CLICK.put(id, now);
            if (player != null) {
                Component dirName = Component.translatable("tooltip.industrial_core.dir_" + face.getName());
                player.displayClientMessage(Component.translatable("tooltip.industrial_core.direction_set", dirName), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
