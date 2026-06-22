package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.BlastFurnaceMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BlastFurnaceScreen extends AbstractContainerScreen<BlastFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/blast_furnace_gui.png");
    private static final ResourceLocation HEAT_FILL =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/heat_fill_6x_anim.png");

    private static final int HEAT_X = 88,  HEAT_Y = 97, HEAT_W = 73, HEAT_H = 6;
    private static final int FILL_W = 96,  FILL_H = 6,  ANIM_H = 36, FILL_FRAMES = 6, FILL_TICKS = 32;

    private static final int PROG_X = 110, PROG_Y = 49, PROG_W = 28, PROG_H = 20;
    private static final int PROG_SRC_U = 119, PROG_SRC_V = 239;

    private int fillTick = 0, fillFrame = 0;

    public BlastFurnaceScreen(BlastFurnaceMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
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

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 315);

        int heatFilled = menu.getScaledHeat();
        if (heatFilled > 0) {
            int vOffset = fillFrame * FILL_H;
            int drawX = x + HEAT_X;
            while (drawX < x + HEAT_X + heatFilled) {
                int tileW = Math.min(FILL_W, x + HEAT_X + heatFilled - drawX);
                g.blit(HEAT_FILL, drawX, y + HEAT_Y, 0, vOffset, tileW, HEAT_H, FILL_W, ANIM_H);
                drawX += FILL_W;
            }
        }

        int progress = menu.getScaledProgress();
        if (progress > 0) {
            g.blit(TEXTURE, x + PROG_X, y + PROG_Y, PROG_SRC_U, PROG_SRC_V, progress, PROG_H, 256, 315);
        }
    }
}
