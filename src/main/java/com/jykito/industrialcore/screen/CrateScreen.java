package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.CrateMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CrateScreen extends AbstractContainerScreen<CrateMenu> {

    private final ResourceLocation texture;
    private final CrateMenu.Layout layout;

    public CrateScreen(CrateMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.layout = CrateMenu.layout(menu.getRows());
        this.texture = new ResourceLocation(IndustrialCore.MODID,
                "textures/gui/crate_" + layout.tier() + "_gui.png");
        this.imageWidth  = CrateMenu.GUI_W;
        this.imageHeight = CrateMenu.GUI_H;
    }

    @Override
    protected void init() {
        super.init();

        // Shift the window so the player inventory ends up at the same screen height for every tier:
        // the crate grows upwards, the bottom of the window stays put. The adjustment is made BEFORE
        // the buttons are created, since they are positioned from topPos.
        this.topPos += CrateMenu.ANCHOR_INV_Y - layout.invY();

        this.inventoryLabelY = 10000;   // labels are drawn on the art itself
        this.titleLabelY     = 10000;

        addRenderableWidget(hitbox(layout.restockY(), "restock", 2));
        addRenderableWidget(hitbox(layout.depositY(), "deposit", 1));
        addRenderableWidget(hitbox(layout.sortY(),    "sort",    0));
    }

    /**
     * The button icons are drawn right into the texture, so the widget here is a transparent
     * click zone over them: it draws no frame or glyph of its own, only a hover highlight.
     */
    private Button hitbox(int y, String key, int id) {
        Button b = new Button(leftPos + CrateMenu.BTN_X, topPos + y, CrateMenu.BTN_SIZE, CrateMenu.BTN_SIZE,
                Component.empty(),
                btn -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null)
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
                },
                sup -> sup.get()) {
            @Override
            protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                if (isHovered()) g.fill(getX(), getY(), getX() + width, getY() + height, 0x40FFFFFF);
            }
        };
        b.setTooltip(Tooltip.create(Component.translatable("gui.industrial_core.crate." + key)));
        return b;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, CrateMenu.GUI_W, CrateMenu.GUI_H);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
