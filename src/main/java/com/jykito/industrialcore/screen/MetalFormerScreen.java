package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.MetalFormerMenu;
import com.jykito.industrialcore.networking.MetalFormerModePacket;
import com.jykito.industrialcore.networking.ModMessages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MetalFormerScreen extends BaseMachineScreen<MetalFormerMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/metal_former_gui.png");

    private ModeButton modeButton;

    public MetalFormerScreen(MetalFormerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle, TEXTURE);
        this.imageWidth = 256;
        this.imageHeight = 224;

        this.PROG_X = 107; this.PROG_Y = 60; this.PROG_W = 33; this.PROG_H = 14;
        this.PROG_SRC_U = 98; this.PROG_SRC_V = 247;
    }

    @Override
    protected void init() {
        super.init();
        int buttonX = this.leftPos + 121;
        int buttonY = this.topPos + 63;
        this.modeButton = this.addRenderableWidget(new ModeButton(buttonX, buttonY));
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        if (this.modeButton != null && this.modeButton.isMouseOver(pMouseX, pMouseY)) {
            int mode = menu.getMode();
            Component tooltip = switch (mode) {
                case 1 -> Component.translatable("gui.industrial_core.mode.extruding");
                default -> Component.translatable("gui.industrial_core.mode.rolling");
            };

            pGuiGraphics.renderTooltip(this.font, tooltip, pMouseX, pMouseY);
        }
    }

    private class ModeButton extends AbstractButton {
        public ModeButton(int x, int y) {
            super(x, y, 8, 8, Component.empty());
        }

        @Override
        public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            int mode = menu.getMode();
            int u = 104 + (mode * 8);
            int v = 229;
            pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), u, v, this.width, this.height, 256, 315);
        }

        @Override
        public void onPress() {
            ModMessages.sendToServer(new MetalFormerModePacket(menu.getBlockEntity().getBlockPos()));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
            this.defaultButtonNarrationText(pNarrationElementOutput);
        }
    }
}
