package com.jykito.industrialcore.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Общий каркас экранов машин: окно 256×224 из текстуры 256×315, подписи ванили спрятаны
 * (они нарисованы в арте), анимация полосок и порядок отрисовки подсказок.
 * <p>
 * Свои подсказки экран рисует в {@link #renderTooltips}, это уже поверх окна и слотов.
 */
public abstract class MachineScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    /** Размер файла текстуры GUI: видимое окно сверху, спрайты под ним. */
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

    /** Подсказки экрана поверх всего окна. */
    protected void renderTooltips(GuiGraphics g, int mx, int my) {}

    /** Сменился кадр анимации полосок; для своих анимаций в такт с ними. */
    protected void onBarFrame() {}
}
