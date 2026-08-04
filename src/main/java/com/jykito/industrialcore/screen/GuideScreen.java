package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Нативный гайд-экран Voltex (замена книжки-гайда). Панель по центру: слева навигация
 * (категория -> запись), справа контент из типизированных блоков (см. {@link GuideContent}).
 * Язык следует за интерфейсом. Подсветка кнопок и скроллбар рисуются кодом (fill), не текстурой.
 */
public class GuideScreen extends Screen {
    private static final ResourceLocation TEX =
            new ResourceLocation(IndustrialCore.MODID, "textures/gui/information_panel.png");
    private static final int TEX_W = 315, TEX_H = 512;   // файл
    private static final int GUI_W = 315, GUI_H = 256;   // отображаемая часть

    // регионы (относительно leftPos/topPos) — из координат юзера
    private static final int HEAD_X = 1,   HEAD_Y = 1,  NAV_X = 1,   NAV_Y = 19, NAV_W = 110, NAV_H = 236;
    private static final int CT_X   = 112, CT_Y   = 19, CT_W  = 202, CT_H  = 236;

    private static final int ROW_H = 13;
    private static final int TEXT  = 0xFFE6EDF3, SUB = 0xFFAEB6BF, DIM = 0xFF8B949E, ACCENT = 0xFF00FFCC;

    private int leftPos, topPos;
    private final List<GuideContent.Category> cats;
    private int catI = 0, entI = 0;
    private int scroll = 0, contentH = 0;
    private int navScroll = 0, navContentH = 0;
    private final List<int[]> navHits = new ArrayList<>(); // {x, y, w, h, ci, ei(-1=категория)}

    public GuideScreen() {
        super(Component.translatable("gui.industrial_core.guide.title"));
        this.cats = GuideContent.load();
    }

