package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.ThermogeneratorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ThermogeneratorScreen extends MachineScreen<ThermogeneratorMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/thermogenerator_gui.png");

    private static final int HEAT_BAR_X = 77, HEAT_BAR_Y = 97, HEAT_BAR_W = 110, HEAT_BAR_H = 6;

    private static final int PROG_X = 105, PROG_Y = 54, PROG_W = 43, PROG_H = 25;
    private static final int PROG_SRC_U = 106, PROG_SRC_V = 237;

    private static final int INFO_X = 112, INFO_Y = 79;

    private float visualProgress = 0f;
    private boolean progressInitialized = false;

    private static final int TEXT_BRIGHT = 0xFFE6EDF3;
    private static final int TEXT_GRAY   = 0xFFADB5BD;

    public ThermogeneratorScreen(ThermogeneratorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean isWorking = menu.getGenerationRate() > 0;
        if (!progressInitialized) {
            visualProgress = isWorking ? 1f : 0f;
            progressInitialized = true;
        } else if (isWorking) {
            visualProgress = Math.min(1f, visualProgress + 0.005f);
        } else {
            visualProgress = Math.max(0f, visualProgress - 0.003f);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        Bars.heat(g, font, mouseX, mouseY, leftPos + HEAT_BAR_X, topPos + HEAT_BAR_Y, HEAT_BAR_W, HEAT_BAR_H,
                menu.getHeat(), menu.getMaxHeat());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 315);

        int heatFilled = menu.getScaledHeat(HEAT_BAR_W);
        bars.draw(g, FillBar.HEAT, x + HEAT_BAR_X, y + HEAT_BAR_Y, heatFilled, HEAT_BAR_H);

        int filled = (int)(visualProgress * PROG_H);
        if (filled > 0) {
            int srcV  = PROG_SRC_V + (PROG_H - filled);
            int dstY  = PROG_Y + (PROG_H - filled);
            g.blit(TEXTURE, x + PROG_X, y + dstY, PROG_SRC_U, srcV, PROG_W, filled, 256, 315);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        int gen = menu.getGenerationRate();
        String text = gen + " hU/t";

        float screenW = font.width(text) * 0.5f;
        float screenH = 9 * 0.5f;
        float tx = INFO_X + (30 - screenW) / 2f;
        float ty = INFO_Y + (11 - screenH) / 2f;

        g.pose().pushPose();
        g.pose().translate(tx, ty, 0);
        g.pose().scale(0.5f, 0.5f, 1f);
        g.drawString(font, text, 0, 0, TEXT_GRAY, false);
        g.pose().popPose();
    }
}
