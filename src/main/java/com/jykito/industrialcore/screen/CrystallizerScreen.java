package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.CrystallizerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CrystallizerScreen extends MachineScreen<CrystallizerMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/crystallizer_gui.png");

    private static final int ENERGY_X = 91, ENERGY_Y = 96, ENERGY_W = 81, BAR_H = 6;

    private static final int TANK_X = 64, TANK_Y = 40, TANK_W = 16, TANK_H = 64;
    private static final int GLASS_U = 192, GLASS_V = 237;

    private static final int PROG_X = 115, PROG_Y = 60, PROG_W = 23, PROG_H = 16;
    private static final int PROG_SRC_U = 101, PROG_SRC_V = 254;

    public CrystallizerScreen(CrystallizerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        Bars.energy(g, font, mx, my, leftPos + ENERGY_X, topPos + ENERGY_Y, ENERGY_W, BAR_H,
                menu.getEnergyStored(), menu.getMaxEnergy());
        renderFluidTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        bars.draw(g, FillBar.ENERGY, x + ENERGY_X, y + ENERGY_Y, menu.getScaledEnergy(ENERGY_W));

        int progress = menu.getScaledProgress(PROG_W);
        if (progress > 0)
            g.blit(TEXTURE, x + PROG_X, y + PROG_Y, PROG_SRC_U, PROG_SRC_V, progress, PROG_H, TEX_W, TEX_H);

        FluidGauge.draw(g, menu.blockEntity.inputTank.getFluid(),
                menu.blockEntity.inputTank.getCapacity(), x + TANK_X, y + TANK_Y, TANK_W, TANK_H);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, x + TANK_X, y + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);
    }

    private void renderFluidTooltip(GuiGraphics g, int mx, int my) {
        FluidGauge.tooltip(g, font, mx, my, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H,
                menu.blockEntity.inputTank.getFluid(), menu.blockEntity.inputTank.getCapacity());
    }
}
