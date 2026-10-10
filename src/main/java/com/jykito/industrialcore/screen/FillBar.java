package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Анимированная заливка полосок энергии, тепла и прогресса.
 * <p>
 * Текстура 96×36: шесть кадров по 6 px друг под другом. Полоса длиннее 96 px набирается
 * повтором кадра. Кадр сменяется раз в {@code frameTicks} вызовов render экрана.
 */
public final class FillBar {

    public static final ResourceLocation ENERGY =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/energy_fill_6x_anim.png");
    public static final ResourceLocation HEAT =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/heat_fill_6x_anim.png");
    /** Шевроны: косые полосы бегут вправо на 1 px за кадр. */
    public static final ResourceLocation PROGRESS =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/progress_fill_6x_anim.png");

    public static final int DEFAULT_FRAME_TICKS = 32;
    private static final int TILE_W = 96, FRAME_H = 6, TEX_H = 36, FRAMES = 6;

    private final int frameTicks;
    private int tick, frame;

    public FillBar(int frameTicks) { this.frameTicks = frameTicks; }

    /** Вызывать раз за render. Возвращает true, когда сменился кадр. */
    public boolean advance() {
        if (++tick < frameTicks) return false;
        tick = 0;
        frame = (frame + 1) % FRAMES;
        return true;
    }

    /** Заливка слева направо на {@code filled} px, высота полосы 6 px. */
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
