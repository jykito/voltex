package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.ThermostationBlockEntity;
import com.jykito.industrialcore.menu.ThermostationMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ThermostationScreen extends MachineScreen<ThermostationMenu> {
    private static final ResourceLocation TEXTURE     = new ResourceLocation(IndustrialCore.MODID, "textures/gui/thermostation_gui.png");

    // бары (dest в GUI)
    private static final int ENERGY_X = 103, ENERGY_Y = 38, ENERGY_W = 50, ENERGY_H = 6;
    private static final int HEAT_X   = 103, HEAT_Y   = 47, HEAT_W   = 50, HEAT_H   = 6;
    // "работает" лит-оверлей (source в текстуре)
    private static final int WORK_X = 106, WORK_Y = 57, WORK_W = 43, WORK_H = 26, WORK_U = 141, WORK_V = 253;
    // инфо-поля
    private static final int GEN_X = 102, GEN_Y = 85, GEN_W = 50, GEN_H = 21;
    private static final int INFO_X = 50, INFO_Y = 40;

    private static final int C_HEAT = 0xFFFF6A3D, C_ENERGY = 0xFF00FFCC, C_LABEL = 0xFF9AA0A6, C_VALUE = 0xFFE0E0E0;

    public ThermostationScreen(ThermostationMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 8);
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        Bars.energy(g, font, mx, my, leftPos + ENERGY_X, topPos + ENERGY_Y, ENERGY_W, ENERGY_H,
                menu.getEnergy(), com.jykito.industrialcore.block.entity.ThermostationBlockEntity.ENERGY_CAP);
        Bars.heat(g, font, mx, my, leftPos + HEAT_X, topPos + HEAT_Y, HEAT_W, HEAT_H,
                menu.getHeat(), menu.getMaxHeat());
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        int ef = menu.getScaledEnergy(ENERGY_W);
        bars.draw(g, FillBar.ENERGY, x + ENERGY_X, y + ENERGY_Y, ef, ENERGY_H);
        int hf = menu.getScaledHeat(HEAT_W);
        bars.draw(g, FillBar.HEAT, x + HEAT_X, y + HEAT_Y, hf, HEAT_H);

        if (menu.isWorking())
            g.blit(TEXTURE, x + WORK_X, y + WORK_Y, WORK_U, WORK_V, WORK_W, WORK_H, TEX_W, TEX_H);

        drawInfo(g, x, y);
    }

    private void drawInfo(GuiGraphics g, int x, int y) {
        // малое поле — генерация тепла/тик
        String gen = menu.getGenRate() + "/t";
        drawCentered(g, gen, x + GEN_X + GEN_W / 2f, y + GEN_Y + (GEN_H - 8) / 2f, C_HEAT, 1.0f);

        // большое поле — подробности
        int max = menu.getMaxHeat();
        int pct = max > 0 ? menu.getHeat() * 100 / max : 0;
        int bx = x + INFO_X + 3, yy = y + INFO_Y + 3;
        yy = stat(g, tr("heat"),       pct + "%",                  C_HEAT,   bx, yy);
        yy = stat(g, tr("energy"),     fmtK(menu.getEnergy()),     C_ENERGY, bx, yy);
        yy = stat(g, tr("throughput"), menu.getMaxOutput() + "/t", C_VALUE,  bx, yy);
        boolean full = max > 0 && menu.getHeat() >= max;
        String st = menu.isWorking() ? tr("working")
                : menu.getEnergy() < 5 ? tr("nopower")
                : full ? tr("full") : tr("idle");
        int stCol = menu.isWorking() ? 0xFF55FF7A : full ? 0xFFFFD24A : 0xFF9AA0A6;
        drawScaled(g, tr("status"), bx, yy, C_LABEL, 0.7f);
        drawScaled(g, st, bx, yy + 7, stCol, 0.85f);
    }

    private int stat(GuiGraphics g, String label, String val, int valCol, int x, int y) {
        drawScaled(g, label, x, y, C_LABEL, 0.7f);
        drawScaled(g, val, x, y + 7, valCol, 0.85f);
        return y + 15;
    }

    private void drawScaled(GuiGraphics g, String txt, float x, float y, int col, float s) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(s, s, 1f);
        g.drawString(this.font, txt, 0, 0, col, false);
        g.pose().popPose();
    }

    private void drawCentered(GuiGraphics g, String txt, float cx, float cy, int col, float s) {
        float w = this.font.width(txt) * s;
        drawScaled(g, txt, cx - w / 2f, cy, col, s);
    }

    private static String tr(String key) { return Component.translatable("gui.industrial_core.thermostation." + key).getString(); }
    private static String fmtK(int v) { return v >= 1000 ? String.format("%.1fk", v / 1000f) : String.valueOf(v); }
}
