package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.menu.BaseMachineMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public abstract class BaseMachineScreen<T extends BaseMachineMenu> extends AbstractContainerScreen<T> {

    private static final ResourceLocation ENERGY_FILL =
            new ResourceLocation("industrial_core", "textures/gui/energy_fill_6x_anim.png");

    private static final int BAR_X = 72, BAR_Y = 97, BAR_W = 110, BAR_H = 6;
    private static final int FILL_W = 96, FILL_H = 6, ANIM_H = 36, FILL_FRAMES = 6, FILL_TICKS = 32;

    protected int PROG_X = 117, PROG_Y = 61, PROG_W = 16, PROG_H = 12;
    protected int PROG_SRC_U = 122, PROG_SRC_V = 243;

    private final ResourceLocation texture;
    private int fillTick = 0, fillFrame = 0;

    public BaseMachineScreen(T pMenu, Inventory pPlayerInventory, Component pTitle, ResourceLocation texture) {
        super(pMenu, pPlayerInventory, pTitle);
        this.texture = texture;
        this.imageWidth  = 256;
        this.imageHeight = 224;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY     = 10000;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        if (++fillTick >= FILL_TICKS) { fillTick = 0; fillFrame = (fillFrame + 1) % FILL_FRAMES; }

        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(texture, x, y, 0, 0, imageWidth, imageHeight, 256, 315);

        int filled = menu.getScaledEnergy();
        if (filled > 0) {
            int vOffset = fillFrame * FILL_H;
            int drawX = x + BAR_X;
            while (drawX < x + BAR_X + filled) {
                int tileW = Math.min(FILL_W, x + BAR_X + filled - drawX);
                g.blit(ENERGY_FILL, drawX, y + BAR_Y, 0, vOffset, tileW, BAR_H, FILL_W, ANIM_H);
                drawX += FILL_W;
            }
        }

        int progress = menu.getScaledProgress(PROG_W);
        if (progress > 0) {
            g.blit(texture, x + PROG_X, y + PROG_Y, PROG_SRC_U, PROG_SRC_V, progress, PROG_H, 256, 315);
        }
    }
}
