package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.EnergyPortMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class EnergyPortScreen extends MachineScreen<EnergyPortMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/multiblock_energy_port_gui.png");

    // заливка внутри стандартной полосы энергии (рамка 154×8 с молнией слева)
    private static final int BAR_X = 56, BAR_Y = 60, BAR_W = 152, BAR_H = 6;
    private static final int INFO_X = 48;

    private static final int C_LABEL = 0xFF9AA0A6;

    public EnergyPortScreen(EnergyPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);
        bars.draw(g, FillBar.ENERGY, x + BAR_X, y + BAR_Y, menu.getScaledEnergy(BAR_W), BAR_H);
        // заряд и ёмкость — в подсказке полосы, на экране только назначение блока
        drawScaled(g, Component.translatable("gui.industrial_core.port.energy_in_only").getString(), x + INFO_X, y + 98, C_LABEL, 0.8f);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        Bars.energy(g, font, mx, my, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H, menu.getEnergy(), menu.getMaxEnergy());
        int ux = leftPos + EnergyPortMenu.UPGRADE_X, uy = topPos + EnergyPortMenu.UPGRADE_Y;
        if (menu.getSlot(0).getItem().isEmpty() && mx >= ux && mx < ux + 16 && my >= uy && my < uy + 16)
            g.renderTooltip(font, Component.translatable("gui.industrial_core.port.upgrade_slot"), mx, my);
    }

    private void drawScaled(GuiGraphics g, String txt, float x, float y, int col, float s) {
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(s, s, 1f);
        g.drawString(font, txt, 0, 0, col, false);
        g.pose().popPose();
    }

}
