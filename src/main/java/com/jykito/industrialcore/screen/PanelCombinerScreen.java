package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.PanelCombinerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PanelCombinerScreen extends MachineScreen<PanelCombinerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/panel_combiner.png");

    private static final int BAR_X = 73, BAR_Y = 119, BAR_W = 110, BAR_H = 6;
    /** Readings in the screen recess (window O3, author 2026-10-09): three lines in full-size font, step 11 (scale 0.5 before). */
    private static final int INFO_X = 54, INFO_Y = 78, LINE = 11;

    private static final int TEXT_BRIGHT = 0xFFE6EDF3;
    private static final int TEXT_DIM    = 0xFF8B949E;
    private static final int ENERGY_BLUE = 0xFF29B6F6;
    private static final int ACCENT      = 0xFFD4A017;

    public PanelCombinerScreen(PanelCombinerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth  = 256;
        imageHeight = 240;
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        Bars.energy(g, font, mx, my, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H,
                menu.getEnergyStored(), menu.getMaxEnergy());
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;

        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        int filled = menu.getScaledEnergy(BAR_W);
        if (filled > 0) {
            int barX = x + BAR_X, barY = y + BAR_Y;
            bars.draw(g, FillBar.ENERGY, barX, barY, filled);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        long gen = menu.getCurrentGen();
        int out = menu.getCurrentOutput();

        g.pose().pushPose();
        g.pose().translate(INFO_X, INFO_Y, 0);

        Component genValue = gen > 0
                ? Component.literal(formatFE(gen) + " FE/t").withStyle(s -> s.withColor(ENERGY_BLUE))
                : Component.literal("0 FE/t").withStyle(s -> s.withColor(TEXT_DIM));
        Component genLine = Component.translatable("gui.industrial_core.panel_combiner.generation")
                .append(Component.literal(": ").withStyle(s -> s.withColor(TEXT_BRIGHT)))
                .append(genValue);
        g.drawString(font, genLine, 0, 0, TEXT_BRIGHT, false);

        Component bufLine = Component.translatable("gui.industrial_core.panel_combiner.buffer")
                .append(Component.literal(": ").withStyle(s -> s.withColor(TEXT_BRIGHT)))
                .append(Component.literal(formatFE(menu.getEnergyStored()) + " / " + formatFE(menu.getMaxEnergy()) + " FE")
                        .withStyle(s -> s.withColor(ENERGY_BLUE)));
        g.drawString(font, bufLine, 0, LINE, TEXT_BRIGHT, false);

        Component outValue = out > 0
                ? Component.literal(formatFE(out) + " FE/t").withStyle(s -> s.withColor(ENERGY_BLUE))
                : Component.literal("0 FE/t").withStyle(s -> s.withColor(TEXT_DIM));
        Component outLine = Component.translatable("gui.industrial_core.panel_combiner.output")
                .append(Component.literal(": ").withStyle(s -> s.withColor(TEXT_BRIGHT)))
                .append(outValue);
        g.drawString(font, outLine, 0, 2 * LINE, TEXT_BRIGHT, false);

        g.pose().popPose();
    }

    private static String formatFE(long fe) {
        if (fe >= 1_000_000_000L) return String.format("%.2fB", fe / 1_000_000_000.0);
        if (fe >= 1_000_000L)     return String.format("%.1fM", fe / 1_000_000.0);
        if (fe >= 1_000L)         return String.format("%.1fk", fe / 1_000.0);
        return String.valueOf(fe);
    }
}
