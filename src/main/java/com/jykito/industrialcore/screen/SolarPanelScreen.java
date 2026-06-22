package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.SolarPanelMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SolarPanelScreen extends AbstractContainerScreen<SolarPanelMenu> {

    private final ResourceLocation TEXTURE;
    private static final ResourceLocation ENERGY_FILL =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/energy_fill_6x_anim.png");

    private static final int TEX_W = 256, TEX_H = 315;

    private static final int FILL_W = 96, FILL_H = 6, ANIM_H = 36, FRAMES = 6, FRAME_TICKS = 32;
    private int animTick = 0, currentFrame = 0;

    private static final int BAR_X = 73, BAR_Y = 97, BAR_W = 110, BAR_H = 6;

    private static final int INFO_X = 55, INFO_Y = 36, INFO_W = 146, INFO_H = 43;

    private static final int[] TIER_COLORS = {
            0xFF2B6FBE,
            0xFF1FA3C9,
            0xFF0EB89E,
            0xFF8B44B5,
            0xFFD4A017
    };

    private static final int TEXT_DIM    = 0xFF8B949E;
    private static final int TEXT_BRIGHT = 0xFFE6EDF3;
    private static final int ENERGY_BLUE = 0xFF000000 | com.jykito.industrialcore.ModStyle.ENERGY;

    public SolarPanelScreen(SolarPanelMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth  = 256;
        imageHeight = 240;
        TEXTURE = new ResourceLocation(IndustrialCore.MODID,
                "textures/gui/solar_gui_t" + menu.blockEntity.getTierLevel() + ".png");
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = 10000;
        titleLabelY     = 10000;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        if (++animTick >= FRAME_TICKS) { animTick = 0; currentFrame = (currentFrame + 1) % FRAMES; }
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        int filled = menu.getScaledEnergy(BAR_W);
        if (filled > 0) {
            int barX = x + BAR_X, barY = y + BAR_Y;
            int vOffset = currentFrame * FILL_H;
            int drawX = barX;
            while (drawX < barX + filled) {
                int tileW = Math.min(FILL_W, barX + filled - drawX);
                g.blit(ENERGY_FILL, drawX, barY, 0, vOffset, tileW, FILL_H, FILL_W, ANIM_H);
                drawX += FILL_W;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        int gen = menu.getCurrentGen();

        int tx = INFO_X + 4, ty = INFO_Y + 4;

        g.pose().pushPose();
        g.pose().translate(tx, ty, 0);
        g.pose().scale(0.5f, 0.5f, 1f);

        String panelName = menu.blockEntity.getBlockState().getBlock().getName().getString();
        g.drawString(font, panelName, 0, 0, TEXT_BRIGHT, false);

        Component bufferLine = Component.translatable("gui.industrial_core.solar.buffer").append(": ")
                .withStyle(s -> s.withColor(TEXT_BRIGHT))
                .append(Component.literal(formatFE(menu.getEnergyStored()) + " / " + formatFE(menu.getMaxEnergy()) + " FE")
                        .withStyle(s -> s.withColor(ENERGY_BLUE)));
        g.drawString(font, bufferLine, 0, 18, TEXT_BRIGHT, false);

        Component genValue = gen > 0
                ? Component.literal(formatFE(gen) + " FE/t").withStyle(s -> s.withColor(ENERGY_BLUE))
                : Component.translatable("gui.industrial_core.solar.no_gen").withStyle(s -> s.withColor(TEXT_DIM));
        Component genLine = Component.translatable("gui.industrial_core.solar.generation").append(": ")
                .withStyle(s -> s.withColor(TEXT_BRIGHT))
                .append(genValue);
        g.drawString(font, genLine, 0, 36, TEXT_BRIGHT, false);

        g.pose().popPose();
    }

    private static String formatFE(int fe) {
        if (fe >= 1_000_000) return String.format("%.1fM", fe / 1_000_000.0);
        if (fe >= 1_000)     return String.format("%.1fk", fe / 1_000.0);
        return String.valueOf(fe);
    }
}
