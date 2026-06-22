package com.jykito.industrialcore.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CatalystItem extends Item {

    private final String tooltipKey;

    public CatalystItem(String tooltipKey) {
        super(new Item.Properties().stacksTo(16));
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(tooltipKey).withStyle(
                net.minecraft.ChatFormatting.GRAY));
    }
}
