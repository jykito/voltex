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
import com.jykito.industrialcore.item.ModDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

    /**
     * The selected source of a link. Dimension and kind are kept because the "drill beacon -> industrial miner" link
     * works across dimensions. Kind: "beacon", "miner" or "other".
     */
    public record LinkSource(BlockPos pos, ResourceLocation dim, String kind) {
        public static final Codec<LinkSource> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(LinkSource::pos),
                ResourceLocation.CODEC.fieldOf("dim").forGetter(LinkSource::dim),
                Codec.STRING.fieldOf("kind").forGetter(LinkSource::kind)).apply(i, LinkSource::new));
        public static final StreamCodec<ByteBuf, LinkSource> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, LinkSource::pos,
                ResourceLocation.STREAM_CODEC, LinkSource::dim,
                ByteBufCodecs.STRING_UTF8, LinkSource::kind,
                LinkSource::new);
    }

    public LogisticsLinkerItem(Properties props) { super(props); }

    // onItemUseFirst is called BEFORE block.use(), otherwise GUI blocks (core/node/hub) would open their window before the bind
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        ItemStack linker = ctx.getItemInHand();
        LinkSource src = linker.get(ModDataComponents.LINK_SOURCE.get());

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                if (src != null) {
                    clearSource(linker);
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

        if (src == null) {

            if (!level.isClientSide()) {
                linker.set(ModDataComponents.LINK_SOURCE.get(), new LinkSource(pos.immutable(), level.dimension().location(),
                        be instanceof DrillBeaconBlockEntity ? "beacon" : be instanceof IndustrialMinerCoreBlockEntity ? "miner" : "other"));
                player.displayClientMessage(msg("msg.industrial_core.linker.source_selected", ChatFormatting.AQUA), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide()) {
            BlockPos srcPos = src.pos();
            ResourceKey<Level> srcDim = ResourceKey.create(Registries.DIMENSION, src.dim());

            if (linkSite(level, player, linker, src.kind(), be, srcPos, srcDim)) return InteractionResult.sidedSuccess(level.isClientSide());
            if (!srcDim.equals(level.dimension())) {
                clearSource(linker);
                player.displayClientMessage(msg("msg.industrial_core.linker.source_invalid", ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            if (srcPos.equals(pos)) {
                clearSource(linker);
                player.displayClientMessage(msg("msg.industrial_core.linker.selection_reset", ChatFormatting.GRAY), true);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            if (Math.sqrt(srcPos.distSqr(pos)) > 64) {
                player.displayClientMessage(msg("msg.industrial_core.linker.too_far", ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            BlockEntity srcBe = level.getBlockEntity(srcPos);
            if (!isLinkable(srcBe)) {
                clearSource(linker);
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
                clearSource(linker);
                player.displayClientMessage(msg("msg.industrial_core.linker.connected", ChatFormatting.GREEN), true);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            if (!areCompatible(srcBe, be)) {
                player.displayClientMessage(msg("msg.industrial_core.linker.incompatible", ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            addLink(srcBe, pos);
            addLink(be, srcPos);
            clearSource(linker);
            player.displayClientMessage(msg("msg.industrial_core.linker.connected", ChatFormatting.GREEN), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void clearSource(ItemStack linker) {
        linker.remove(ModDataComponents.LINK_SOURCE.get());
    }

    /**
     * Drill beacon <-> industrial miner. In the same dimension in any order; a beacon in another dimension is selected
     * first, then you click the miner (its position cannot be checked in another dimension). Range is unlimited.
     * Returns true if the click belonged to this link and has been handled.
     */
    private static boolean linkSite(Level level, Player player, ItemStack linker, String kind, BlockEntity be, BlockPos srcPos, ResourceKey<Level> srcDim) {
        IndustrialMinerCoreBlockEntity miner;
        ResourceKey<Level> siteDim;
        BlockPos sitePos;
        if (be instanceof IndustrialMinerCoreBlockEntity m && kind.equals("beacon")) {
            miner = m; siteDim = srcDim; sitePos = srcPos;
        } else if (be instanceof DrillBeaconBlockEntity && kind.equals("miner")) {
            if (!srcDim.equals(level.dimension()) || !(level.getBlockEntity(srcPos) instanceof IndustrialMinerCoreBlockEntity m)) {
                clearSource(linker);
                player.displayClientMessage(msg("msg.industrial_core.linker.site_order", ChatFormatting.YELLOW), true);
                return true;
            }
            miner = m; siteDim = level.dimension(); sitePos = be.getBlockPos();
        } else return false;
        clearSource(linker);
        switch (miner.addSite(siteDim, sitePos)) {
            case ADDED -> player.displayClientMessage(Component.translatable("msg.industrial_core.linker.site_added",
                    sitePos.getX(), sitePos.getY(), sitePos.getZ()).withStyle(ChatFormatting.GREEN), true);
            case EXISTS -> player.displayClientMessage(msg("msg.industrial_core.linker.site_exists", ChatFormatting.YELLOW), true);
            case FULL -> player.displayClientMessage(msg("msg.industrial_core.linker.site_full", ChatFormatting.RED), true);
        }
        return true;
    }

    public static boolean hasSource(ItemStack stack) {
        return stack.has(ModDataComponents.LINK_SOURCE.get());
    }

    public static @Nullable BlockPos getSource(ItemStack stack) {
        LinkSource src = stack.get(ModDataComponents.LINK_SOURCE.get());
        return src != null ? src.pos() : null;
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.industrial_core.linker.hint1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.industrial_core.linker.hint2").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.industrial_core.linker.hint3").withStyle(ChatFormatting.DARK_GRAY));
        LinkSource src = stack.get(ModDataComponents.LINK_SOURCE.get());
        if (src != null) {
            BlockPos p = src.pos();
            tip.add(Component.translatable("tooltip.industrial_core.linker.source", p.toShortString()).withStyle(ChatFormatting.AQUA));
        }
    }
}