    @Override protected void init() {
        this.leftPos = (this.width - GUI_W) / 2;
        this.topPos  = (this.height - GUI_H) / 2;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.blit(TEX, leftPos, topPos, 0, 0, GUI_W, GUI_H, TEX_W, TEX_H);
        g.drawString(font, getTitle(), leftPos + HEAD_X + 6, topPos + HEAD_Y + 5, TEXT, false);
        renderNav(g, mx, my);
        renderContent(g, mx, my);
        super.render(g, mx, my, pt);
        GuideContent.Entry e = current();
        if (e != null) for (GuideContent.Element el : e.elements)
            if (el instanceof GuideContent.Multiblock mb) {
                ItemStack hov = mb.materialAt(mx, my);
                if (hov != null && !hov.isEmpty()) { g.renderTooltip(font, hov, mx, my); break; }
            }
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void renderNav(GuiGraphics g, int mx, int my) {
        navHits.clear();
        int x = leftPos + NAV_X, top = topPos + NAV_Y;
        int startY = top + 4 - navScroll, y = startY;
        boolean overNav = in(mx, my, x, top, NAV_W, NAV_H);
        g.enableScissor(x, top, x + NAV_W, top + NAV_H);
        for (int ci = 0; ci < cats.size(); ci++) {
            GuideContent.Category c = cats.get(ci);
            boolean vis = y + ROW_H > top && y < top + NAV_H;                 // строка в видимой полосе
            boolean act = ci == catI, hv = vis && overNav && in(mx, my, x, y, NAV_W, ROW_H);
            if (act)      g.fill(x, y, x + NAV_W, y + ROW_H, 0x2000FFCC);
            else if (hv)  g.fill(x, y, x + NAV_W, y + ROW_H, 0x18FFFFFF);
            g.drawString(font, c.name, x + 6, y + 3, act ? ACCENT : TEXT, false);
            if (vis) navHits.add(new int[]{x, y, NAV_W, ROW_H, ci, -1});
            y += ROW_H;
            if (act) {
                for (int ei = 0; ei < c.entries.size(); ei++) {
                    boolean evis = y + ROW_H > top && y < top + NAV_H;
                    boolean ea = ei == entI, eh = evis && overNav && in(mx, my, x, y, NAV_W, ROW_H);
                    if (ea)      { g.fill(x, y, x + NAV_W, y + ROW_H, 0x2000FFCC); g.fill(x, y, x + 2, y + ROW_H, ACCENT); }
                    else if (eh) g.fill(x, y, x + NAV_W, y + ROW_H, 0x14FFFFFF);
                    g.drawString(font, c.entries.get(ei).name, x + 12, y + 3, ea ? ACCENT : (eh ? SUB : DIM), false);
                    if (evis) navHits.add(new int[]{x, y, NAV_W, ROW_H, ci, ei});
                    y += ROW_H;
                }
            }
        }
        navContentH = y - startY;
        g.disableScissor();

        int maxNav = Math.max(0, navContentH - NAV_H);
        if (maxNav > 0) {
            int tx = x + NAV_W - 2, tTop = top + 2, tH = NAV_H - 4;
            int thumbH = Math.max(14, tH * NAV_H / navContentH);
            int thumbY = tTop + (tH - thumbH) * navScroll / maxNav;
            g.fill(tx, thumbY, tx + 2, thumbY + thumbH, 0x6000FFCC);
        }
    }

    private void renderContent(GuiGraphics g, int mx, int my) {
        int x = leftPos + CT_X + 8, top = topPos + CT_Y + 8, w = CT_W - 16, inner = CT_H - 16;
        g.enableScissor(leftPos + CT_X, topPos + CT_Y, leftPos + CT_X + CT_W, topPos + CT_Y + CT_H);
        int startY = top - scroll, y = startY;
        GuideContent.Entry e = current();
        if (e != null) for (GuideContent.Element el : e.elements) y += el.render(g, font, x, y, w) + 6;
        contentH = y - startY;
        g.disableScissor();

        int maxScroll = Math.max(0, contentH - inner);
        if (maxScroll > 0) {
            int tx = leftPos + CT_X + CT_W - 3, tTop = topPos + CT_Y + 2, tH = CT_H - 4;
            int thumbH = Math.max(16, tH * inner / contentH);
            int thumbY = tTop + (tH - thumbH) * scroll / maxScroll;
            g.fill(tx, thumbY, tx + 2, thumbY + thumbH, 0x6000FFCC);
        }
    }

    private GuideContent.Entry current() {
        if (cats.isEmpty()) return null;
        catI = Mth.clamp(catI, 0, cats.size() - 1);
        List<GuideContent.Entry> es = cats.get(catI).entries;
        if (es.isEmpty()) return null;
        entI = Mth.clamp(entI, 0, es.size() - 1);
        return es.get(entI);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        GuideContent.Entry ce = current();
        if (ce != null) for (GuideContent.Element el : ce.elements)
            if (el instanceof GuideContent.Multiblock mb && mb.hitButton((int) mx, (int) my)) { mb.step(); return true; }
        if (in((int) mx, (int) my, leftPos + NAV_X, topPos + NAV_Y, NAV_W, NAV_H))
            for (int[] h : navHits) {
                if (mx >= h[0] && mx < h[0] + h[2] && my >= h[1] && my < h[1] + h[3]) {
                    if (h[5] == -1) { if (catI != h[4]) { catI = h[4]; entI = 0; scroll = 0; } }
                    else { catI = h[4]; entI = h[5]; scroll = 0; }
                    return true;
                }
            }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (in((int) mx, (int) my, leftPos + NAV_X, topPos + NAV_Y, NAV_W, NAV_H)) {
            int maxNav = Math.max(0, navContentH - NAV_H);
            navScroll = Mth.clamp(navScroll - (int) (delta * 14), 0, maxNav);
            return true;
        }
        if (in((int) mx, (int) my, leftPos + CT_X, topPos + CT_Y, CT_W, CT_H)) {
            int maxScroll = Math.max(0, contentH - (CT_H - 16));
            scroll = Mth.clamp(scroll - (int) (delta * 14), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        GuideContent.Entry e = current();
        if (e != null) for (GuideContent.Element el : e.elements)
            if (el instanceof GuideContent.Multiblock mb && mb.inBox((int) mx, (int) my)) {
                mb.yaw += (float) dx;
                mb.pitch = Mth.clamp(mb.pitch + (float) dy, -90f, 90f);
                mb.dragging = true; return true;
            }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        GuideContent.Entry e = current();
        if (e != null) for (GuideContent.Element el : e.elements)
            if (el instanceof GuideContent.Multiblock mb) mb.dragging = false;
        return super.mouseReleased(mx, my, btn);
    }
}
