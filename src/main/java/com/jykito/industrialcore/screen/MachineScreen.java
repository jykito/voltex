package com.jykito.industrialcore.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Shared frame of machine screens: a 256x224 window from a 256x315 texture, vanilla labels hidden
 * (they are drawn in the art), bar animation and tooltip draw order.
 * <p>
 * The screen draws its own tooltips in {@link #renderTooltips}, which is already over the window and slots.
 */
public abstract class MachineScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    /** GUI texture file size: the visible window on top, sprites below it. */
    protected static final int TEX_W = 256, TEX_H = 315;

    protected final FillBar bars;

    protected MachineScreen(T menu, Inventory inv, Component title) {
        this(menu, inv, title, FillBar.DEFAULT_FRAME_TICKS);
    }

    protected MachineScreen(T menu, Inventory inv, Component title, int barFrameTicks) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 224;
        this.bars = new FillBar(barFrameTicks);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        if (bars.advance()) onBarFrame();
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        renderTooltips(g, mx, my);
    }

    /** Screen tooltips over the whole window. */
    protected void renderTooltips(GuiGraphics g, int mx, int my) {}

    /** The bar animation frame changed; for your own animations to keep in step with them. */
    protected void onBarFrame() {}
}
