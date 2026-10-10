package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.CoalGeneratorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class CoalGeneratorScreen extends MachineScreen<CoalGeneratorMenu> {

    private static final ResourceLocation TEXTURE    = new ResourceLocation(IndustrialCore.MODID, "textures/gui/coal_generator_gui.png");

    private static final int BAR_X = 119, BAR_Y = 50, BAR_W = 42, BAR_H = 6;
    private static final int INFO_X = 93, INFO_Y = 74;

    public CoalGeneratorScreen(CoalGeneratorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Bars.energy(guiGraphics, font, mouseX, mouseY, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H,
                menu.getEnergyStored(), menu.getMaxEnergy());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int x = leftPos, y = topPos;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 315);

        bars.draw(guiGraphics, FillBar.ENERGY, x + BAR_X, y + BAR_Y, menu.getScaledEnergy(), BAR_H);

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
