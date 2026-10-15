package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.EnergyBankMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public class EnergyBankScreen extends MachineScreen<EnergyBankMenu> {
    private static final ResourceLocation TEXTURE     = new ResourceLocation(IndustrialCore.MODID, "textures/gui/energy_bank_gui.png");

    private static final int BAR_X = 92, BAR_Y = 87, BAR_W = 75, BAR_H = 6;   // energy bar
    private static final int INFO_X = 76, INFO_Y = 41;                        // info field (107x43)
    private static final int RES_X = 169, RES_Y = 86, RES_W = 14, RES_H = 8;  // reserve field

    private static final int C_ENERGY = 0xFF00FFCC, C_LABEL = 0xFF9AA0A6, C_IN = 0xFF6BE06B, C_OUT = 0xFFFF7A6B, C_VALUE = 0xFFE0E0E0;

    private EditBox reserveField;

    public EnergyBankScreen(EnergyBankMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 6);
    }

    @Override protected void init() {
        super.init();
        reserveField = new EditBox(this.font, leftPos + RES_X + 1, topPos + RES_Y, RES_W - 1, RES_H, Component.empty());
        reserveField.setMaxLength(3);
        reserveField.setBordered(false);      // the frame is the drawn one, EditBox only draws the text
        reserveField.setTextColor(0xFFFFFF);
        reserveField.setFilter(s -> s.matches("\\d{0,3}"));
        reserveField.setValue(String.valueOf(menu.getReserve()));
        addRenderableWidget(reserveField);
    }

    // While the player is not editing the field, keep it in sync with the server.
    // Otherwise on reopening the GUI the field gets stuck at the pre-sync 0 ("the reserve drops").
    @Override
    public void containerTick() {
        super.containerTick();
        if (reserveField != null && !reserveField.isFocused())
            reserveField.setValue(String.valueOf(menu.getReserve()));
    }

    private void commitReserve() {
        int v = 0; try { v = Integer.parseInt(reserveField.getValue()); } catch (Exception ignored) {}
        v = Mth.clamp(v, 0, 100);
        reserveField.setValue(String.valueOf(v));
        if (this.minecraft != null && this.minecraft.gameMode != null)
            this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, v);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (reserveField.isFocused()) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { commitReserve(); reserveField.setFocused(false); return true; }
            if (reserveField.keyPressed(key, scan, mods) || key != GLFW.GLFW_KEY_ESCAPE) return true; // swallow keys (including 'e', otherwise the GUI closes)
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (reserveField.isFocused() && reserveField.charTyped(c, mods)) return true;
        return super.charTyped(c, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        boolean was = reserveField.isFocused();
        boolean r = super.mouseClicked(mx, my, btn);
        if (was && !reserveField.isFocused()) commitReserve(); // unfocus on click: commit
        return r;
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mx, int my) {
        Bars.energy(g, font, mx, my, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H,
                menu.getEnergy(), menu.getMaxEnergy());
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        bars.draw(g, FillBar.ENERGY, x + BAR_X, y + BAR_Y, menu.getScaledEnergy(BAR_W), BAR_H);
        drawInfo(g, x, y);
    }

    private void drawInfo(GuiGraphics g, int x, int y) {
        int max = menu.getMaxEnergy(), e = menu.getEnergy();
        int pct = max > 0 ? (int) ((long) e * 100 / max) : 0;
        int bx = x + INFO_X + 3, yy = y + INFO_Y + 3;

        drawScaled(g, tr("stored"), bx, yy, C_LABEL, 0.8f);
        drawScaled(g, fmtFE(e) + " / " + fmtFE(max), bx, yy + 8, C_ENERGY, 0.8f);
        drawScaled(g, pct + "%", bx, yy + 16, C_VALUE, 0.8f);
        drawScaled(g, tr("in")  + " +" + fmtFE(menu.getIoIn())  + "/t", bx, yy + 24, C_IN,  0.8f);
        drawScaled(g, tr("out") + " -" + fmtFE(menu.getIoOut()) + "/t", bx, yy + 32, C_OUT, 0.8f);

        // "%" right after the reserve field
        drawScaled(g, "%", x + RES_X + RES_W, y + RES_Y + 1, C_LABEL, 0.75f);
    }

    private void drawScaled(GuiGraphics g, String txt, float x, float y, int col, float s) {
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(s, s, 1f);
        g.drawString(this.font, txt, 0, 0, col, false);
        g.pose().popPose();
    }

    private static String tr(String k) { return Component.translatable("gui.industrial_core.energy_bank." + k).getString(); }
    private static String fmtFE(int v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000f);
        if (v >= 1_000)     return String.format("%.1fk", v / 1_000f);
        return String.valueOf(v);
    }
}
