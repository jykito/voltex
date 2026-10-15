package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Animated fill of the energy, heat and progress bars.
 * <p>
 * Texture 96x36: six frames of 6 px stacked. A bar longer than 96 px is built
 * by repeating the frame. The frame changes once every {@code frameTicks} screen render calls.
 */
public final class FillBar {

    public static final ResourceLocation ENERGY =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/energy_fill_6x_anim.png");
    public static final ResourceLocation HEAT =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/heat_fill_6x_anim.png");
    /** Chevrons: diagonal stripes move right by 1 px per frame. */
    public static final ResourceLocation PROGRESS =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/progress_fill_6x_anim.png");

    public static final int DEFAULT_FRAME_TICKS = 32;
    private static final int TILE_W = 96, FRAME_H = 6, TEX_H = 36, FRAMES = 6;

    private final int frameTicks;
    private int tick, frame;

    public FillBar(int frameTicks) { this.frameTicks = frameTicks; }

    /** Call once per render. Returns true when the frame changed. */
    public boolean advance() {
        if (++tick < frameTicks) return false;
        tick = 0;
        frame = (frame + 1) % FRAMES;
        return true;
    }

    /** Fill from left to right by {@code filled} px, bar height 6 px. */
    public void draw(GuiGraphics g, ResourceLocation tex, int x, int y, int filled) {
        draw(g, tex, x, y, filled, FRAME_H);
    }

    public void draw(GuiGraphics g, ResourceLocation tex, int x, int y, int filled, int h) {
        if (filled <= 0) return;
        int v = frame * FRAME_H;
        for (int dx = x; dx < x + filled; dx += TILE_W)
            g.blit(tex, dx, y, 0, v, Math.min(TILE_W, x + filled - dx), h, TILE_W, TEX_H);
    }
}
