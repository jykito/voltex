package com.jykito.industrialcore.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jykito.industrialcore.IndustrialCore;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Модель контента гайда + загрузчик из JSON.
 * Файлы: assets/industrial_core/guide/&lt;lang&gt;/*.json — по одному на категорию.
 * Язык берётся из интерфейса; если для языка ничего нет — фолбэк на en_us.
 * Загрузка ленивая (на открытии экрана): правки подхватываются по F3+T без пересборки.
 */
public final class GuideContent {
    private static final Logger LOG = LogUtils.getLogger();
    private GuideContent() {}

    static final int TEXT = 0xFFE6EDF3, SUB = 0xFFAEB6BF, MUTE = 0xFF6A6A73, ACCENT = 0xFF00FFCC, SLOT = 0xFF0E0E12;

    // ---------- модель ----------
    public static final class Category {
        public final String name; public final List<Entry> entries; public final ItemStack icon;
        Category(String n, List<Entry> e, ItemStack icon) { name = n; entries = e; this.icon = icon; }
    }
    public static final class Entry {
        public final String id, name; public final List<Element> elements; public final ItemStack icon;
        Entry(String id, String n, List<Element> e, ItemStack icon) { this.id = id; name = n; elements = e; this.icon = icon; }
        /** Совпадает ли с целью ссылки: по id, иначе по названию. */
        boolean is(String target) { return target.equals(id) || target.equalsIgnoreCase(name); }
        /** Весь текст записи в нижнем регистре — для поиска. */
        String searchText() {
            StringBuilder sb = new StringBuilder(name);
            for (Element el : elements) sb.append(' ').append(el.plainText());
            return sb.toString().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Блок контента. render возвращает свою высоту; по ходу отрисовки блок запоминает,
     * где лежат его иконки и ссылки, чтобы экран показал подсказку и обработал клик.
     */
    public abstract static class Element {
        public abstract int render(GuiGraphics g, Font f, int x, int y, int w);
        /** Предмет под курсором (для подсказки) или null. */
        public ItemStack hoverItem(int mx, int my) { return null; }
        /** Цель ссылки под курсором или null. */
        public String linkAt(int mx, int my) { return null; }
        /** Первый предмет блока — автоиконка записи и главы. */
        public ItemStack firstItem() { return ItemStack.EMPTY; }
        /** Текст блока для поиска. */
        public String plainText() { return ""; }
    }

    /** Где отрисованы иконки блока: прямоугольник + предмет. */
    static final class Hits {
        final List<int[]> rects = new ArrayList<>(); final List<ItemStack> items = new ArrayList<>();
        void clear() { rects.clear(); items.clear(); }
        void add(int x, int y, int w, int h, ItemStack st) { rects.add(new int[]{x, y, w, h}); items.add(st); }
        ItemStack at(int mx, int my) {
            for (int i = 0; i < rects.size(); i++) {
                int[] r = rects.get(i);
                if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) return items.get(i);
            }
            return null;
        }
    }

    /** Разметка текста: **слово** — бирюзой, остальное — цветом абзаца. */
    static Component rich(String s, int baseColor) {
        MutableComponent out = Component.empty();
        String[] parts = s.split("\\*\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            int col = (i % 2 == 1 ? ACCENT : baseColor) & 0xFFFFFF;
            out.append(Component.literal(parts[i]).withStyle(st -> st.withColor(col)));
        }
        return out;
    }
    static String plain(String s) { return s.replace("**", ""); }

    static void slot(GuiGraphics g, int x, int y, int size) {
        g.fill(x, y, x + size, y + size, SLOT);
        g.fill(x, y, x + size, y + 1, 0xFF22262C);
    }

    static int parseColor(String hex) {
        try { return 0xFF000000 | Integer.parseInt(hex.replace("#", ""), 16); } catch (Exception e) { return ACCENT; }
    }

