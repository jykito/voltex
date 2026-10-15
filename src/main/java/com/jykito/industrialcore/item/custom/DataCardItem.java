package com.jykito.industrialcore.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Scientist nanite data card: research of one topic. A blank one ({@code theme == null}) is a template that
 * the nanite lab writes. A separate item per topic so recipes, JEI and autocrafting work without NBT.
 * The body is tinted in the topic colour on layer 1 through ItemColor.
 */
public class DataCardItem extends Item {

    private final @Nullable String theme;

    public DataCardItem(@Nullable String theme, Properties props) {
        super(props);
        this.theme = theme;
    }

    public @Nullable String theme() { return theme; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(theme == null ? "tooltip.industrial_core.data_card.blank" : "tooltip.industrial_core.data_card.written")
                .withStyle(ChatFormatting.GRAY));
    }
}
