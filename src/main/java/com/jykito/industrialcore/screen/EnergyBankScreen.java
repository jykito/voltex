package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.menu.EnergyBankMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public class EnergyBankScreen extends AbstractContainerScreen<EnergyBankMenu> {
    private static final ResourceLocation TEXTURE     = new ResourceLocation(IndustrialCore.MODID, "textures/gui/energy_bank_gui.png");
    private static final ResourceLocation ENERGY_FILL = new ResourceLocation(IndustrialCore.MODID, "textures/gui/energy_fill_6x_anim.png");
    private static final int TEX_W = 256, TEX_H = 315;
    private static final int FILL_W = 96, ANIM_H = 36, FRAME_H = 6, FRAMES = 6, FRAME_TICKS = 6;
    private int animTick = 0, frame = 0;

    private static final int BAR_X = 92, BAR_Y = 87, BAR_W = 75, BAR_H = 6;   // энергобар
    private static final int INFO_X = 76, INFO_Y = 41;                        // инфо-поле (107×43)
    private static final int RES_X = 169, RES_Y = 86, RES_W = 14, RES_H = 8;  // поле резерва

    private static final int C_ENERGY = 0xFF00FFCC, C_LABEL = 0xFF9AA0A6, C_IN = 0xFF6BE06B, C_OUT = 0xFFFF7A6B, C_VALUE = 0xFFE0E0E0;

    private EditBox reserveField;

    public EnergyBankScreen(EnergyBankMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256; this.imageHeight = 224;
    }

    @Override protected void init() {
        super.init();
        this.inventoryLabelY = 10000; this.titleLabelY = 10000;
        reserveField = new EditBox(this.font, leftPos + RES_X + 1, topPos + RES_Y, RES_W - 1, RES_H, Component.empty());
        reserveField.setMaxLength(3);
        reserveField.setBordered(false);      // рамка — твоя нарисованная, EditBox рисует только текст
        reserveField.setTextColor(0xFFFFFF);
        reserveField.setFilter(s -> s.matches("\\d{0,3}"));
        reserveField.setValue(String.valueOf(menu.getReserve()));
        addRenderableWidget(reserveField);
    }

    // Пока игрок не редактирует поле — держим его в синке с сервером.
    // Иначе на реоткрытии GUI поле застревает на до-синковом 0 («резерв спадает»).
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
            if (reserveField.keyPressed(key, scan, mods) || key != GLFW.GLFW_KEY_ESCAPE) return true; // едим клавиши (в т.ч. 'e', иначе GUI закроется)
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
        if (was && !reserveField.isFocused()) commitReserve(); // расфокус кликом — коммит
        return r;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        if (++animTick >= FRAME_TICKS) { animTick = 0; frame = (frame + 1) % FRAMES; }
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        Bars.energy(g, font, mx, my, leftPos + BAR_X, topPos + BAR_Y, BAR_W, BAR_H,
                menu.getEnergy(), menu.getMaxEnergy());
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, TEX_W, TEX_H);

        int filled = menu.getScaledEnergy(BAR_W);
        if (filled > 0) {
            int bx = x + BAR_X, by = y + BAR_Y, v = frame * FRAME_H, dx = bx;
            while (dx < bx + filled) {
                int tw = Math.min(FILL_W, bx + filled - dx);
                g.blit(ENERGY_FILL, dx, by, 0, v, tw, BAR_H, FILL_W, ANIM_H);
                dx += FILL_W;
            }
        }
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

        // "%" сразу после поля резерва
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
