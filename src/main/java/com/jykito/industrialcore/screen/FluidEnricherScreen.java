package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.FluidEnricherBlockEntity;
import com.jykito.industrialcore.menu.FluidEnricherMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix4f;

public class FluidEnricherScreen extends AbstractContainerScreen<FluidEnricherMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(IndustrialCore.MODID + ":textures/gui/fluid_enricher_gui.png");
    private static final ResourceLocation ENERGY_FILL = new ResourceLocation(IndustrialCore.MODID, "textures/gui/energy_fill_6x_anim.png");

    private static final int FILL_W = 96, FILL_H = 6, ANIM_H = 36, FRAMES = 6, FRAME_TICKS = 32;
    private static final int BAR_X = 74, BAR_Y = 97, BAR_W = 110, BAR_H = 6;

    private int animTick = 0, animFrame = 0;

    private static final int TANK_W = 16, TANK_H = 64;
    private static final int IN_TANK_X = 48,  IN_TANK_Y = 24;
    private static final int OUT_TANK_X = 188, OUT_TANK_Y = 24;

    private static final int GLASS_U = 181, GLASS_V = 233;

    private static final int PROG_X = 79, PROG_Y = 42, PROG_H = 26;
    private static final int PROG_SRC_U = 65, PROG_SRC_V = 235;

    public FluidEnricherScreen(FluidEnricherMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 256;
        this.imageHeight = 224;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        if (++animTick >= FRAME_TICKS) { animTick = 0; animFrame = (animFrame + 1) % FRAMES; }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        FluidEnricherBlockEntity entity = (FluidEnricherBlockEntity) menu.blockEntity;

        renderFluidTooltip(guiGraphics, mouseX, mouseY, this.leftPos + IN_TANK_X,  this.topPos + IN_TANK_Y,  TANK_W, TANK_H, entity.inputTank.getFluid(),  entity.inputTank.getCapacity());
        renderFluidTooltip(guiGraphics, mouseX, mouseY, this.leftPos + OUT_TANK_X, this.topPos + OUT_TANK_Y, TANK_W, TANK_H, entity.outputTank.getFluid(), entity.outputTank.getCapacity());
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
            int vOffset = animFrame * FILL_H;
            int drawX = barX;
            while (drawX < barX + filled) {
                int tileW = Math.min(FILL_W, barX + filled - drawX);
                guiGraphics.blit(ENERGY_FILL, drawX, barY, 0, vOffset, tileW, BAR_H, FILL_W, ANIM_H);
                drawX += FILL_W;
            }
        }

        int progressWidth = menu.getScaledProgress();
        if (progressWidth > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + PROG_X, this.topPos + PROG_Y, PROG_SRC_U, PROG_SRC_V, progressWidth, PROG_H, 256, 315);
        }

        FluidEnricherBlockEntity entity = (FluidEnricherBlockEntity) menu.blockEntity;

        renderFluidTank(guiGraphics, entity.inputTank.getFluid(),  entity.inputTank.getCapacity(),  this.leftPos + IN_TANK_X,  this.topPos + IN_TANK_Y,  TANK_W, TANK_H);
        renderFluidTank(guiGraphics, entity.outputTank.getFluid(), entity.outputTank.getCapacity(), this.leftPos + OUT_TANK_X, this.topPos + OUT_TANK_Y, TANK_W, TANK_H);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(TEXTURE, this.leftPos + IN_TANK_X,  this.topPos + IN_TANK_Y,  GLASS_U, GLASS_V, TANK_W, TANK_H, 256, 315);
        guiGraphics.blit(TEXTURE, this.leftPos + OUT_TANK_X, this.topPos + OUT_TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, 256, 315);

        RenderSystem.disableBlend();
    }

    private void renderFluidTank(GuiGraphics guiGraphics, FluidStack fluidStack, int capacity, int x, int y, int width, int height) {
        if (fluidStack.isEmpty()) return;

        int fluidHeight = (int) ((float) fluidStack.getAmount() / capacity * height);
        if (fluidHeight <= 0) return;

        IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation texture = clientFluid.getStillTexture(fluidStack);
        if (texture == null) return;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        int color = clientFluid.getTintColor(fluidStack);

        if (fluidStack.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                || fluidStack.getFluid() == net.minecraft.world.level.material.Fluids.FLOWING_WATER) {
            color = 0xFF3F76E4;
        }

        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = ((color >> 24) & 0xFF) / 255.0F;
        if (a == 0.0F) a = 1.0F;

        RenderSystem.enableBlend();
        guiGraphics.setColor(r, g, b, a);
        for (int i = 0; i < width; i += 16) {
            for (int j = 0; j < fluidHeight; j += 16) {
                int drawWidth  = Math.min(width - i, 16);
                int drawHeight = Math.min(fluidHeight - j, 16);
                int drawX = x + i;
                int drawY = y + height - j - drawHeight;
                guiGraphics.blit(drawX, drawY, 0, drawWidth, drawHeight, sprite);
            }
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void renderFluidTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height, FluidStack fluidStack, int capacity) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            Component tooltipText;
            if (fluidStack.isEmpty()) {
                tooltipText = Component.translatable("gui.industrial_core.empty");
            } else {
                Component fluidName = fluidStack.getFluid().getFluidType().getDescription(fluidStack);
                tooltipText = Component.literal("").append(fluidName).append(": " + fluidStack.getAmount() + " / " + capacity + " mB");
            }
            guiGraphics.renderTooltip(this.font, tooltipText, mouseX, mouseY);
        }
    }
}
