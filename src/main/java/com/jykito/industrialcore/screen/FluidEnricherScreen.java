package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.FluidEnricherBlockEntity;
import com.jykito.industrialcore.menu.FluidEnricherMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Matrix4f;

public class FluidEnricherScreen extends MachineScreen<FluidEnricherMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(IndustrialCore.MODID + ":textures/gui/fluid_enricher_gui.png");

    private static final int BAR_X = 74, BAR_Y = 97, BAR_W = 110, BAR_H = 6;

    private static final int TANK_W = 16, TANK_H = 64;
    private static final int IN_TANK_X = 48,  IN_TANK_Y = 24;
    private static final int OUT_TANK_X = 188, OUT_TANK_Y = 24;

    private static final int GLASS_U = 181, GLASS_V = 233;

    private static final int PROG_X = 103, PROG_Y = 42, PROG_W = 46, PROG_H = 26;
    private static final int PROG_SRC_U = 101, PROG_SRC_V = 262;

    public FluidEnricherScreen(FluidEnricherMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Bars.energy(guiGraphics, font, mouseX, mouseY, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H,
                menu.getEnergyStored(), menu.getMaxEnergy());
        FluidEnricherBlockEntity entity = (FluidEnricherBlockEntity) menu.blockEntity;
        FluidGauge.tooltip(guiGraphics, font, mouseX, mouseY, this.leftPos + IN_TANK_X,  this.topPos + IN_TANK_Y,  TANK_W, TANK_H, entity.inputTank.getFluid(),  entity.inputTank.getCapacity());
        FluidGauge.tooltip(guiGraphics, font, mouseX, mouseY, this.leftPos + OUT_TANK_X, this.topPos + OUT_TANK_Y, TANK_W, TANK_H, entity.outputTank.getFluid(), entity.outputTank.getCapacity());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight, 256, 315);

        int filled = menu.getScaledEnergy();
        if (filled > 0) {
            int barX = this.leftPos + BAR_X, barY = this.topPos + BAR_Y;
            bars.draw(guiGraphics, FillBar.ENERGY, barX, barY, filled, BAR_H);
        }

        int progressWidth = menu.getScaledProgress(PROG_W);
        if (progressWidth > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + PROG_X, this.topPos + PROG_Y, PROG_SRC_U, PROG_SRC_V, progressWidth, PROG_H, 256, 315);
        }

        FluidEnricherBlockEntity entity = (FluidEnricherBlockEntity) menu.blockEntity;

        FluidGauge.draw(guiGraphics, entity.inputTank.getFluid(),  entity.inputTank.getCapacity(),  this.leftPos + IN_TANK_X,  this.topPos + IN_TANK_Y,  TANK_W, TANK_H);
        FluidGauge.draw(guiGraphics, entity.outputTank.getFluid(), entity.outputTank.getCapacity(), this.leftPos + OUT_TANK_X, this.topPos + OUT_TANK_Y, TANK_W, TANK_H);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(TEXTURE, this.leftPos + IN_TANK_X,  this.topPos + IN_TANK_Y,  GLASS_U, GLASS_V, TANK_W, TANK_H, 256, 315);
        guiGraphics.blit(TEXTURE, this.leftPos + OUT_TANK_X, this.topPos + OUT_TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, 256, 315);

        RenderSystem.disableBlend();
    }

}
