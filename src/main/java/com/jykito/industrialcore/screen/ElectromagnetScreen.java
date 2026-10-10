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
    private static final ResourceLocation TEXTURE = new ResourceLocation(IndustrialCore.MODID, "textures/gui/electromagnet_gui.png");

    /** Энергия — его полоса с молнией слева, как у остальных машин (tools/gui_gen/mag_cryo/build.js, там те же координаты). */
    private static final int BAR_X = 73, BAR_Y = 122, BAR_H = 6;

    private static final int TANK_W = 16, TANK_H = 64;
    private static final int TANK_A_X = 54,  TANK_Y = 32;
    private static final int TANK_B_X = 186;
    private static final int GLASS_U = 34, GLASS_V = 242;

    // Инфо-поля: слева нестабильность, справа статус+пучки. CRIT = порог «вот-вот квенч».
    private static final int CRIT = 80;
    private static final int INFO_L_X = 23, INFO_R_X = 204, INFO_Y = 27;
    private static final float TXT_SCALE = 0.5f; // мельче основного шрифта — влезть в инфо-поля

    // Резонанс/мощность. Дорожки нарисованы в текстуре; код рисует спрайты-ползунки + иконку цели + аним-заливку.
    private static final int RES_X = 73, RES_Y = 102, RES_W = 110, RES_H = 6;   // бар резонанса (окно-ползунок ловит иконку цели)
    private static final int THR_X = 73, THR_Y = 112, THR_W = 110, THR_H = 6;   // бар мощности (аним-заливка + ползунок)
    // Спрайты в основной текстуре (UV): ползунок резонанса 96,264 (5×6, окно 3×4); иконка цели 133,263 (3×6); ползунок мощности 74,264 (5×6).
    private static final ResourceLocation THROTTLE_FILL = new ResourceLocation(IndustrialCore.MODID, "textures/gui/trottling_bar_anim.png");
    private static final int THR_FILL_H = 6, THR_ANIM_H = 60, THR_FRAMES = 10;  // аним-заливка мощности 110×60 = 10 кадров по 6px
    // Прогресс: аним-форма юзера (стрип 64×1152 = 18 кадров 64×64), кадр выбирается по прогрессу, центр на выходе (128,60)
    private static final ResourceLocation PROGRESS_ANIM = new ResourceLocation(IndustrialCore.MODID, "textures/gui/progress_bar_magnet_anim.png");
    private static final int PROG_FRAME = 64, PROG_FRAMES = 18, PROG_PX = 97, PROG_PY = 29;
    private int thrAnimFrame = 0;
    private float throttleLocal = 0f;         // клиентское значение мощности (мгновенный отклик при перетаскивании)
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
        if (!draggingThrottle) throttleLocal = menu.getThrottle() / 100f; // синк с сервера, пока не тащим ползунок
        drawResonance(g);
        drawThrottle(g);
        drawProgress(g);
    }

    /** Инфо-панели: слева нестабильность (крупно) + радиус поля; справа статус + множитель ×1/×2 крупно по центру. */
    private void drawReadout(GuiGraphics g) {
        int instab = menu.getInstability();
        boolean working = menu.isWorking();
        boolean doubling = menu.isDoubling();
        int lcx = leftPos + 37, rcx = leftPos + 218, ty = topPos;   // центры лев./прав. панелей (сдвиг ±15 от кольца)
        int col = instab >= CRIT ? 0xFF5555 : instab >= 50 ? 0xFFD24A : 0x55FF7A;

        // ── Левая панель: нестабильность (крупно) + радиус поля ──
        drawScaled(g, Component.translatable("gui.industrial_core.magnet.instab"), lcx, ty + 34, 0xA0A0A0, 0.72f);
        drawScaled(g, Component.literal(instab + "%"), lcx, ty + 45, col, 1.2f);
        int tier = menu.getCoilTier();
        String fr = tier > 0
                ? String.format(java.util.Locale.ROOT, "%.1f",
                        com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity.fieldRadius(instab, tier))
                : "—";
        drawScaled(g, Component.translatable("gui.industrial_core.magnet.field", fr), lcx, ty + 68, col, 0.72f);

        // ── Правая панель: статус + множитель ×1/×2 (крупно, в центре) ──
        Component status = instab >= CRIT ? Component.translatable("gui.industrial_core.magnet.critical")
                : working ? Component.translatable("gui.industrial_core.magnet.working")
                : Component.translatable("gui.industrial_core.magnet.idle");
        int scol = instab >= CRIT ? 0xFF5555 : working ? 0x55FF7A : 0xA0A0A0;
        drawScaled(g, status, rcx, ty + 33, scol, 0.72f);
        drawScaled(g, Component.literal(doubling ? "×2" : "×1"), rcx, ty + 48, doubling ? 0x55FF7A : 0xA0A0A0, 1.7f);
    }

    /** Текст с масштабом, центрированный по X вокруг (cx); cy — верх строки. */
    private void drawScaled(GuiGraphics g, Component text, int cx, int cy, int color, float scale) {
        float w = font.width(text) * scale;
        g.pose().pushPose();
        g.pose().translate(cx - w / 2f, cy, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Бар резонанса: иконка дрейф-цели + окно-ползунок нестабильности (ловим цель в окно). Цель зеленеет по заряду. */
    private void drawResonance(GuiGraphics g) {
        int x = leftPos + RES_X, y = topPos + RES_Y;
        int instab = menu.getInstability();
        int target = menu.getResonanceTarget();
        int charge = menu.getResonanceCharge();
        boolean caught = target > 0 && Math.abs(instab - target) <= 3;                    // поймал цель в окно (±3%)
        if (target > 0) {                                                                 // иконку цели рисуем ПОД ползунком (видна в окне 3×4)
            int tx = Math.max(x, Math.min(x + Math.round(target / 100f * RES_W) - 1, x + RES_W - 3)); // кламп в границы бара
            if (caught) { float k = charge / 100f; g.setColor(1f - 0.55f * k, 1f, 1f - 0.55f * k, 1f); } // белый → зелёный по заряду
            g.blit(TEXTURE, tx, y, 133, 263, 3, 6, 256, 315);
            g.setColor(1f, 1f, 1f, 1f);
        }
        int hx = Math.max(x, Math.min(x + Math.round(instab / 100f * RES_W) - 2, x + RES_W - 5)); // окно-ползунок, клампим в бар
        g.blit(TEXTURE, hx, y, 96, 264, 5, 6, 256, 315);
    }

    /** Бар мощности: аним-заливка от левого края до ползунка + спрайт-ползунок (перетаскивается мышью → пакет на сервер). */
    private void drawThrottle(GuiGraphics g) {
        int x = leftPos + THR_X, y = topPos + THR_Y;
        int fillW = Math.round(throttleLocal * THR_W);
        if (fillW > 0)                                                                    // аним-заливка (110×60, 10 кадров)
            g.blit(THROTTLE_FILL, x, y, 0, thrAnimFrame * THR_FILL_H, fillW, THR_H, THR_W, THR_ANIM_H);
        int hx = Math.max(x, Math.min(x + fillW - 2, x + THR_W - 5));                     // ползунок на конце заливки, клампим в бар
        g.blit(TEXTURE, hx, y, 74, 264, 5, 6, 256, 315);
    }

    /** Прогресс крафта: кадр аним-формы (стрип юзера) по доле прогресса, по центру слота-выхода. */
    private void drawProgress(GuiGraphics g) {
        int max = menu.getMaxProgress();
        if (max <= 1) return;                                                             // только при активном крафте
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
