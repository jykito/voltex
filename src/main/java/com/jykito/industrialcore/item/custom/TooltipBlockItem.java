package com.jykito.industrialcore.item.custom;
import net.minecraft.world.item.Item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TooltipBlockItem extends BlockItem {
    private final String tooltipKey;

    public TooltipBlockItem(Block block, Properties properties, String tooltipKey) {
        super(block, properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        @Nullable Level level = context.level();

        tooltipComponents.add(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.AQUA));

        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
    }
}
