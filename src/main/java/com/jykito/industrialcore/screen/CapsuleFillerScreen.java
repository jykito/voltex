package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.CapsuleFillerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CapsuleFillerScreen extends MachineScreen<CapsuleFillerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/capsule_filler_gui.png");

    private static final int ENERGY_X = 72, ENERGY_Y = 97, BAR_H = 6;

    private static final int TANK_X = 102, TANK_Y = 40, TANK_W = 16, TANK_H = 48;
    private static final int GLASS_U = 132, GLASS_V = 241;

    private static final int PROG_X = 127, PROG_Y = 58, PROG_W = 8, PROG_H = 13;
    private static final int PROG_SRC_U = 96, PROG_SRC_V = 249;

    public CapsuleFillerScreen(CapsuleFillerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        renderFluidTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        bars.draw(g, FillBar.ENERGY, x + ENERGY_X, y + ENERGY_Y, menu.getScaledEnergy());

        int ph = menu.getScaledProgress();
        if (ph > 0)
            g.blit(TEXTURE, x + PROG_X, y + PROG_Y, PROG_SRC_U, PROG_SRC_V, PROG_W, ph, TEX_W, TEX_H);

        FluidGauge.draw(g, menu.blockEntity.getFluidInTank(), menu.blockEntity.getTankCapacity(),
                x + TANK_X, y + TANK_Y, TANK_W, TANK_H);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, x + TANK_X, y + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);
    }

    private void renderFluidTooltip(GuiGraphics g, int mx, int my) {
        FluidGauge.tooltip(g, font, mx, my, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H,
                menu.blockEntity.getFluidInTank(), menu.blockEntity.getTankCapacity());
    }
}
