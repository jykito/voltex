package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.ModStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Locale;

/**
 * Тултипы полосок в GUI машин.
 * <p>
 * Раньше каждый экран сам проверял попадание курсора и сам собирал текст, поэтому у одних
 * машин тултип был, у других нет, а формат числа отличался. Здесь один вход на все экраны:
 * передал координаты полоски и значения — получил единообразную подсказку.
 * <p>
 * Вызывать из {@code render(...)} ПОСЛЕ {@code super.render(...)}, иначе тултип закроется
 * содержимым окна.
 */
public final class Bars {

    private Bars() {}

    /** Энергия: «Энергия: 12 345 / 64 000 FE», цветом энергии мода. */
    public static void energy(GuiGraphics g, Font font, int mx, int my,
                              int x, int y, int w, int h, int stored, int max) {
        bar(g, font, mx, my, x, y, w, h, stored, max,
                "gui.industrial_core.bar.energy", Style.EMPTY.applyTo(ModStyle.ENERGY_STYLE));
    }

    /** Тепло: у теплосети своя единица, без FE. */
    public static void heat(GuiGraphics g, Font font, int mx, int my,
                            int x, int y, int w, int h, int stored, int max) {
        bar(g, font, mx, my, x, y, w, h, stored, max,
                "gui.industrial_core.bar.heat", Style.EMPTY.withColor(ChatFormatting.GOLD));
    }

    private static void bar(GuiGraphics g, Font font, int mx, int my,
                            int x, int y, int w, int h, int stored, int max, String key, Style style) {
        if (mx < x || mx >= x + w || my < y || my >= y + h) return;
        g.renderTooltip(font, Component.translatable(key, num(stored), num(max)).withStyle(style), mx, my);
    }

    /** Разряды через пробел: шестизначные буферы иначе не читаются. */
    private static String num(int v) {
        return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
    }
}
