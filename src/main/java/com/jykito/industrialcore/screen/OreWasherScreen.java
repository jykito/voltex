package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.OreWasherBlockEntity;
import com.jykito.industrialcore.menu.OreWasherMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class OreWasherScreen extends MachineScreen<OreWasherMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(IndustrialCore.MODID + ":textures/gui/ore_washer_gui.png");
    private static final int BAR_X = 72, BAR_Y = 97, BAR_H = 6;

    private static final int TANK_W = 16, TANK_H = 48, TANK_X = 57, TANK_Y = 39;

    private static final int GLASS_U = 130, GLASS_V = 229;

    private static final int PROG_X = 118, PROG_Y = 56, PROG_W = 16, PROG_H = 12;
    private static final int PROG_SRC_U = 65, PROG_SRC_V = 245;

    public OreWasherScreen(OreWasherMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        Bars.energy(g, font, mouseX, mouseY, leftPos + BAR_X, topPos + BAR_Y, 110, BAR_H,
                menu.getEnergyStored(), menu.getMaxEnergy());
        OreWasherBlockEntity entity = (OreWasherBlockEntity) menu.blockEntity;
        FluidGauge.tooltip(g, font, mouseX, mouseY, this.leftPos + TANK_X, this.topPos + TANK_Y, TANK_W, TANK_H,
                entity.waterTank.getFluid(), entity.waterTank.getCapacity());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight, 256, 315);

        int filled = menu.getScaledEnergy();
        if (filled > 0) {
            int barX = this.leftPos + BAR_X, barY = this.topPos + BAR_Y;
            bars.draw(g, FillBar.ENERGY, barX, barY, filled, BAR_H);
        }

        int progress = menu.getScaledProgress(PROG_W);
        if (progress > 0) {
            g.blit(TEXTURE, this.leftPos + PROG_X, this.topPos + PROG_Y, PROG_SRC_U, PROG_SRC_V, progress, PROG_H, 256, 315);
        }

        OreWasherBlockEntity entity = (OreWasherBlockEntity) menu.blockEntity;
        FluidGauge.draw(g, entity.waterTank.getFluid(), entity.waterTank.getCapacity(),
                this.leftPos + TANK_X, this.topPos + TANK_Y, TANK_W, TANK_H);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, this.leftPos + TANK_X, this.topPos + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, 256, 315);
        RenderSystem.disableBlend();
    }

}
