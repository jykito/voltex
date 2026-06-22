package com.jykito.industrialcore.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TooltipItem extends Item {
    private final String tooltipKey;

    public TooltipItem(Properties properties, String tooltipKey) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {

        tooltipComponents.add(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.LIGHT_PURPLE));

        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}
