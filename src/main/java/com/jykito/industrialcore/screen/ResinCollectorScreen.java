package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.menu.ResinCollectorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ResinCollectorScreen extends AbstractContainerScreen<ResinCollectorMenu> {

    public ResinCollectorScreen(ResinCollectorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF3C3C3C);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF4A4A4A);

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++) {
                int sx = x + 62 + col * 18, sy = y + 18 + row * 18;
                g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
                g.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
            }

        g.fill(x + 7, y + 83, x + 169, y + 161, 0xFF373737);
        g.fill(x + 8, y + 84, x + 168, y + 160, 0xFF8B8B8B);
    }
}