    static final class Header extends Element {
        final String s; Header(String s) { this.s = s; }
        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            g.drawString(f, s, x, y, TEXT, false);
            g.fill(x, y + 11, x + w, y + 12, 0x3000FFCC);
            return 16;
        }
        @Override public String plainText() { return s; }
    }
    static final class Text extends Element {
        final String raw; final Component c;
        Text(String s) { raw = s; c = rich(s, SUB); }
        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            List<FormattedCharSequence> ls = f.split(c, w);
            int yy = y;
            for (FormattedCharSequence l : ls) { g.drawString(f, l, x, yy, SUB, false); yy += 10; }
            return ls.size() * 10 + 2;
        }
        @Override public String plainText() { return plain(raw); }
    }
    static final class Items extends Element {
        final String label; final ItemStack[] st; final Hits hits = new Hits();
        Items(String label, ItemStack[] st) { this.label = label; this.st = st; }
        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            hits.clear();
            int yy = y;
            if (label != null) { g.drawString(f, label, x, yy, MUTE, false); yy += 11; }
            int ix = x;
            for (ItemStack s : st) { slot(g, ix, yy, 18); g.renderItem(s, ix + 1, yy + 1); hits.add(ix, yy, 18, 18, s); ix += 22; }
            return (label != null ? 11 : 0) + 20;
        }
        @Override public ItemStack hoverItem(int mx, int my) { return hits.at(mx, my); }
        @Override public ItemStack firstItem() { return st.length > 0 ? st[0] : ItemStack.EMPTY; }
        @Override public String plainText() {
            StringBuilder b = new StringBuilder(label == null ? "" : label);
            for (ItemStack s : st) b.append(' ').append(s.getHoverName().getString());
            return b.toString();
        }
    }

    /** Карточка записи: крупная иконка, название, подзаголовок, цветные метки. */
    static final class Card extends Element {
        final ItemStack icon; final String title, subtitle; final List<String[]> tags; final Hits hits = new Hits();
        Card(ItemStack icon, String title, String subtitle, List<String[]> tags) { this.icon = icon; this.title = title; this.subtitle = subtitle; this.tags = tags; }
        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            hits.clear();
            slot(g, x, y, 34);
            g.pose().pushPose(); g.pose().translate(x + 1, y + 1, 0); g.pose().scale(2f, 2f, 1f);
            g.renderItem(icon, 0, 0);
            g.pose().popPose();
            hits.add(x, y, 34, 34, icon);
            int tx = x + 40, tw = w - 40, ty = y + 1;
            for (FormattedCharSequence l : f.split(Component.literal(title), tw)) { g.drawString(f, l, tx, ty, TEXT, false); ty += 10; }
            if (subtitle != null && !subtitle.isEmpty())
                for (FormattedCharSequence l : f.split(Component.literal(subtitle), tw)) { g.drawString(f, l, tx, ty, MUTE, false); ty += 10; }
            int cx = tx, cy = ty + 1;
            for (String[] t : tags) {
                int col = parseColor(t[1]), cw = f.width(t[0]) + 6;
                if (cx + cw > x + w && cx > tx) { cx = tx; cy += 12; }
                g.fill(cx, cy, cx + cw, cy + 10, (col & 0xFFFFFF) | 0x30000000);
                g.drawString(f, t[0], cx + 3, cy + 1, col, false);
                cx += cw + 3;
            }
            int h = Math.max(34, (tags.isEmpty() ? ty : cy + 10) - y);
            g.fill(x, y + h + 3, x + w, y + h + 4, 0x3000FFCC);
            return h + 8;
        }
        @Override public ItemStack hoverItem(int mx, int my) { return hits.at(mx, my); }
        @Override public ItemStack firstItem() { return icon; }
        @Override public String plainText() { return title + ' ' + (subtitle == null ? "" : subtitle); }
    }

    /**
     * Характеристики: пары «что — как» в тёмной плашке; длинные значения переносятся. На широком контенте
     * (от {@link #TWO_COLUMNS} px) и от 4 строк — в две колонки, каждая своей плашкой, но только если значению в колонке
     * остаётся от {@link #MIN_VALUE} px: длинная подпись («Энергоэффективность») иначе выталкивала значение за колонку.
     */
    static final class Stats extends Element {
        static final int TWO_COLUMNS = 280, MIN_VALUE = 80;
        final List<String[]> rows; Stats(List<String[]> rows) { this.rows = rows; }
        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            int kw = 0;
            for (String[] r : rows) kw = Math.max(kw, f.width(r[0]));
            int cw = (w - 6) / 2;
            if (w < TWO_COLUMNS || rows.size() < 4 || 6 + kw + 8 + MIN_VALUE > cw - 6) return column(g, f, x, y, w, rows);
            int half = (rows.size() + 1) / 2;
            int h1 = column(g, f, x, y, cw, rows.subList(0, half));
            int h2 = column(g, f, x + cw + 6, y, cw, rows.subList(half, rows.size()));
            return Math.max(h1, h2);
        }

        private static int column(GuiGraphics g, Font f, int x, int y, int w, List<String[]> rows) {
            int kw = 0;
            for (String[] r : rows) kw = Math.max(kw, f.width(r[0]));
            int vx = x + 6 + kw + 8, vw = Math.max(40, x + w - 6 - vx);
            List<List<FormattedCharSequence>> vals = new ArrayList<>();
            int total = 7;
            for (String[] r : rows) { List<FormattedCharSequence> v = f.split(rich(r[1], TEXT), vw); vals.add(v); total += v.size() * 10 + 1; }
            g.fill(x, y, x + w, y + total, 0xFF1A1D22);
            g.fill(x, y, x + w, y + 1, 0xFF2F343A);
            int yy = y + 4;
            for (int i = 0; i < rows.size(); i++) {
                g.drawString(f, rows.get(i)[0], x + 6, yy, MUTE, false);
                for (FormattedCharSequence l : vals.get(i)) { g.drawString(f, l, vx, yy, TEXT, false); yy += 10; }
                yy += 1;
            }
            return total + 4;
        }
        @Override public String plainText() {
            StringBuilder b = new StringBuilder();
            for (String[] r : rows) b.append(r[0]).append(' ').append(plain(r[1])).append(' ');
            return b.toString();
        }
    }

    /** Совет: подсвеченная плашка с полосой слева. */
    static final class Tip extends Element {
        final String raw; Tip(String s) { raw = s; }
        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            List<FormattedCharSequence> ls = f.split(rich(raw, TEXT), w - 18);
            int h = ls.size() * 10 + 7;
            g.fill(x, y, x + w, y + h, 0xFF0F2A28);
            g.fill(x, y, x + 2, y + h, ACCENT);
            g.drawString(f, "!", x + 7, y + 4, ACCENT, false);
            int yy = y + 4;
            for (FormattedCharSequence l : ls) { g.drawString(f, l, x + 14, yy, TEXT, false); yy += 10; }
            return h + 4;
        }
        @Override public String plainText() { return plain(raw); }
    }

    /** Список предметов с названиями и количеством, в две колонки. */
    static final class ItemList extends Element {
        final String label; final List<ItemStack> st; final Hits hits = new Hits();
        ItemList(String label, List<ItemStack> st) { this.label = label; this.st = st; }
        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            hits.clear();
            int yy = y;
            if (label != null) { g.drawString(f, label, x, yy, MUTE, false); yy += 11; }
            int colW = w / 2;
            for (int i = 0; i < st.size(); i++) {
                ItemStack s = st.get(i);
                int cx = x + (i % 2) * colW, cy = yy + (i / 2) * 20;
                slot(g, cx, cy, 18);
                g.renderItem(s, cx + 1, cy + 1);
                hits.add(cx, cy, colW - 4, 18, s);
                String cnt = s.getCount() > 1 ? "×" + s.getCount() : "";
                int nameW = colW - 26 - (cnt.isEmpty() ? 0 : f.width(cnt) + 4);
                g.drawString(f, f.plainSubstrByWidth(s.getHoverName().getString(), nameW), cx + 22, cy + 5, TEXT, false);
                if (!cnt.isEmpty()) g.drawString(f, cnt, cx + colW - 6 - f.width(cnt), cy + 5, MUTE, false);
            }
            return (label != null ? 11 : 0) + ((st.size() + 1) / 2) * 20 + 2;
        }
        @Override public ItemStack hoverItem(int mx, int my) { return hits.at(mx, my); }
        @Override public ItemStack firstItem() { return st.isEmpty() ? ItemStack.EMPTY : st.get(0); }
        @Override public String plainText() {
            StringBuilder b = new StringBuilder(label == null ? "" : label);
            for (ItemStack s : st) b.append(' ').append(s.getHoverName().getString());
            return b.toString();
        }
    }

    /** Ссылки на другие записи: кликабельные плашки {текст, цель}. */
    static final class Links extends Element {
        final String label; final List<String[]> links; final List<int[]> rects = new ArrayList<>();
        Links(String label, List<String[]> links) { this.label = label; this.links = links; }
        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            rects.clear();
            int yy = y;
            if (label != null) { g.drawString(f, label, x, yy, MUTE, false); yy += 11; }
            int cx = x, cy = yy;
            for (String[] l : links) {
                String t = l[0] + " ›";
                int tw = f.width(t) + 8;
                if (cx + tw > x + w && cx > x) { cx = x; cy += 13; }
                g.fill(cx, cy, cx + tw, cy + 11, 0xFF17302C);
                g.drawString(f, t, cx + 4, cy + 2, ACCENT, false);
                rects.add(new int[]{cx, cy, tw, 11});
                cx += tw + 4;
            }
            return cy + 11 - y + 4;
        }
        @Override public String linkAt(int mx, int my) {
            for (int i = 0; i < rects.size(); i++) {
                int[] r = rects.get(i);
                if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) return links.get(i)[1];
            }
            return null;
        }
    }
    static final class Image extends Element {
        final ResourceLocation tex; final int w, h;
        Image(ResourceLocation t, int w, int h) { tex = t; this.w = w; this.h = h; }
        public int render(GuiGraphics g, Font f, int x, int y, int width) {
            int dw = w > 0 ? w : width, dh = h > 0 ? h : width / 2;
            if (dw > width) { dh = dh * width / dw; dw = width; }
            g.blit(tex, x, y, 0, 0, dw, dh, dw, dh);
            return dh + 2;
        }
    }

    /**
     * 3D-превью структуры: модели блоков, драг-вращение + авто-спин; кнопка листает слои (1,2,…,полная,1); материалы с тултипами.
     * Если у схемы есть {@code model} (структура, у которой при сборке блоки прячутся и рисуется своя модель), на полном
     * виде вместо блоков рисуется эта модель от блока ядра (GuideStructureModels); по слоям — как и раньше, блоки.
     */
    static final class Multiblock extends Element {
        final List<BlockState> states; final List<BlockPos> poss;
        final @Nullable String model; final @Nullable BlockPos anchor;
        final int sx, sy, sz;
        int boxH = 130;               // высота окна структуры; растёт с шириной контента (панель «планшет» шире прежней)
        final List<ItemStack> mats;   // иконка + count = нужное число
        final int[] matX, matYs;      // где лежат слоты материалов (для тултипа); строк может быть несколько
        float yaw = -45f, pitch = 30f;
        int shownLayers;              // сколько слоёв показано (1..sy; sy = полная форма)
        boolean dragging = false;
        int lastX, lastY, lastW, matY;
        int btnX, btnY, btnW, btnH;
        boolean showModel = true;     // своя модель на полном виде; выключил — видны блоки последнего слоя
        int mbtnX, mbtnY, mbtnW;

        Multiblock(List<BlockState> s, List<BlockPos> p, int sx, int sy, int sz, @Nullable String model) {
            states = s; poss = p; this.sx = sx; this.sy = sy; this.sz = sz;
            String core = model == null ? null : GuideStructureModels.coreOf(model);
            BlockPos a = null;
            if (core != null) for (int i = 0; i < s.size() && a == null; i++)
                if (core.equals(String.valueOf(ForgeRegistries.BLOCKS.getKey(s.get(i).getBlock())))) a = p.get(i);
            this.anchor = a; this.model = a == null ? null : model;
            this.shownLayers = sy;
            LinkedHashMap<Item, Integer> cnt = new LinkedHashMap<>();
            for (BlockState st : s) { ItemStack ic = new ItemStack(st.getBlock()); if (!ic.isEmpty()) cnt.merge(ic.getItem(), 1, Integer::sum); }
            List<ItemStack> m = new ArrayList<>();
            for (Map.Entry<Item, Integer> e : cnt.entrySet()) m.add(new ItemStack(e.getKey(), e.getValue()));
            mats = m;
            matX = new int[m.size()];
            matYs = new int[m.size()];
        }

        boolean inBox(int mx, int my)     { return mx >= lastX && mx < lastX + lastW && my >= lastY && my < lastY + boxH; }
        boolean hitButton(int mx, int my) { return mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH; }
        void step()                       { if (sy > 0) shownLayers = (shownLayers % sy) + 1; }
        boolean hitModelButton(int mx, int my) { return model != null && mx >= mbtnX && mx < mbtnX + mbtnW && my >= mbtnY && my < mbtnY + btnH; }
        void toggleModel()                { showModel = !showModel; }

        ItemStack materialAt(int mx, int my) {
            for (int i = 0; i < mats.size(); i++)
                if (mx >= matX[i] && mx < matX[i] + 18 && my >= matYs[i] && my < matYs[i] + 18) return mats.get(i);
            return null;
        }
        @Override public ItemStack hoverItem(int mx, int my) { return materialAt(mx, my); }

        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            lastX = x; lastY = y; lastW = w;
            boxH = Math.max(130, Math.min(190, w * 3 / 5));
            // без автокручения: структура стоит, крутится только перетаскиванием
            renderStructure(g, x, y, w);

            btnW = 82; btnH = 16; mbtnW = model != null ? 82 : 0;
            btnX = x + (w - btnW - (mbtnW > 0 ? mbtnW + 4 : 0)) / 2; btnY = y + boxH + 4;
            button(g, btnX, btnY, btnW, btnH);
            String label = shownLayers >= sy
                    ? Component.translatable("gui.industrial_core.guide.assemble").getString()
                    : Component.translatable("gui.industrial_core.guide.layer").getString() + " " + shownLayers + "/" + sy;
            g.drawString(f, label, btnX + (btnW - f.width(label)) / 2, btnY + 4, ACCENT, false);
            if (model != null) {   // переключатель своей модели
                mbtnX = btnX + btnW + 4; mbtnY = btnY;
                button(g, mbtnX, mbtnY, mbtnW, btnH);
                String ml = Component.translatable(showModel ? "gui.industrial_core.guide.model_on" : "gui.industrial_core.guide.model_off").getString();
                g.drawString(f, ml, mbtnX + (mbtnW - f.width(ml)) / 2, mbtnY + 4, showModel ? ACCENT : MUTE, false);
            }

            int ly = btnY + btnH + 8;
            g.drawString(f, Component.translatable("gui.industrial_core.guide.materials").getString(), x, ly, MUTE, false);
            matY = ly + 11;
            // материалы переносятся на новую строку, когда не влезают по ширине
            int ix = x, iy = matY;
            for (int i = 0; i < mats.size(); i++) {
                ItemStack st = mats.get(i);
                String n = "×" + st.getCount();
                int cellW = 21 + f.width(n) + 10;
                if (ix > x && ix + cellW - 10 > x + w) { ix = x; iy += 21; }
                matX[i] = ix; matYs[i] = iy;
                slot(g, ix, iy, 18);
                g.renderItem(st, ix + 1, iy + 1);
                g.drawString(f, n, ix + 21, iy + 5, SUB, false);
                ix += cellW;
            }
            return (iy + 20 + 4) - y;
        }

        private static void button(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x, y, x + w, y + h, 0x2000FFCC);
            g.fill(x, y, x + w, y + 1, 0x4000FFCC);
            g.fill(x, y + h - 1, x + w, y + h, 0x4000FFCC);
        }

        private void renderStructure(GuiGraphics g, int x, int y, int w) {
            int maxDim = Math.max(sx, Math.max(sy, sz));
            if (maxDim <= 0) return;
            float scale = Math.min(w, boxH) / (float) maxDim * 0.55f;
            PoseStack ps = g.pose();
            ps.pushPose();
            ps.translate(x + w / 2.0, y + boxH / 2.0, 220);
            ps.scale(scale, -scale, scale);
            ps.mulPose(Axis.XP.rotationDegrees(pitch));
            ps.mulPose(Axis.YP.rotationDegrees(yaw));
            ps.translate(-sx / 2.0, -sy / 2.0, -sz / 2.0);

            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            Lighting.setupForFlatItems();                         // плоский равномерный свет: видимые грани всегда освещены (нет «перескока» при вращении)
            MultiBufferSource.BufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();
            BlockRenderDispatcher br = Minecraft.getInstance().getBlockRenderer();
            boolean formed = model != null && showModel && shownLayers >= sy;   // собранный вид: блоки спрятаны, рисуется модель
            for (int i = 0; i < states.size() && !formed; i++) {
                BlockPos p = poss.get(i);
                if (p.getY() >= shownLayers) continue;   // показываем слои 0..shownLayers-1
                ps.pushPose();
                ps.translate(p.getX(), p.getY(), p.getZ());
                br.renderSingleBlock(states.get(i), ps, buf, 0xF000F0, OverlayTexture.NO_OVERLAY);
                ps.popPose();
            }
            if (formed) {
                ps.pushPose();
                ps.translate(anchor.getX(), anchor.getY(), anchor.getZ());
                GuideStructureModels.render(model, ps, buf);
                ps.popPose();
                Lighting.setupFor3DItems();   // у моделей свет по нормалям, плоский их обесцвечивает
            }
            buf.endBatch();
            RenderSystem.enableCull();
            Lighting.setupForFlatItems();
            ps.popPose();
        }
    }

    // ---------- загрузка ----------
    public static List<Category> load() {
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        List<Category> cats = loadLang(lang);
        if (cats.isEmpty() && !"en_us".equals(lang)) cats = loadLang("en_us");
        return cats;
    }

    private static List<Category> loadLang(String lang) {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        Map<ResourceLocation, Resource> found = rm.listResources("guide/" + lang,
                rl -> rl.getNamespace().equals(IndustrialCore.MODID) && rl.getPath().endsWith(".json"));
        List<ResourceLocation> keys = new ArrayList<>(found.keySet());
        keys.sort(Comparator.comparing(ResourceLocation::getPath));
        List<Category> cats = new ArrayList<>();
        for (ResourceLocation rl : keys) {
            try (InputStreamReader r = new InputStreamReader(found.get(rl).open(), StandardCharsets.UTF_8)) {
                cats.add(parseCategory(GsonHelper.parse(r)));
            } catch (Exception e) {
                LOG.warn("[Voltex guide] не разобрал {}: {}", rl, e.toString());
            }
        }
        return cats;
    }

    private static Category parseCategory(JsonObject o) {
        String name = GsonHelper.getAsString(o, "name", "");
        List<Entry> entries = new ArrayList<>();
        if (o.has("entries")) for (JsonElement je : o.getAsJsonArray("entries")) {
            JsonObject eo = je.getAsJsonObject();
            List<Element> els = new ArrayList<>();
            if (eo.has("elements")) for (JsonElement ee : eo.getAsJsonArray("elements")) {
                Element el = parseElement(ee.getAsJsonObject());
                if (el != null) els.add(el);
            }
            // иконка записи: явная или первый предмет из её блоков
            ItemStack icon = eo.has("icon") ? stackOf(GsonHelper.getAsString(eo, "icon")) : ItemStack.EMPTY;
            for (int i = 0; icon.isEmpty() && i < els.size(); i++) icon = els.get(i).firstItem();
            String entryName = GsonHelper.getAsString(eo, "name", "");
            entries.add(new Entry(GsonHelper.getAsString(eo, "id", entryName), entryName, els, icon));
        }
        ItemStack icon = o.has("icon") ? stackOf(GsonHelper.getAsString(o, "icon")) : ItemStack.EMPTY;
        for (int i = 0; icon.isEmpty() && i < entries.size(); i++) icon = entries.get(i).icon;
        return new Category(name, entries, icon);
    }

    private static List<String[]> pairs(JsonObject o, String key, String a, String b) {
        List<String[]> out = new ArrayList<>();
        if (o.has(key)) for (JsonElement je : o.getAsJsonArray(key)) {
            if (je.isJsonArray()) { JsonArray arr = je.getAsJsonArray(); out.add(new String[]{arr.get(0).getAsString(), arr.size() > 1 ? arr.get(1).getAsString() : ""}); }
            else { JsonObject p = je.getAsJsonObject(); out.add(new String[]{GsonHelper.getAsString(p, a, ""), GsonHelper.getAsString(p, b, "")}); }
        }
        return out;
    }

    private static Element parseElement(JsonObject o) {
        String type = GsonHelper.getAsString(o, "type", "");
        switch (type) {
            case "header": return new Header(GsonHelper.getAsString(o, "text", ""));
            case "text":   return new Text(GsonHelper.getAsString(o, "text", ""));
            case "card":   return new Card(stackOf(GsonHelper.getAsString(o, "icon", "")), GsonHelper.getAsString(o, "title", ""),
                                           GsonHelper.getAsString(o, "subtitle", ""), pairs(o, "tags", "text", "color"));
            case "stats":  return new Stats(pairs(o, "rows", "key", "value"));
            case "tip":    return new Tip(GsonHelper.getAsString(o, "text", ""));
            case "itemlist": {
                String label = o.has("label") ? GsonHelper.getAsString(o, "label") : null;
                List<ItemStack> st = new ArrayList<>();
                if (o.has("items")) for (JsonElement je : o.getAsJsonArray("items")) {
                    if (je.isJsonPrimitive()) { st.add(stackOf(je.getAsString())); continue; }
                    JsonObject io = je.getAsJsonObject();
                    ItemStack s = stackOf(GsonHelper.getAsString(io, "id", ""));
                    if (!s.isEmpty()) s.setCount(Math.max(1, GsonHelper.getAsInt(io, "count", 1)));
                    st.add(s);
                }
                return new ItemList(label, st);
            }
            case "links":  return new Links(o.has("label") ? GsonHelper.getAsString(o, "label") : null, pairs(o, "links", "text", "to"));
            case "items": {
                String label = o.has("label") ? GsonHelper.getAsString(o, "label") : null;
                List<ItemStack> st = new ArrayList<>();
                if (o.has("items")) for (JsonElement je : o.getAsJsonArray("items")) st.add(stackOf(je.getAsString()));
                return new Items(label, st.toArray(new ItemStack[0]));
            }
            case "image": {
                ResourceLocation tex = new ResourceLocation(GsonHelper.getAsString(o, "texture", ""));
                return new Image(tex, GsonHelper.getAsInt(o, "w", 0), GsonHelper.getAsInt(o, "h", 0));
            }
            case "multiblock": return parseMultiblock(GsonHelper.getAsJsonObject(o, "pattern"));
            default: return null; // неизвестный тип → пропуск
        }
    }

    private static Element parseMultiblock(JsonObject pat) {
        Map<Character, BlockState> key = new HashMap<>();
        if (pat.has("key")) for (Map.Entry<String, JsonElement> en : pat.getAsJsonObject("key").entrySet()) {
            Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(en.getValue().getAsString()));
            if (b != null && !en.getKey().isEmpty()) key.put(en.getKey().charAt(0), b.defaultBlockState());
        }
        List<BlockPos> poss = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        JsonArray layers = pat.getAsJsonArray("layers");
        int sx = 0, sy = layers.size(), sz = 0;
        for (int ly = 0; ly < layers.size(); ly++) {
            JsonArray rows = layers.get(ly).getAsJsonArray();
            sz = Math.max(sz, rows.size());
            for (int rz = 0; rz < rows.size(); rz++) {
                String row = rows.get(rz).getAsString();
                sx = Math.max(sx, row.length());
                for (int cx = 0; cx < row.length(); cx++) {
                    BlockState st = key.get(row.charAt(cx));
                    if (st != null) { poss.add(new BlockPos(cx, ly, rz)); states.add(st); }
                }
            }
        }
        connectNeighbours(poss, states);
        return new Multiblock(states, poss, sx, sy, sz, pat.has("model") ? pat.get("model").getAsString() : null);
    }

    /**
     * Досчитывает соединения внутри превью.
     * <p>
     * Схема даёт только «какой блок где», а вид соединяемых блоков — панелей, корпуса, труб —
     * живёт в булевых свойствах сторон. С {@code defaultBlockState} они все выключены, поэтому
     * панели выходили прямыми, смотрели в одну сторону и не гнулись на углах.
     * <p>
     * Правило простое: сторона включается, если по соседству стоит ТОТ ЖЕ блок. Для панелей и
     * корпуса это ровно их собственное правило, а для превью большего и не нужно — мир вокруг
     * схемы всё равно не показывается.
     */
    private static void connectNeighbours(List<BlockPos> poss, List<BlockState> states) {
        Map<BlockPos, Block> occupied = new HashMap<>();
        for (int i = 0; i < poss.size(); i++) occupied.put(poss.get(i), states.get(i).getBlock());

        for (int i = 0; i < states.size(); i++) {
            BlockState st = states.get(i);
            BlockPos p = poss.get(i);
            for (Direction d : Direction.values()) {
                BooleanProperty prop = sideProperty(st, d);
                if (prop == null) continue;
                st = st.setValue(prop, occupied.get(p.relative(d)) == st.getBlock());
            }
            states.set(i, st);
        }
    }

    /** Булево свойство стороны по её имени: «north», «up» и так далее. Нет такого — блок не соединяемый. */
    @Nullable
    private static BooleanProperty sideProperty(BlockState st, Direction d) {
        for (Property<?> pr : st.getProperties())
            if (pr instanceof BooleanProperty bp && bp.getName().equals(d.getName())) return bp;
        return null;
    }

    private static ItemStack stackOf(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return it == null ? ItemStack.EMPTY : new ItemStack(it);
    }
}
