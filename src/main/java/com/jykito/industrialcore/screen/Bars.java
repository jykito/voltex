package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.ModStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Locale;

/**
 * Bar tooltips in machine GUIs.
 * <p>
 * Before, every screen checked the cursor hit and built the text itself, so some
 * machines had a tooltip and others did not, and the number format differed. Here there is one entry for all screens:
 * pass the bar coordinates and values, get a uniform tooltip.
 * <p>
 * Call from {@code render(...)} AFTER {@code super.render(...)}, otherwise the window contents
 * cover the tooltip.
 */
public final class Bars {

    private Bars() {}

    /** Energy: "Energy: 12 345 / 64 000 FE", in the mod's energy colour. */
    public static void energy(GuiGraphics g, Font font, int mx, int my,
                              int x, int y, int w, int h, int stored, int max) {
        bar(g, font, mx, my, x, y, w, h, stored, max,
                "gui.industrial_core.bar.energy", Style.EMPTY.applyTo(ModStyle.ENERGY_STYLE));
    }

    /** Heat: the heat network has its own unit, no FE. */
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

    /** Thousands separated by spaces: six-digit buffers are unreadable otherwise. */
    private static String num(int v) {
        return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
    }
}
