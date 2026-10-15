package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.menu.MinerCoreMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

public class MinerCoreScreen extends MachineScreen<MinerCoreMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/miner_core_gui.png");

    private static final int ENERGY_X = 93, ENERGY_Y = 105, ENERGY_W = 69, BAR_H = 6;

    private static final int TANK_X = 64, TANK_Y = 43, TANK_W = 16, TANK_H = 64;
    private static final int GLASS_U = 192, GLASS_V = 243;

    private static final int PROG_X = 111, PROG_Y = 53, PROG_W = 30, PROG_H = 34;
    private static final int PROG_SRC_U = 106, PROG_SRC_V = 251;

    public MinerCoreScreen(MinerCoreMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        Bars.energy(g, font, mx, my, leftPos + ENERGY_X, topPos + ENERGY_Y, ENERGY_W, BAR_H,
                menu.getTotalEnergy(), menu.getMaxTotalEnergy());
        int fx = leftPos + TANK_X, fy = topPos + TANK_Y;
        if (mx >= fx && mx < fx + TANK_W && my >= fy && my < fy + TANK_H)
            g.renderTooltip(font, Component.literal(
                    menu.getTotalFluid() + " / " + menu.getMaxTotalFluid() + " mB"), mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        int filled = menu.getScaledEnergy(ENERGY_W);
        bars.draw(g, FillBar.ENERGY, x + ENERGY_X, y + ENERGY_Y, filled, BAR_H);

        // Fill from bottom to top: take the bottom `progress` pixels of the source
        // and put them at the bottom edge of the area, so the offset is the same for src and dst.
        int progress = menu.getScaledProgress(PROG_H);
        if (progress > 0) {
            int offset = PROG_H - progress;
            g.blit(TEXTURE, x + PROG_X, y + PROG_Y + offset,
                    PROG_SRC_U, PROG_SRC_V + offset, PROG_W, progress, TEX_W, TEX_H);
        }

        FluidGauge.draw(g, new FluidStack(ModFluids.SOURCE_DRILL_FLUID.get(), menu.getTotalFluid()),
                menu.getMaxTotalFluid(), x + TANK_X, y + TANK_Y, TANK_W, TANK_H);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, x + TANK_X, y + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, TEX_W, TEX_H);
    }

}
