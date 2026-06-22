package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.block.custom.wire.WireTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WireTooltipBlockItem extends BlockItem {
    private final WireTier tier;

    public WireTooltipBlockItem(Block block, Properties properties, WireTier tier) {
        super(block, properties);
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        int rate = tier.getMaxTransferRate();
        String text = (rate == Integer.MAX_VALUE) ? "∞ FE/t" : String.format("%,d FE/t", rate);
        tooltip.add(Component.literal(text).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, isAdvanced);
    }
}
