package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.MatterFabricatorBlockEntity;
import com.jykito.industrialcore.menu.MatterFabricatorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MatterFabricatorScreen extends AbstractContainerScreen<MatterFabricatorMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/matter_fabricator_gui.png");

    private static final int PROG_X = 83, PROG_Y = 37, PROG_W = 88, PROG_H = 68;
    private static final int PROG_SRC_U = 83, PROG_SRC_V = 243;

    private static final int TIP_X = 110, TIP_Y = 48, TIP_W = 36, TIP_H = 36;

    public MatterFabricatorScreen(MatterFabricatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
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
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        int px = leftPos + TIP_X, py = topPos + TIP_Y;
        if (this.hoveredSlot == null && mouseX >= px && mouseX < px + TIP_W && mouseY >= py && mouseY < py + TIP_H) {
            int pct = menu.getScaledEnergy(100);
            g.renderTooltip(this.font, Component.translatable("screen.industrial_core.matter.progress", pct, MatterFabricatorBlockEntity.COST / 1_000_000), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 315);

        int h = menu.getScaledEnergy(PROG_H);
        if (h > 0) {
            g.blit(TEXTURE, leftPos + PROG_X, topPos + PROG_Y + (PROG_H - h),
                    PROG_SRC_U, PROG_SRC_V + (PROG_H - h), PROG_W, h, 256, 315);
        }
    }
}
