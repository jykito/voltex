package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity;
import com.jykito.industrialcore.menu.ElectromagnetMenu;
import com.jykito.industrialcore.networking.MagnetThrottlePacket;
import com.jykito.industrialcore.networking.ModMessages;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ElectromagnetScreen extends MachineScreen<ElectromagnetMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/electromagnet_gui.png");

    /** Energy: the author's bar with a bolt on the left, like the other machines (tools/gui_gen/mag_cryo/build.js has the same coordinates). */
    private static final int BAR_X = 73, BAR_Y = 122, BAR_H = 6;

    private static final int TANK_W = 16, TANK_H = 64;
    private static final int TANK_A_X = 54,  TANK_Y = 32;
    private static final int TANK_B_X = 186;
    private static final int GLASS_U = 34, GLASS_V = 242;

    // Info fields: instability on the left, status + beams on the right. CRIT = the "about to quench" threshold.
    private static final int CRIT = 80;
    private static final int INFO_L_X = 23, INFO_R_X = 204, INFO_Y = 27;
    private static final float TXT_SCALE = 0.5f; // smaller than the main font, to fit into the info fields

    // Resonance/power. The tracks are drawn in the texture; the code draws the slider sprites + the target icon + the animated fill.
    private static final int RES_X = 73, RES_Y = 102, RES_W = 110, RES_H = 6;   // resonance bar (the window slider catches the target icon)
    private static final int THR_X = 73, THR_Y = 112, THR_W = 110, THR_H = 6;   // power bar (animated fill + slider)
    // Sprites in the main texture (UV): resonance slider 96,264 (5x6, window 3x4); target icon 133,263 (3x6); power slider 74,264 (5x6).
    private static final ResourceLocation THROTTLE_FILL = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/trottling_bar_anim.png");
    private static final int THR_FILL_H = 6, THR_ANIM_H = 60, THR_FRAMES = 10;  // animated power fill 110x60 = 10 frames of 6px
    // Progress: the author's animated shape (strip 64x1152 = 18 frames of 64x64), the frame is picked by progress, centred on the output (128,60)
    private static final ResourceLocation PROGRESS_ANIM = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "textures/gui/progress_bar_magnet_anim.png");
    private static final int PROG_FRAME = 64, PROG_FRAMES = 18, PROG_PX = 97, PROG_PY = 29;
    private int thrAnimFrame = 0;
    private float throttleLocal = 0f;         // client power value (instant response while dragging)
    private boolean draggingThrottle = false;

    public ElectromagnetScreen(ElectromagnetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 234;
    }

    @Override
    protected void onBarFrame() {
        thrAnimFrame = (thrAnimFrame + 1) % THR_FRAMES;
    }

    @Override
    protected void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
        ElectromagnetCoreBlockEntity be = menu.blockEntity;
        FluidGauge.tooltip(g, font, mouseX, mouseY, leftPos + TANK_A_X, topPos + TANK_Y, TANK_W, TANK_H, be.tankA.getFluid(), be.tankA.getCapacity());
        FluidGauge.tooltip(g, font, mouseX, mouseY, leftPos + TANK_B_X, topPos + TANK_Y, TANK_W, TANK_H, be.tankB.getFluid(), be.tankB.getCapacity());
        if (isHover(mouseX, mouseY, RES_X, RES_Y - 1, RES_W, RES_H + 4))
            g.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("gui.industrial_core.magnet.resonance"),
                    Component.translatable("gui.industrial_core.magnet.resonance_info", menu.getResonanceTarget(), menu.getResonanceCharge())),
                    mouseX, mouseY);
        else if (isHover(mouseX, mouseY, THR_X, THR_Y - 2, THR_W, THR_H + 4))
            g.renderTooltip(font, Component.translatable("gui.industrial_core.magnet.throttle_info", Math.round(throttleLocal * 100f)), mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 315);

        bars.draw(g, FillBar.ENERGY, leftPos + BAR_X, topPos + BAR_Y, menu.getScaledEnergy(), BAR_H);

        ElectromagnetCoreBlockEntity be = menu.blockEntity;
        FluidGauge.draw(g, be.tankA.getFluid(), be.tankA.getCapacity(), leftPos + TANK_A_X, topPos + TANK_Y, TANK_W, TANK_H);
        FluidGauge.draw(g, be.tankB.getFluid(), be.tankB.getCapacity(), leftPos + TANK_B_X, topPos + TANK_Y, TANK_W, TANK_H);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, leftPos + TANK_A_X, topPos + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, 256, 315);
        g.blit(TEXTURE, leftPos + TANK_B_X, topPos + TANK_Y, GLASS_U, GLASS_V, TANK_W, TANK_H, 256, 315);
        RenderSystem.disableBlend();

        drawReadout(g);
        if (!draggingThrottle) throttleLocal = menu.getThrottle() / 100f; // sync from the server while the slider is not being dragged
        drawResonance(g);
        drawThrottle(g);
        drawProgress(g);
    }

    /** Info panels: instability (large) + field radius on the left; status + the x1/x2 multiplier large in the centre on the right. */
    private void drawReadout(GuiGraphics g) {
        int instab = menu.getInstability();
        boolean working = menu.isWorking();
        boolean doubling = menu.isDoubling();
        int lcx = leftPos + 37, rcx = leftPos + 218, ty = topPos;   // centres of the left/right panels (offset ±15 from the ring)
        int col = instab >= CRIT ? 0xFF5555 : instab >= 50 ? 0xFFD24A : 0x55FF7A;

        // ── Left panel: instability (large) + field radius ──
        drawScaled(g, Component.translatable("gui.industrial_core.magnet.instab"), lcx, ty + 34, 0xA0A0A0, 0.72f);
        drawScaled(g, Component.literal(instab + "%"), lcx, ty + 45, col, 1.2f);
        int tier = menu.getCoilTier();
        String fr = tier > 0
                ? String.format(java.util.Locale.ROOT, "%.1f",
                        com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity.fieldRadius(instab, tier))
                : "—";
        drawScaled(g, Component.translatable("gui.industrial_core.magnet.field", fr), lcx, ty + 68, col, 0.72f);

        // ── Right panel: status + x1/x2 multiplier (large, centred) ──
        Component status = instab >= CRIT ? Component.translatable("gui.industrial_core.magnet.critical")
                : working ? Component.translatable("gui.industrial_core.magnet.working")
                : Component.translatable("gui.industrial_core.magnet.idle");
        int scol = instab >= CRIT ? 0xFF5555 : working ? 0x55FF7A : 0xA0A0A0;
        drawScaled(g, status, rcx, ty + 33, scol, 0.72f);
        drawScaled(g, Component.literal(doubling ? "×2" : "×1"), rcx, ty + 48, doubling ? 0x55FF7A : 0xA0A0A0, 1.7f);
    }

    /** Scaled text centred on X around (cx); cy is the top of the line. */
    private void drawScaled(GuiGraphics g, Component text, int cx, int cy, int color, float scale) {
        float w = font.width(text) * scale;
        g.pose().pushPose();
        g.pose().translate(cx - w / 2f, cy, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Resonance bar: drifting target icon + instability window slider (catch the target in the window). The target turns green with charge. */
    private void drawResonance(GuiGraphics g) {
        int x = leftPos + RES_X, y = topPos + RES_Y;
        int instab = menu.getInstability();
        int target = menu.getResonanceTarget();
        int charge = menu.getResonanceCharge();
        boolean caught = target > 0 && Math.abs(instab - target) <= 3;                    // caught the target in the window (±3%)
        if (target > 0) {                                                                 // the target icon is drawn UNDER the slider (visible in the 3x4 window)
            int tx = Math.max(x, Math.min(x + Math.round(target / 100f * RES_W) - 1, x + RES_W - 3)); // clamp to the bar bounds
            if (caught) { float k = charge / 100f; g.setColor(1f - 0.55f * k, 1f, 1f - 0.55f * k, 1f); } // white -> green by charge
            g.blit(TEXTURE, tx, y, 133, 263, 3, 6, 256, 315);
            g.setColor(1f, 1f, 1f, 1f);
        }
        int hx = Math.max(x, Math.min(x + Math.round(instab / 100f * RES_W) - 2, x + RES_W - 5)); // window slider, clamped to the bar
        g.blit(TEXTURE, hx, y, 96, 264, 5, 6, 256, 315);
    }

    /** Power bar: animated fill from the left edge to the slider + a slider sprite (dragged with the mouse -> packet to the server). */
    private void drawThrottle(GuiGraphics g) {
        int x = leftPos + THR_X, y = topPos + THR_Y;
        int fillW = Math.round(throttleLocal * THR_W);
        if (fillW > 0)                                                                    // animated fill (110x60, 10 frames)
            g.blit(THROTTLE_FILL, x, y, 0, thrAnimFrame * THR_FILL_H, fillW, THR_H, THR_W, THR_ANIM_H);
        int hx = Math.max(x, Math.min(x + fillW - 2, x + THR_W - 5));                     // slider at the end of the fill, clamped to the bar
        g.blit(TEXTURE, hx, y, 74, 264, 5, 6, 256, 315);
    }

    /** Craft progress: a frame of the animated shape (the author's strip) by progress fraction, centred on the output slot. */
    private void drawProgress(GuiGraphics g) {
        int max = menu.getMaxProgress();
        if (max <= 1) return;                                                             // only while crafting
        float p = Math.max(0f, Math.min(1f, menu.getProgress() / (float) max));
        int frame = Math.min(PROG_FRAMES - 1, Math.round(p * (PROG_FRAMES - 1)));
        g.blit(PROGRESS_ANIM, leftPos + PROG_PX, topPos + PROG_PY, 0, frame * PROG_FRAME,
                PROG_FRAME, PROG_FRAME, PROG_FRAME, PROG_FRAME * PROG_FRAMES);
    }

    private boolean isHover(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= leftPos + rx && mx < leftPos + rx + rw && my >= topPos + ry && my < topPos + ry + rh;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && overThrottle(mx, my)) { draggingThrottle = true; setThrottleFromMouse(mx); return true; }
        return super.mouseClicked(mx, my, button);
    }
    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingThrottle && button == 0) { setThrottleFromMouse(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }
    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (draggingThrottle && button == 0) { draggingThrottle = false; return true; }
        return super.mouseReleased(mx, my, button);
    }
    private boolean overThrottle(double mx, double my) {
        int x = leftPos + THR_X, y = topPos + THR_Y;
        return mx >= x - 2 && mx <= x + THR_W + 2 && my >= y - 3 && my <= y + THR_H + 3;
    }
    private void setThrottleFromMouse(double mx) {
        float t = (float) ((mx - (leftPos + THR_X)) / THR_W);
        t = Math.max(0f, Math.min(1f, t));
        if (Math.abs(t - throttleLocal) < 0.005f) return;
        throttleLocal = t;
        ModMessages.sendToServer(new MagnetThrottlePacket(menu.blockEntity.getBlockPos(), t));
    }

}
