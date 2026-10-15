package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.PhaseSplitterMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PhaseSplitterScreen extends MachineScreen<PhaseSplitterMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/phase_splitter_gui.png");

    private static final int BAR_X = 71, BAR_Y = 98, BAR_H = 6;

    private static final int PROG_X = 108, PROG_Y = 45, PROG_W = 32, PROG_H = 46;
    private static final int PROG_SRC_U = 107, PROG_SRC_V = 225;

    public PhaseSplitterScreen(PhaseSplitterMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        Bars.energy(g, font, mouseX, mouseY, leftPos + BAR_X, topPos + BAR_Y, 110, BAR_H,
                menu.getEnergyStored(), menu.getMaxEnergy());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        int filled = menu.getScaledEnergy();
        if (filled > 0) {
            int barX = x + BAR_X, barY = y + BAR_Y;
            bars.draw(g, FillBar.ENERGY, barX, barY, filled, BAR_H);
        }

        int progress = menu.getScaledProgress();
        if (progress > 0) {
            g.blit(TEXTURE, x + PROG_X, y + PROG_Y, PROG_SRC_U, PROG_SRC_V, progress, PROG_H, TEX_W, TEX_H);
        }
    }
}
