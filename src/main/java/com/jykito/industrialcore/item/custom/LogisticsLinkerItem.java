package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.block.entity.DistantControllerBlockEntity;
import com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity;
import com.jykito.industrialcore.block.entity.HubBlockEntity;
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

    // onItemUseFirst — вызывается ДО block.use(), иначе GUI-блоки (ядро/нода/хаб) открывали бы окно раньше бинда
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
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
        if (!isLinkable(be)) return InteractionResult.PASS;

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
            if (!isLinkable(srcBe)) {
                tag.remove(KEY_SOURCE);
                player.displayClientMessage(msg("msg.industrial_core.linker.source_invalid", ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            // Контроллер ↔ ядро: привязка пульта (одностороннее хранение позиции ядра в контроллере)
            DistantControllerBlockEntity ctrl =
                    srcBe instanceof DistantControllerBlockEntity cs ? cs
                  : be    instanceof DistantControllerBlockEntity ct ? ct : null;
            ElectromagnetCoreBlockEntity linkCore =
                    srcBe instanceof ElectromagnetCoreBlockEntity ks ? ks
                  : be    instanceof ElectromagnetCoreBlockEntity kt ? kt : null;
            if (ctrl != null && linkCore != null) {
                ctrl.setLinkedCore(linkCore.getBlockPos());
                tag.remove(KEY_SOURCE);
                player.displayClientMessage(msg("msg.industrial_core.linker.connected", ChatFormatting.GREEN), true);
                return InteractionResult.sidedSuccess(level.isClientSide());
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
        return be instanceof LogisticsNodeBlockEntity
                || be instanceof HubBlockEntity;
    }

    // Линкер соединяет всё: логистику (нода/хаб) + пульт ↔ ядро магнита
    private static boolean isLinkable(BlockEntity be) {
        return isLogistics(be)
                || be instanceof DistantControllerBlockEntity
                || be instanceof ElectromagnetCoreBlockEntity;
    }

    private static boolean isItemSide(BlockEntity be) {
        return be instanceof HubBlockEntity h && !h.fluid;
    }

    private static boolean isFluidSide(BlockEntity be) {
        return be instanceof HubBlockEntity h && h.fluid;
    }

    private static boolean areCompatible(BlockEntity a, BlockEntity b) {
        if (a instanceof LogisticsNodeBlockEntity || b instanceof LogisticsNodeBlockEntity) return true;
        return (isItemSide(a) && isItemSide(b)) || (isFluidSide(a) && isFluidSide(b));
    }

    private static void addLink(BlockEntity be, BlockPos target) {
        if (be instanceof LogisticsNodeBlockEntity n) n.addLink(target);
        else if (be instanceof HubBlockEntity h) h.addLink(target);
    }

    private static void clearLinks(Level level, BlockEntity be) {
        if (be instanceof LogisticsNodeBlockEntity n) n.clearAllLinks(level);
        else if (be instanceof HubBlockEntity h) h.clearAllLinks(level);
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
