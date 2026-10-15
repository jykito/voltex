package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.LiquefierMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class LiquefierScreen extends MachineScreen<LiquefierMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/liquefier_gui.png");

    private static final int ENERGY_X = 72, ENERGY_Y = 98, BAR_H = 6;
    private static final int HEAT_X = 72, HEAT_Y = 108;

    private static final int TANK_X = 142, TANK_Y = 29, TANK_W = 16, TANK_H = 64;

    private static final int GLASS_U = 192, GLASS_V = 237;

    private static final int PROG_X = 105, PROG_Y = 47, PROG_W = 33, PROG_H = 28;
    private static final int PROG_SRC_U = 91, PROG_SRC_V = 231;

    public LiquefierScreen(LiquefierMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        Bars.heat(g, font, mx, my, leftPos + HEAT_X, topPos + HEAT_Y, 110, BAR_H,
                menu.getHeat(), menu.getMaxHeat());
        renderFluidTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        bars.draw(g, FillBar.ENERGY, x + ENERGY_X, y + ENERGY_Y, menu.getScaledEnergy());
        bars.draw(g, FillBar.HEAT,   x + HEAT_X,   y + HEAT_Y,   menu.getScaledHeat(110));

        int progress = menu.getScaledProgress();
        if (progress > 0)
            g.blit(TEXTURE, x + PROG_X, y + PROG_Y, PROG_SRC_U, PROG_SRC_V, progress, PROG_H, TEX_W, TEX_H);

        FluidGauge.draw(g, menu.blockEntity.outputTank.getFluid(),
                menu.blockEntity.outputTank.getCapacity(), x + TANK_X, y + TANK_Y, TANK_W, TANK_H);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, x + TANK_X, y + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);
    }

    private void renderFluidTooltip(GuiGraphics g, int mx, int my) {
        FluidGauge.tooltip(g, font, mx, my, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H,
                menu.blockEntity.outputTank.getFluid(), menu.blockEntity.outputTank.getCapacity());
    }
}
