package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.BatteryChargerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BatteryChargerScreen extends MachineScreen<BatteryChargerMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/battery_charger_gui.png");

    private static final int BAR_X = 72, BAR_Y = 98, BAR_H = 6;

    private static final int CHARGE_X = 111, CHARGE_Y = 54, CHARGE_W = 34, CHARGE_H = 28;
    private static final int CHARGE_SRC_U = 108, CHARGE_SRC_V = 237;

    public BatteryChargerScreen(BatteryChargerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {

    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        Bars.energy(g, font, mouseX, mouseY, leftPos + BAR_X, topPos + BAR_Y, 110, BAR_H,
                menu.getEnergy(), menu.getMaxEnergy());
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

        int charge = menu.getScaledCharge(CHARGE_H);
        if (charge > 0) {
            int drawY = y + CHARGE_Y + (CHARGE_H - charge);
            int srcV  = CHARGE_SRC_V + (CHARGE_H - charge);
            g.blit(TEXTURE, x + CHARGE_X, drawY, CHARGE_SRC_U, srcV, CHARGE_W, charge, TEX_W, TEX_H);
        }
    }
}
