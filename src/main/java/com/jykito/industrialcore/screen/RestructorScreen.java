package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.RestructorBlockEntity;
import com.jykito.industrialcore.menu.RestructorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RestructorScreen extends AbstractContainerScreen<RestructorMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID + ":textures/gui/restructor_gui.png");

    private static final int PROGRESS_X = 122, PROGRESS_Y = 59, PROGRESS_W = 12, PROGRESS_H = 17;
    private static final int PROGRESS_SRC_U = 166, PROGRESS_SRC_V = 260;

    private static final int SRC_X = 103, SRC_Y = 75, SRC_W = 50, SRC_H = 16;
    private static final int SRC_SRC_U = 90, SRC_SRC_V = 259;
    private static final int CATALYST_SLOT = 2;

    public RestructorScreen(RestructorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth  = 256;
        this.imageHeight = 224;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY     = 10000;
    }

    @Override public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderBarTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 315);

        if (menu.getSlot(CATALYST_SLOT).hasItem()) {
            guiGraphics.blit(TEXTURE, x + SRC_X, y + SRC_Y, SRC_SRC_U, SRC_SRC_V, SRC_W, SRC_H, 256, 315);
        }

        int sp = menu.getScaledProgress();
        if (sp > 0) {
            guiGraphics.blit(TEXTURE,
                    x + PROGRESS_X, y + PROGRESS_Y + PROGRESS_H - sp,
                    PROGRESS_SRC_U, PROGRESS_SRC_V + (PROGRESS_H - sp),
                    PROGRESS_W, sp, 256, 315);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    private void renderBarTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H, mouseX, mouseY)) {
            int energy = menu.getEnergyAccumulated();
            int cost   = menu.getMaxRecipeCost();
            String costStr = cost > 0 ? formatFE(cost) : "—";
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("screen.industrial_core.restructor.charge", formatFE(energy), costStr),
                    mouseX, mouseY);
        }
    }

    private boolean isHovering(int relX, int relY, int w, int h, int mouseX, int mouseY) {
        int ax = this.leftPos + relX;
        int ay = this.topPos  + relY;
        return mouseX >= ax && mouseX < ax + w && mouseY >= ay && mouseY < ay + h;
    }

    private static String formatFE(int fe) {
        if (fe >= 1_000_000) return String.format("%.1fM", fe / 1_000_000.0);
        if (fe >= 1_000)     return String.format("%.1fk", fe / 1_000.0);
        return fe + "";
    }
}
