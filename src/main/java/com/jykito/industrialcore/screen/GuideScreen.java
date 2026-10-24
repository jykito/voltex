package com.jykito.industrialcore.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Native Voltex guide screen, "tablet" look (the author picked variant B from tools/guide-review/guide_mockup.html on 2026-10-07):
 * a device frame, chapters as icon tabs at the top with the chapter title, search and the current chapter's entries on the left
 * (when searching, the matching entries of all chapters), content from typed blocks on the right ({@link GuideContent}),
 * "back / next" through the whole guide in order and "entry N of M" at the bottom.
 * <p>
 * The window fits the screen: not larger than {@link #MAX_W}x{@link #MAX_H}, not smaller than the old 315x256. Everything is drawn in code
 * in the old panel's colours. The language follows the interface.
 */
public class GuideScreen extends Screen {

    private static final int MAX_W = 470, MAX_H = 300, MIN_W = 315, MIN_H = 256;
    private static final int BEZEL = 7, TAB_H = 24, TAB_W = 22, LIST_W = 128, SEARCH_H = 18, FOOT_H = 15, ROW_H = 13;
    private static final int TEXT = 0xFFE6EDF3, SUB = 0xFFAEB6BF, DIM = 0xFF8B949E, ACCENT = 0xFF00FFCC, EDGE = 0xFF3A4048;
    /** Add-on chapters: amber, apart from Voltex's own cyan. */
    private static final int ADDON = 0xFFE8B04A;
    private static final int C_BEZEL = 0xFF14171A, C_BEZEL_IN = 0xFF1A1E22, C_TABS = 0xFF343A40, C_LIST = 0xFF1B1E21,
            C_BODY = 0xFF23272B, C_FOOT = 0xFF1D2125, C_FIELD = 0xFF0E1013;

    private int leftPos, topPos, guiW, guiH;
    /** Inner area (without the frame) and its parts. */
    private int ix, iy, iw, ih, listX, listY, listH, ctX, ctY, ctW, ctH;
    private final List<GuideContent.Category> cats;
    private int catI = 0, entI = 0;
    private int scroll = 0, contentH = 0;
    private int listScroll = 0, listContentH = 0;
    private final List<int[]> listHits = new ArrayList<>();   // {x, y, w, h, ci, ei}
    private final List<int[]> tabHits = new ArrayList<>();    // {x, y, w, h, ci}
    private EditBox search;
    private String query = "";
    private int[] prevHit, nextHit;                            // {x, y, w, h} of the buttons at the bottom

    public GuideScreen() {
        super(Component.translatable("gui.industrial_core.guide.title"));
        this.cats = new ArrayList<>(GuideContent.load());
        this.cats.add(NaniteAtlasPages.build());   // nanite species atlas as the last tab
    }

    @Override protected void init() {
        guiW = Mth.clamp(width - 30, MIN_W, MAX_W);
        guiH = Mth.clamp(height - 20, MIN_H, MAX_H);
        leftPos = (width - guiW) / 2;
        topPos = (height - guiH) / 2;
        ix = leftPos + BEZEL; iy = topPos + BEZEL; iw = guiW - 2 * BEZEL; ih = guiH - 2 * BEZEL;
        listX = ix; listY = iy + TAB_H + SEARCH_H; listH = ih - TAB_H - SEARCH_H;
        ctX = ix + LIST_W + 1; ctY = iy + TAB_H; ctW = iw - LIST_W - 1; ctH = ih - TAB_H - FOOT_H;

        search = new EditBox(font, ix + 16, iy + TAB_H + 5, LIST_W - 22, 10, Component.empty());
        search.setBordered(false);
        search.setMaxLength(40);
        search.setTextColor(TEXT);
        search.setHint(Component.translatable("gui.industrial_core.guide.search").withStyle(st -> st.withColor(DIM & 0xFFFFFF)));
        search.setValue(query);
        search.setResponder(s -> { query = s.trim().toLowerCase(Locale.ROOT); listScroll = 0; });
        addRenderableWidget(search);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        renderBezel(g);
        renderTabs(g, mx, my);
        renderSearchField(g);
        renderList(g, mx, my);
        renderContent(g, mx, my);
        renderFooter(g, mx, my);
        super.render(g, mx, my, pt);
        renderTooltips(g, mx, my);
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ── device frame ──

    private void renderBezel(GuiGraphics g) {
        int x0 = leftPos, y0 = topPos, x1 = leftPos + guiW, y1 = topPos + guiH;
        g.fill(x0, y0, x1, y1, C_BEZEL);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, C_BEZEL_IN);
        for (int[] s : new int[][]{{x0 + 3, y0 + 3}, {x1 - 6, y0 + 3}, {x0 + 3, y1 - 6}, {x1 - 6, y1 - 6}}) {   // screws
            g.fill(s[0], s[1], s[0] + 3, s[1] + 3, EDGE);
            g.fill(s[0] + 1, s[1] + 1, s[0] + 2, s[1] + 2, 0xFF111417);
        }
        int cx = (x0 + x1) / 2;
        g.fill(cx - 12, y0 + 2, cx + 12, y0 + 4, ACCENT);   // power light
        g.fill(ix, iy, ix + iw, iy + ih, C_BODY);
    }

    // ── chapter tabs ──

    private void renderTabs(GuiGraphics g, int mx, int my) {
        tabHits.clear();
        g.fill(ix, iy, ix + iw, iy + TAB_H, C_TABS);
        int x = ix + 4;
        for (int ci = 0; ci < cats.size(); ci++) {
            boolean act = ci == catI && query.isEmpty(), hv = in(mx, my, x - 1, iy + 2, TAB_W + 2, TAB_H - 2);
            if (act) { g.fill(x - 1, iy + 2, x + TAB_W + 1, iy + TAB_H, C_BODY); g.fill(x - 1, iy + 2, x + TAB_W + 1, iy + 3, ACCENT); }
            else if (hv) g.fill(x - 1, iy + 2, x + TAB_W + 1, iy + TAB_H, 0x18FFFFFF);
            ItemStack icon = cats.get(ci).icon;
            if (icon != null && !icon.isEmpty()) g.renderItem(icon, x + 3, iy + 5);
            if (!cats.get(ci).addon.isEmpty()) g.fill(x + TAB_W - 4, iy + 5, x + TAB_W - 1, iy + 8, ADDON);   // add-on chapter mark
            tabHits.add(new int[]{x - 1, iy + 2, TAB_W + 2, TAB_H - 2, ci});
            x += TAB_W + 2;
        }
        // chapter title on the right, if it fits after the tabs
        String name = query.isEmpty() && !cats.isEmpty() ? cats.get(catI).name : Component.translatable("gui.industrial_core.guide.search").getString();
        int right = ix + iw - 8, room = right - x - 6;
        if (room > 30) {
            String t = font.plainSubstrByWidth(name, room);
            g.drawString(font, t, right - font.width(t), iy + 8, ACCENT, false);
        }
    }

    private void renderSearchField(GuiGraphics g) {
        int x = ix + 4, y = iy + TAB_H + 3, w = LIST_W - 8;
        g.fill(ix, iy + TAB_H, ix + LIST_W, iy + ih, C_LIST);
        g.fill(ix + LIST_W, iy + TAB_H, ix + LIST_W + 1, iy + ih, EDGE);
        g.fill(x, y, x + w, y + 12, C_FIELD);
        g.fill(x, y + 11, x + w, y + 12, search.isFocused() ? ACCENT : 0xFF2F343A);
        g.drawString(font, "⌕", x + 3, y + 2, DIM, false);
    }

    // ── chapter entries (or search results) ──

    private void renderList(GuiGraphics g, int mx, int my) {
        listHits.clear();
        int x = listX, top = listY, h = listH;
        int startY = top + 2 - listScroll, y = startY;
        boolean over = in(mx, my, x, top, LIST_W, h);
        g.enableScissor(x, top, x + LIST_W, top + h);
        // add-on chapter: its add-on's name as the first row (the title bar has no room once tabs pile up)
        String addon = query.isEmpty() && !cats.isEmpty() ? cats.get(catI).addon : "";
        if (!addon.isEmpty()) {
            g.fill(x + 7, y + 3, x + 11, y + 7, ADDON);
            g.drawString(font, font.plainSubstrByWidth(addon, LIST_W - 26), x + 20, y + 2, ADDON, false);
            y += ROW_H;
        }
        int found = 0;
        for (int ci = 0; ci < cats.size(); ci++) {
            if (query.isEmpty() && ci != catI) continue;
            List<GuideContent.Entry> es = cats.get(ci).entries;
            for (int ei = 0; ei < es.size(); ei++) {
                GuideContent.Entry e = es.get(ei);
                if (!query.isEmpty() && !e.searchText().contains(query)) continue;
                found++;
                boolean vis = y + ROW_H > top && y < top + h;
                boolean act = ci == catI && ei == entI, hv = vis && over && in(mx, my, x, y, LIST_W, ROW_H);
                if (act)     { g.fill(x, y - 1, x + LIST_W, y + ROW_H - 1, 0x1C00FFCC); g.fill(x, y - 1, x + 2, y + ROW_H - 1, ACCENT); }
                else if (hv) g.fill(x, y - 1, x + LIST_W, y + ROW_H - 1, 0x14FFFFFF);
                smallIcon(g, e.icon, x + 5, y, 0.7f);
                g.drawString(font, font.plainSubstrByWidth(e.name, LIST_W - 26), x + 20, y + 2, act ? ACCENT : (hv ? TEXT : SUB), false);
                if (vis) listHits.add(new int[]{x, y - 1, LIST_W, ROW_H, ci, ei});
                y += ROW_H;
            }
        }
        if (found == 0 && !query.isEmpty()) {
            g.drawString(font, Component.translatable("gui.industrial_core.guide.nothing"), x + 6, y + 3, DIM, false);
            y += ROW_H;
        }
        listContentH = y - startY;
        g.disableScissor();
        scrollbar(g, x + LIST_W - 3, top, h, listScroll, listContentH);
    }

    private void smallIcon(GuiGraphics g, ItemStack st, int x, int y, float s) {
        if (st == null || st.isEmpty()) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(s, s, 1f);
        g.renderItem(st, 0, 0);
        g.pose().popPose();
    }

    private static void scrollbar(GuiGraphics g, int x, int top, int h, int scroll, int contentH) {
        int max = Math.max(0, contentH - h);
        if (max <= 0) return;
        int tH = h - 4, thumbH = Math.max(14, tH * h / contentH);
        int thumbY = top + 2 + (tH - thumbH) * scroll / max;
        g.fill(x, thumbY, x + 2, thumbY + thumbH, 0x6000FFCC);
    }

    // ── content ──

    private void renderContent(GuiGraphics g, int mx, int my) {
        int x = ctX + 9, top = ctY + 8, w = ctW - 20;
        g.enableScissor(ctX, ctY, ctX + ctW, ctY + ctH);
        int startY = top - scroll, y = startY;
        GuideContent.Entry e = current();
        if (e != null) for (GuideContent.Element el : e.elements) y += el.render(g, font, x, y, w) + 5;
        contentH = y - startY;
        g.disableScissor();
        scrollbar(g, ctX + ctW - 3, ctY, ctH, scroll, contentH + 12);
    }

    /** "‹ previous / next ›" through the whole guide in order, across chapter borders; "entry N of M" in the middle. */
    private void renderFooter(GuiGraphics g, int mx, int my) {
        int x = ctX, y = ctY + ctH, w = ctW;
        g.fill(x, y, x + w, y + FOOT_H, C_FOOT);
        g.fill(x, y, x + w, y + 1, EDGE);
        prevHit = nextHit = null;
        int[] p = neighbour(-1), n = neighbour(1);
        int maxW = w / 3;
        if (p != null) {
            String t = "‹ " + font.plainSubstrByWidth(cats.get(p[0]).entries.get(p[1]).name, maxW - 10);
            prevHit = new int[]{x + 4, y + 2, font.width(t) + 4, FOOT_H - 3};
            g.drawString(font, t, x + 6, y + 4, in(mx, my, prevHit[0], prevHit[1], prevHit[2], prevHit[3]) ? ACCENT : SUB, false);
        }
        if (n != null) {
            String t = font.plainSubstrByWidth(cats.get(n[0]).entries.get(n[1]).name, maxW - 10) + " ›";
            int tw = font.width(t);
            nextHit = new int[]{x + w - 8 - tw, y + 2, tw + 4, FOOT_H - 3};
            g.drawString(font, t, x + w - 6 - tw, y + 4, in(mx, my, nextHit[0], nextHit[1], nextHit[2], nextHit[3]) ? ACCENT : SUB, false);
        }
        if (current() != null) {
            String pos = Component.translatable("gui.industrial_core.guide.position", entI + 1, cats.get(catI).entries.size()).getString();
            int pw = font.width(pos);
            if (pw < w - 2 * maxW) g.drawString(font, pos, x + (w - pw) / 2, y + 4, DIM, false);
        }
    }

    /** The neighbouring entry in the overall order: {chapter, entry} or null at the edge. */
    private int[] neighbour(int dir) {
        if (cats.isEmpty() || current() == null) return null;
        int ci = catI, ei = entI + dir;
        while (ci >= 0 && ci < cats.size()) {
            int size = cats.get(ci).entries.size();
            if (ei >= 0 && ei < size) return new int[]{ci, ei};
            ci += dir;
            if (ci < 0 || ci >= cats.size()) return null;
            ei = dir > 0 ? 0 : cats.get(ci).entries.size() - 1;
        }
        return null;
    }

    private void renderTooltips(GuiGraphics g, int mx, int my) {
        for (int[] t : tabHits)
            if (in(mx, my, t[0], t[1], t[2], t[3])) {
                GuideContent.Category c = cats.get(t[4]);
                List<Component> lines = new ArrayList<>(List.of(Component.literal(c.name)));
                if (!c.addon.isEmpty()) lines.add(Component.literal(c.addon).withStyle(s -> s.withColor(ADDON & 0xFFFFFF)));
                g.renderComponentTooltip(font, lines, mx, my);
                return;
            }
        if (!in(mx, my, ctX, ctY, ctW, ctH)) return;
        GuideContent.Entry e = current();
        if (e == null) return;
        for (GuideContent.Element el : e.elements) {
            ItemStack hov = el.hoverItem(mx, my);
            if (hov != null && !hov.isEmpty()) { g.renderTooltip(font, hov, mx, my); return; }
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

    private void open(int ci, int ei) {
        if (ci != catI) listScroll = 0;
        catI = ci; entI = ei; scroll = 0;
    }

    /** Following a link: entry id or its title, in any chapter. */
    private boolean follow(String target) {
        for (int ci = 0; ci < cats.size(); ci++) {
            List<GuideContent.Entry> es = cats.get(ci).entries;
            for (int ei = 0; ei < es.size(); ei++) if (es.get(ei).is(target)) { open(ci, ei); return true; }
        }
        return false;
    }

    // ── input ──

    @Override
    public boolean mouseClicked(double mxd, double myd, int btn) {
        int mx = (int) mxd, my = (int) myd;
        GuideContent.Entry ce = current();
        if (ce != null && in(mx, my, ctX, ctY, ctW, ctH)) for (GuideContent.Element el : ce.elements) {
            if (el instanceof GuideContent.Multiblock mb && mb.hitButton(mx, my)) { mb.step(); return true; }
            if (el instanceof GuideContent.Multiblock mb && mb.hitModelButton(mx, my)) { mb.toggleModel(); return true; }
            String link = el.linkAt(mx, my);
            if (link != null && follow(link)) return true;
        }
        if (prevHit != null && in(mx, my, prevHit[0], prevHit[1], prevHit[2], prevHit[3])) { int[] p = neighbour(-1); if (p != null) open(p[0], p[1]); return true; }
        if (nextHit != null && in(mx, my, nextHit[0], nextHit[1], nextHit[2], nextHit[3])) { int[] n = neighbour(1); if (n != null) open(n[0], n[1]); return true; }
        for (int[] t : tabHits)
            if (in(mx, my, t[0], t[1], t[2], t[3])) {   // chapter tab: reset the search, open the first entry
                if (!query.isEmpty()) { search.setValue(""); query = ""; }
                if (catI != t[4]) open(t[4], 0);
                return true;
            }
        if (in(mx, my, listX, listY, LIST_W, listH))
            for (int[] h : listHits)
                if (in(mx, my, h[0], h[1], h[2], h[3])) { open(h[4], h[5]); return true; }
        return super.mouseClicked(mxd, myd, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (in((int) mx, (int) my, listX, listY, LIST_W, listH)) {
            listScroll = Mth.clamp(listScroll - (int) (delta * 14), 0, Math.max(0, listContentH - listH));
            return true;
        }
        if (in((int) mx, (int) my, ctX, ctY, ctW, ctH)) {
            scroll = Mth.clamp(scroll - (int) (delta * 14), 0, Math.max(0, contentH + 12 - ctH));
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
