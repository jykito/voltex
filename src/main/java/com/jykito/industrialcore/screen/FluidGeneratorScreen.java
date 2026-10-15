package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.FluidGeneratorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FluidGeneratorScreen extends MachineScreen<FluidGeneratorMenu> {

    private static final ResourceLocation TEX_LAVA =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/lava_gen_gui.png");
    private static final ResourceLocation TEX_WATER =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/water_gen_gui.png");

    private static final int ENERGY_X = 72, ENERGY_Y = 97, BAR_H = 6;

    private static final int TANK_X = 120, TANK_Y = 40, TANK_W = 16, TANK_H = 48;
    private static final int GLASS_U = 167, GLASS_V = 244;

    public FluidGeneratorScreen(FluidGeneratorMenu menu, Inventory inv, Component title) {
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
        ResourceLocation TEXTURE = menu.blockEntity.getGeneratorType()
                == com.jykito.industrialcore.block.entity.FluidGeneratorType.LAVA ? TEX_LAVA : TEX_WATER;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        bars.draw(g, FillBar.ENERGY, x + ENERGY_X, y + ENERGY_Y, menu.getScaledEnergy());

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
