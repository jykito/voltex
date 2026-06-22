package com.jykito.industrialcore.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class JeiGui {
    private JeiGui() {}

    public static final ResourceLocation FURNACE =
            new ResourceLocation("minecraft", "textures/gui/container/furnace.png");
    public static final ResourceLocation ENERGY_FILL =
            new ResourceLocation("industrial_core", "textures/gui/energy_fill_6x_anim.png");
    public static final ResourceLocation HEAT_FILL =
            new ResourceLocation("industrial_core", "textures/gui/heat_fill_6x_anim.png");

    public static final int FILL_TILE_W = 96, FILL_H = 6, FILL_SHEET_W = 96, FILL_SHEET_H = 36,
                            FILL_FRAMES = 6, FILL_FRAME_TICKS = 16;

    public static IDrawable slot(IGuiHelper helper) { return helper.getSlotDrawable(); }

    public static void arrow(GuiGraphics g, int x, int y) {
        g.blit(FURNACE, x, y, 176, 14, 24, 16);
    }

    public static void tileFill(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int frame) {
        int v = frame * FILL_H;
        int drawn = 0;
        while (drawn < w) {
            int tw = Math.min(FILL_TILE_W, w - drawn);
            g.blit(tex, x + drawn, y, 0, v, tw, FILL_H, FILL_SHEET_W, FILL_SHEET_H);
            drawn += FILL_TILE_W;
        }
    }

    public static String fe(long v) {
        if (v >= 1_000_000) return (v / 1_000_000) + "M";
        if (v >= 1_000)     return (v / 1_000) + "k";
        return String.valueOf(v);
    }

    public static final int ENERGY = com.jykito.industrialcore.ModStyle.ENERGY;

    public static String secs(int ticks) {
        double s = ticks / 20.0;
        return (s == Math.rint(s) ? Integer.toString((int) s) : Double.toString(s)) + "s";
    }

    public static final int SMALL_LINE = 6;
    public static void smallText(GuiGraphics g, net.minecraft.client.gui.Font font, String text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.5f, 0.5f, 1f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }
    public static void smallText(GuiGraphics g, net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.5f, 0.5f, 1f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    public static final class Anim {
        private int tick = 0, frame = 0;
        public int frame() { return frame; }
        public void advance() {
            if (++tick >= FILL_FRAME_TICKS) { tick = 0; frame = (frame + 1) % FILL_FRAMES; }
        }
    }
}
