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
import net.minecraft.world.level.block.Block;
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
        public final String name; public final List<Entry> entries;
        Category(String n, List<Entry> e) { name = n; entries = e; }
    }
    public static final class Entry {
        public final String name; public final List<Element> elements;
        Entry(String n, List<Element> e) { name = n; elements = e; }
    }
    public abstract static class Element { public abstract int render(GuiGraphics g, Font f, int x, int y, int w); }

    static final class Header extends Element {
        final String s; Header(String s) { this.s = s; }
        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            g.drawString(f, s, x, y, TEXT, false);
            g.fill(x, y + 11, x + w, y + 12, 0x3000FFCC);
            return 16;
        }
    }
    static final class Text extends Element {
        final Component c; Text(String s) { c = Component.literal(s); }
        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            List<FormattedCharSequence> ls = f.split(c, w);
            int yy = y;
            for (FormattedCharSequence l : ls) { g.drawString(f, l, x, yy, SUB, false); yy += 9; }
            return ls.size() * 9 + 3;
        }
    }
    static final class Items extends Element {
        final String label; final ItemStack[] st;
        Items(String label, ItemStack[] st) { this.label = label; this.st = st; }
        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            int yy = y;
            if (label != null) { g.drawString(f, label, x, yy, MUTE, false); yy += 11; }
            int ix = x;
            for (ItemStack s : st) { g.fill(ix, yy, ix + 18, yy + 18, SLOT); g.renderItem(s, ix + 1, yy + 1); ix += 22; }
            return (label != null ? 11 : 0) + 20;
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

    /** 3D-превью структуры: модели блоков, драг-вращение + авто-спин; кнопка листает слои (1,2,…,полная,1); материалы с тултипами. */
    static final class Multiblock extends Element {
        final List<BlockState> states; final List<BlockPos> poss;
        final int sx, sy, sz, boxH = 130;
        final List<ItemStack> mats;   // иконка + count = нужное число
        final int[] matX;             // X-координаты слотов материалов (для тултипа)
        float yaw = -45f, pitch = 30f;
        int shownLayers;              // сколько слоёв показано (1..sy; sy = полная форма)
        boolean dragging = false;
        int lastX, lastY, lastW, matY;
        int btnX, btnY, btnW, btnH;

        Multiblock(List<BlockState> s, List<BlockPos> p, int sx, int sy, int sz) {
            states = s; poss = p; this.sx = sx; this.sy = sy; this.sz = sz;
            this.shownLayers = sy;
            LinkedHashMap<Item, Integer> cnt = new LinkedHashMap<>();
            for (BlockState st : s) { ItemStack ic = new ItemStack(st.getBlock()); if (!ic.isEmpty()) cnt.merge(ic.getItem(), 1, Integer::sum); }
            List<ItemStack> m = new ArrayList<>();
            for (Map.Entry<Item, Integer> e : cnt.entrySet()) m.add(new ItemStack(e.getKey(), e.getValue()));
            mats = m;
            matX = new int[m.size()];
        }

        boolean inBox(int mx, int my)     { return mx >= lastX && mx < lastX + lastW && my >= lastY && my < lastY + boxH; }
        boolean hitButton(int mx, int my) { return mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH; }
        void step()                       { if (sy > 0) shownLayers = (shownLayers % sy) + 1; }

        ItemStack materialAt(int mx, int my) {
            for (int i = 0; i < mats.size(); i++)
                if (mx >= matX[i] && mx < matX[i] + 18 && my >= matY && my < matY + 18) return mats.get(i);
            return null;
        }

        public int render(GuiGraphics g, Font f, int x, int y, int w) {
            lastX = x; lastY = y; lastW = w;
            if (!dragging) yaw += 0.25f;
            renderStructure(g, x, y, w);

            btnW = 82; btnH = 16;
            btnX = x + (w - btnW) / 2; btnY = y + boxH + 4;
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0x2000FFCC);
            g.fill(btnX, btnY, btnX + btnW, btnY + 1, 0x4000FFCC);
            g.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, 0x4000FFCC);
            String label = shownLayers >= sy
                    ? Component.translatable("gui.industrial_core.guide.assemble").getString()
                    : Component.translatable("gui.industrial_core.guide.layer").getString() + " " + shownLayers + "/" + sy;
            g.drawString(f, label, btnX + (btnW - f.width(label)) / 2, btnY + 4, ACCENT, false);

            int ly = btnY + btnH + 8;
            g.drawString(f, Component.translatable("gui.industrial_core.guide.materials").getString(), x, ly, MUTE, false);
            matY = ly + 11;
            int ix = x;
            for (int i = 0; i < mats.size(); i++) {
                ItemStack st = mats.get(i);
                matX[i] = ix;
                g.fill(ix, matY, ix + 18, matY + 18, SLOT);
                g.renderItem(st, ix + 1, matY + 1);
                String n = "×" + st.getCount();
                g.drawString(f, n, ix + 21, matY + 5, SUB, false);
                ix += 21 + f.width(n) + 12;
            }
            return boxH + 4 + btnH + 8 + 11 + 20 + 4;
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
            for (int i = 0; i < states.size(); i++) {
                BlockPos p = poss.get(i);
                if (p.getY() >= shownLayers) continue;   // показываем слои 0..shownLayers-1
                ps.pushPose();
                ps.translate(p.getX(), p.getY(), p.getZ());
                br.renderSingleBlock(states.get(i), ps, buf, 0xF000F0, OverlayTexture.NO_OVERLAY);
                ps.popPose();
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
            entries.add(new Entry(GsonHelper.getAsString(eo, "name", ""), els));
        }
        return new Category(name, entries);
    }

    private static Element parseElement(JsonObject o) {
        String type = GsonHelper.getAsString(o, "type", "");
        switch (type) {
            case "header": return new Header(GsonHelper.getAsString(o, "text", ""));
            case "text":   return new Text(GsonHelper.getAsString(o, "text", ""));
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
        return new Multiblock(states, poss, sx, sy, sz);
    }

    private static ItemStack stackOf(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return it == null ? ItemStack.EMPTY : new ItemStack(it);
    }
}
