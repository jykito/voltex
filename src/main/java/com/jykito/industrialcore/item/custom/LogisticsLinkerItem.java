package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.block.entity.DistantControllerBlockEntity;
import com.jykito.industrialcore.block.entity.DrillBeaconBlockEntity;
import com.jykito.industrialcore.block.entity.IndustrialMinerCoreBlockEntity;
import com.jykito.industrialcore.block.entity.IndustrialMinerFrameBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
    /** Dimension and kind of the source: the "drill beacon -> industrial miner" link works across dimensions. */
    private static final String KEY_SOURCE_DIM = "source_dim", KEY_SOURCE_KIND = "source_kind";

    public LogisticsLinkerItem(Properties props) { super(props); }

    // onItemUseFirst is called BEFORE block.use(), otherwise GUI blocks (core/node/hub) would open their window before the bind
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
                    clearSource(tag);
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
        if (be instanceof IndustrialMinerFrameBlockEntity frame && frame.core() != null) { be = frame.core(); pos = be.getBlockPos(); }   // frame = its core
        if (!isLinkable(be)) return InteractionResult.PASS;

        if (!tag.contains(KEY_SOURCE)) {

            if (!level.isClientSide()) {
                tag.putLong(KEY_SOURCE, pos.asLong());
                tag.putString(KEY_SOURCE_DIM, level.dimension().location().toString());
                tag.putString(KEY_SOURCE_KIND, be instanceof DrillBeaconBlockEntity ? "beacon" : be instanceof IndustrialMinerCoreBlockEntity ? "miner" : "other");
                player.displayClientMessage(msg("msg.industrial_core.linker.source_selected", ChatFormatting.AQUA), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide()) {
            BlockPos srcPos = BlockPos.of(tag.getLong(KEY_SOURCE));
            ResourceKey<Level> srcDim = tag.contains(KEY_SOURCE_DIM)
                    ? ResourceKey.create(Registries.DIMENSION, new ResourceLocation(tag.getString(KEY_SOURCE_DIM))) : level.dimension();

            if (linkSite(level, player, tag, be, srcPos, srcDim)) return InteractionResult.sidedSuccess(level.isClientSide());
            if (!srcDim.equals(level.dimension())) {
                clearSource(tag);
                player.displayClientMessage(msg("msg.industrial_core.linker.source_invalid", ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            if (srcPos.equals(pos)) {
                clearSource(tag);
                player.displayClientMessage(msg("msg.industrial_core.linker.selection_reset", ChatFormatting.GRAY), true);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            if (Math.sqrt(srcPos.distSqr(pos)) > 64) {
                player.displayClientMessage(msg("msg.industrial_core.linker.too_far", ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            BlockEntity srcBe = level.getBlockEntity(srcPos);
            if (!isLinkable(srcBe)) {
                clearSource(tag);
                player.displayClientMessage(msg("msg.industrial_core.linker.source_invalid", ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            // Controller <-> core: linking the console (the core position is stored one way, in the controller)
            DistantControllerBlockEntity ctrl =
                    srcBe instanceof DistantControllerBlockEntity cs ? cs
                  : be    instanceof DistantControllerBlockEntity ct ? ct : null;
            ElectromagnetCoreBlockEntity linkCore =
                    srcBe instanceof ElectromagnetCoreBlockEntity ks ? ks
                  : be    instanceof ElectromagnetCoreBlockEntity kt ? kt : null;
            if (ctrl != null && linkCore != null) {
                ctrl.setLinkedCore(linkCore.getBlockPos());
                clearSource(tag);
                player.displayClientMessage(msg("msg.industrial_core.linker.connected", ChatFormatting.GREEN), true);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            if (!areCompatible(srcBe, be)) {
                player.displayClientMessage(msg("msg.industrial_core.linker.incompatible", ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            addLink(srcBe, pos);
            addLink(be, srcPos);
            clearSource(tag);
            player.displayClientMessage(msg("msg.industrial_core.linker.connected", ChatFormatting.GREEN), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void clearSource(CompoundTag tag) {
        tag.remove(KEY_SOURCE); tag.remove(KEY_SOURCE_DIM); tag.remove(KEY_SOURCE_KIND);
    }

    /**
     * Drill beacon <-> industrial miner. In the same dimension in any order; a beacon in another dimension is selected
     * first, then you click the miner (its position cannot be checked in another dimension). Range is unlimited.
     * Returns true if the click belonged to this link and has been handled.
     */
    private static boolean linkSite(Level level, Player player, CompoundTag tag, BlockEntity be, BlockPos srcPos, ResourceKey<Level> srcDim) {
        String kind = tag.getString(KEY_SOURCE_KIND);
        IndustrialMinerCoreBlockEntity miner;
        ResourceKey<Level> siteDim;
        BlockPos sitePos;
        if (be instanceof IndustrialMinerCoreBlockEntity m && kind.equals("beacon")) {
            miner = m; siteDim = srcDim; sitePos = srcPos;
        } else if (be instanceof DrillBeaconBlockEntity && kind.equals("miner")) {
            if (!srcDim.equals(level.dimension()) || !(level.getBlockEntity(srcPos) instanceof IndustrialMinerCoreBlockEntity m)) {
                clearSource(tag);
                player.displayClientMessage(msg("msg.industrial_core.linker.site_order", ChatFormatting.YELLOW), true);
                return true;
            }
            miner = m; siteDim = level.dimension(); sitePos = be.getBlockPos();
        } else return false;
        clearSource(tag);
        switch (miner.addSite(siteDim, sitePos)) {
            case ADDED -> player.displayClientMessage(Component.translatable("msg.industrial_core.linker.site_added",
                    sitePos.getX(), sitePos.getY(), sitePos.getZ()).withStyle(ChatFormatting.GREEN), true);
            case EXISTS -> player.displayClientMessage(msg("msg.industrial_core.linker.site_exists", ChatFormatting.YELLOW), true);
            case FULL -> player.displayClientMessage(msg("msg.industrial_core.linker.site_full", ChatFormatting.RED), true);
        }
        return true;
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

    // The Linker connects everything: logistics (node/hub) + console <-> magnet core
    private static boolean isLinkable(BlockEntity be) {
        return isLogistics(be)
                || be instanceof DistantControllerBlockEntity
                || be instanceof ElectromagnetCoreBlockEntity
                || be instanceof DrillBeaconBlockEntity
                || be instanceof IndustrialMinerCoreBlockEntity;
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
