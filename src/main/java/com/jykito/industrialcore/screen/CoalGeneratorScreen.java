package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.CoalGeneratorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class CoalGeneratorScreen extends AbstractContainerScreen<CoalGeneratorMenu> {

    private static final ResourceLocation TEXTURE    = new ResourceLocation(IndustrialCore.MODID, "textures/gui/coal_generator_gui.png");
    private static final ResourceLocation ENERGY_FILL = new ResourceLocation(IndustrialCore.MODID, "textures/gui/energy_fill_6x_anim.png");

    private static final int FILL_W = 96, FILL_H = 6, ANIM_H = 36, FRAMES = 6, FRAME_TICKS = 32;
    private static final int BAR_X = 119, BAR_Y = 50, BAR_W = 42, BAR_H = 6;
    private static final int INFO_X = 93, INFO_Y = 74;

    private int animTick = 0, animFrame = 0;

    public CoalGeneratorScreen(CoalGeneratorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth  = 256;
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
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int x = leftPos, y = topPos;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 315);

        int filled = menu.getScaledEnergy();
        if (filled > 0) {
            int vOffset = animFrame * FILL_H;
            int drawX = x + BAR_X;
            int endX  = x + BAR_X + filled;
            while (drawX < endX) {
                int tileW = Math.min(FILL_W, endX - drawX);
                guiGraphics.blit(ENERGY_FILL, drawX, y + BAR_Y, 0, vOffset, tileW, BAR_H, FILL_W, ANIM_H);
                drawX += FILL_W;
            }
        }

        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack fuel = menu.slots.get(0).getItem();
        int burn    = menu.getBurnProgress();
        int maxBurn = menu.getMaxBurnTime();

        Component line1 = fuel.isEmpty()
                ? Component.translatable("gui.industrial_core.coal_gen.no_fuel")
                : fuel.getHoverName();
        String line2 = burn > 0 ? (burn / 20) + "s / " + (maxBurn / 20) + "s" : "-";
        String line3 = burn > 0 ? menu.getEnergyPerTick() + " FE/t" : "0 FE/t";

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(INFO_X + 2, INFO_Y + 4, 0);
        guiGraphics.pose().scale(0.5f, 0.5f, 1f);

        guiGraphics.drawString(font, line1,  0,  0, 0xFFFFFF, false);
        guiGraphics.drawString(font, line2,  0, 18, 0xCCCCCC, false);
        guiGraphics.drawString(font, line3,  0, 36, com.jykito.industrialcore.ModStyle.ENERGY, false);

        guiGraphics.pose().popPose();
    }
}
