package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.BlastFurnaceMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BlastFurnaceScreen extends MachineScreen<BlastFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/blast_furnace_gui.png");

    private static final int HEAT_X = 88,  HEAT_Y = 97, HEAT_W = 73, HEAT_H = 6;

    private static final int PROG_X = 110, PROG_Y = 49, PROG_W = 28, PROG_H = 20;
    private static final int PROG_SRC_U = 119, PROG_SRC_V = 239;

    public BlastFurnaceScreen(BlastFurnaceMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        Bars.heat(g, font, mouseX, mouseY, leftPos + HEAT_X, topPos + HEAT_Y, HEAT_W, HEAT_H,
                menu.getHeat(), menu.getMaxHeat());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 315);

        int heatFilled = menu.getScaledHeat();
        bars.draw(g, FillBar.HEAT, x + HEAT_X, y + HEAT_Y, heatFilled, HEAT_H);

        int progress = menu.getScaledProgress();
        if (progress > 0) {
            g.blit(TEXTURE, x + PROG_X, y + PROG_Y, PROG_SRC_U, PROG_SRC_V, progress, PROG_H, 256, 315);
        }
    }
}
