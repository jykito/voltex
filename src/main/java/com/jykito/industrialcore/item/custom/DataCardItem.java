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
 * Карта данных учёного-нанита: исследование одной темы. Пустая ({@code theme == null}) — заготовка, которую
 * нанолаборатория записывает. Отдельный предмет на тему, чтобы рецепты, JEI и автокрафт обходились без NBT.
 * Корпус красится в цвет темы слоем 1 через ItemColor.
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
