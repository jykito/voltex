package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.block.entity.FluidConnectorBlockEntity;
import com.jykito.industrialcore.block.entity.ItemConnectorBlockEntity;
import com.jykito.industrialcore.block.entity.LogisticsNodeBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LogisticsLinkerItem extends Item {

    private static final String KEY_SOURCE = "source";

    public LogisticsLinkerItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        ItemStack linker = ctx.getItemInHand();
        CompoundTag tag = linker.getOrCreateTag();

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                if (tag.contains(KEY_SOURCE)) {
                    tag.remove(KEY_SOURCE);
                    player.displayClientMessage(msg("msg.industrial_core.linker.cancelled", ChatFormatting.YELLOW), true);
                } else {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (isLogistics(be)) {
                        clearLinks(level, be);
                        player.displayClientMessage(msg("msg.industrial_core.linker.links_cleared", ChatFormatting.YELLOW), true);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!isLogistics(be)) return InteractionResult.PASS;

        if (!tag.contains(KEY_SOURCE)) {

            if (!level.isClientSide()) {
                tag.putLong(KEY_SOURCE, pos.asLong());
                player.displayClientMessage(msg("msg.industrial_core.linker.source_selected", ChatFormatting.AQUA), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide()) {
            BlockPos srcPos = BlockPos.of(tag.getLong(KEY_SOURCE));

            if (srcPos.equals(pos)) {
                tag.remove(KEY_SOURCE);
                player.displayClientMessage(msg("msg.industrial_core.linker.selection_reset", ChatFormatting.GRAY), true);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            if (Math.sqrt(srcPos.distSqr(pos)) > 64) {
                player.displayClientMessage(msg("msg.industrial_core.linker.too_far", ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            BlockEntity srcBe = level.getBlockEntity(srcPos);
            if (!isLogistics(srcBe)) {
                tag.remove(KEY_SOURCE);
                player.displayClientMessage(msg("msg.industrial_core.linker.source_invalid", ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            if (!areCompatible(srcBe, be)) {
                player.displayClientMessage(msg("msg.industrial_core.linker.incompatible", ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            addLink(srcBe, pos);
            addLink(be, srcPos);
            tag.remove(KEY_SOURCE);
            player.displayClientMessage(msg("msg.industrial_core.linker.connected", ChatFormatting.GREEN), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static boolean hasSource(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t != null && t.contains(KEY_SOURCE);
    }

    public static @Nullable BlockPos getSource(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return (t != null && t.contains(KEY_SOURCE)) ? BlockPos.of(t.getLong(KEY_SOURCE)) : null;
    }

    private static boolean isLogistics(BlockEntity be) {
        return be instanceof ItemConnectorBlockEntity
                || be instanceof FluidConnectorBlockEntity
                || be instanceof LogisticsNodeBlockEntity;
    }

    private static boolean areCompatible(BlockEntity a, BlockEntity b) {
        if (a instanceof LogisticsNodeBlockEntity || b instanceof LogisticsNodeBlockEntity) return true;
        return (a instanceof ItemConnectorBlockEntity && b instanceof ItemConnectorBlockEntity)
                || (a instanceof FluidConnectorBlockEntity && b instanceof FluidConnectorBlockEntity);
    }

    private static void addLink(BlockEntity be, BlockPos target) {
        if (be instanceof ItemConnectorBlockEntity c) c.addLink(target);
        else if (be instanceof FluidConnectorBlockEntity c) c.addLink(target);
        else if (be instanceof LogisticsNodeBlockEntity n) n.addLink(target);
    }

    private static void clearLinks(Level level, BlockEntity be) {
        if (be instanceof ItemConnectorBlockEntity c) c.clearAllLinks(level);
        else if (be instanceof FluidConnectorBlockEntity c) c.clearAllLinks(level);
        else if (be instanceof LogisticsNodeBlockEntity n) n.clearAllLinks(level);
    }

    private static Component msg(String key, ChatFormatting color) {
        return Component.translatable(key).withStyle(color);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.industrial_core.linker.hint1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.industrial_core.linker.hint2").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.industrial_core.linker.hint3").withStyle(ChatFormatting.DARK_GRAY));
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(KEY_SOURCE)) {
            BlockPos p = BlockPos.of(tag.getLong(KEY_SOURCE));
            tip.add(Component.translatable("tooltip.industrial_core.linker.source", p.toShortString()).withStyle(ChatFormatting.AQUA));
        }
    }
}
