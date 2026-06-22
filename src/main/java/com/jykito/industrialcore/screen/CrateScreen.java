package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.menu.CrateMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CrateScreen extends AbstractContainerScreen<CrateMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");

    private final int rows;

    public CrateScreen(CrateMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.rows = menu.getRows();
        this.imageWidth  = 176;
        this.imageHeight = 114 + rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        g.blit(TEXTURE, x, y, 0, 0, this.imageWidth, 17);
        for (int r = 0; r < rows; r++)
            g.blit(TEXTURE, x, y + 17 + r * 18, 0, 17, this.imageWidth, 18);
        g.blit(TEXTURE, x, y + 17 + rows * 18, 0, 126, this.imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
