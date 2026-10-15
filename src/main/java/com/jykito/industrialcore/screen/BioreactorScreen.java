package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.BioreactorBlockEntity;
import com.jykito.industrialcore.menu.BioreactorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Bioreactor window: the ore washer layout (water tank, input, three outputs, upgrades) plus the additive slot above the input,
 * a heat bar under the energy bar (author, 2026-10-10: "remove the heat hint, just a heat bar").
 */
public class BioreactorScreen extends MachineScreen<BioreactorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/bioreactor_gui.png");
    private static final int BAR_X = 72, BAR_Y = 97, BAR_W = 110, BAR_H = 6, HEAT_Y = 107;
    private static final int TANK_W = 16, TANK_H = 48, TANK_X = 57, TANK_Y = 39;
    private static final int GLASS_U = 130, GLASS_V = 229;
    private static final int PROG_X = 118, PROG_Y = 56, PROG_W = 16, PROG_H = 12;
    private static final int PROG_SRC_U = 65, PROG_SRC_V = 245;

    public BioreactorScreen(BioreactorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        Bars.energy(g, font, mouseX, mouseY, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H, menu.getEnergyStored(), menu.getMaxEnergy());
        Bars.heat(g, font, mouseX, mouseY, leftPos + BAR_X, topPos + HEAT_Y, BAR_W, BAR_H, menu.getHeat(), BioreactorBlockEntity.MAX_HEAT);
        FluidGauge.tooltip(g, font, mouseX, mouseY, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H,
                menu.blockEntity.waterTank.getFluid(), menu.blockEntity.waterTank.getCapacity());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 315);

        int filled = menu.getScaledEnergy();
        if (filled > 0) bars.draw(g, FillBar.ENERGY, leftPos + BAR_X, topPos + BAR_Y, filled, BAR_H);
        int heat = Math.min(BAR_W, menu.getHeat() * BAR_W / BioreactorBlockEntity.MAX_HEAT);
        if (heat > 0) bars.draw(g, FillBar.HEAT, leftPos + BAR_X, topPos + HEAT_Y, heat, BAR_H);

        int progress = menu.getScaledProgress(PROG_W);
        if (progress > 0) g.blit(TEXTURE, leftPos + PROG_X, topPos + PROG_Y, PROG_SRC_U, PROG_SRC_V, progress, PROG_H, 256, 315);

        FluidGauge.draw(g, menu.blockEntity.waterTank.getFluid(), menu.blockEntity.waterTank.getCapacity(), leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, leftPos + TANK_X, topPos + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, 256, 315);
        RenderSystem.disableBlend();
    }
}
