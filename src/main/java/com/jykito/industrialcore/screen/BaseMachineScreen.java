package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.menu.BaseMachineMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public abstract class BaseMachineScreen<T extends BaseMachineMenu> extends MachineScreen<T> {

    private static final int BAR_X = 72, BAR_Y = 97, BAR_W = 110, BAR_H = 6;

    protected int PROG_X = 117, PROG_Y = 61, PROG_W = 16, PROG_H = 12;
    protected int PROG_SRC_U = 122, PROG_SRC_V = 243;

    private final ResourceLocation texture;

    public BaseMachineScreen(T pMenu, Inventory pPlayerInventory, Component pTitle, ResourceLocation texture) {
        super(pMenu, pPlayerInventory, pTitle);
        this.texture = texture;
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        Bars.energy(g, font, mouseX, mouseY, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H,
                menu.getEnergyStored(), menu.getMaxEnergy());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(texture, x, y, 0, 0, imageWidth, imageHeight, 256, 315);

        int filled = menu.getScaledEnergy();
        bars.draw(g, FillBar.ENERGY, x + BAR_X, y + BAR_Y, filled, BAR_H);

        int progress = menu.getScaledProgress(PROG_W);
        if (progress > 0) {
            g.blit(texture, x + PROG_X, y + PROG_Y, PROG_SRC_U, PROG_SRC_V, progress, PROG_H, 256, 315);
        }
    }
}
