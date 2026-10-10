package com.jykito.industrialcore.client.miner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jykito.industrialcore.IndustrialCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.*;

/**
 * Модель промышленного шахтёра и бурового маяка из {@code models/industrial_miner.json} — его пишет
 * tools/texture_viewer/export_industrial_miner.js из того же описания, что рисует эскиз miner_concept.html.
 * <p>
 * Коробки по группам (static, drill, ring, head, dish, beacon, crystal — движение задают BER). Плотность текстуры
 * 8 текселей на блок, как у модели старого шахтёра: у коробки с {@code uv} каждая грань берёт свой прямоугольник
 * атласа целиком; без него грань режется на клетки по 2 блока (плитка атласа 16×16), у материалов из {@code fit}
 * плитка растягивается на всю грань. Материалы из {@code glow} светятся на полной яркости.
 */
public final class IndustrialMinerModel {

    public static final ResourceLocation TEX = new ResourceLocation(IndustrialCore.MODID, "textures/models/industrial_miner.png");
    /** Только зелёные пиксели атласа (tools/texture_viewer/miner_glow_mask.js): светится зелёное, а не всё ядро. */
    public static final ResourceLocation TEX_GLOW = new ResourceLocation(IndustrialCore.MODID, "textures/models/industrial_miner_glow.png");
    private static final ResourceLocation SPEC = new ResourceLocation(IndustrialCore.MODID, "models/industrial_miner.json");
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Четыре вершины (x, y, z), их UV, нормаль и свечение. */
    private record Quad(float[] pos, float[] uv, float nx, float ny, float nz, boolean glow) {}

    private static Map<String, List<Quad>> groups;

    private IndustrialMinerModel() {}

    /** Сбросить после перезагрузки ресурсов. */
    public static void clear() { groups = null; }

    /** UV центра плитки материала (для волн маяка). */
    public static float[] tileCenter(String material) {
        int[] t = tiles().getOrDefault(material, new int[]{0, 0});
        return new float[]{(t[0] + 8) / (float) atlas, (t[1] + 8) / (float) atlas};
    }

    private static int atlas = 64;
    private static Map<String, int[]> tileMap = new HashMap<>();
    private static Map<String, int[]> tiles() { groups(); return tileMap; }

    private static Map<String, List<Quad>> groups() {
        if (groups == null) groups = load();
        return groups;
    }

    private static Map<String, List<Quad>> load() {
        Map<String, List<Quad>> out = new HashMap<>();
        try (Reader r = Minecraft.getInstance().getResourceManager().openAsReader(SPEC)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            atlas = root.get("atlas").getAsInt();
            float texels = root.get("texels").getAsFloat();
            Map<String, int[]> t = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("tiles").entrySet()) {
                JsonArray a = e.getValue().getAsJsonArray();
                t.put(e.getKey(), new int[]{a.get(0).getAsInt(), a.get(1).getAsInt()});
            }
            tileMap = t;
            Set<String> glow = strings(root.getAsJsonArray("glow")), fit = strings(root.getAsJsonArray("fit"));
            for (JsonElement el : root.getAsJsonArray("boxes")) {
                JsonObject b = el.getAsJsonObject();
                String m = b.get("m").getAsString();
                float ry = b.has("ry") ? b.get("ry").getAsFloat() : 0;
                float[][] uv = null;
                if (b.has("uv")) {
                    JsonArray a = b.getAsJsonArray("uv");
                    uv = new float[6][];
                    for (int i = 0; i < 6; i++) {
                        JsonArray rect = a.get(i).getAsJsonArray();
                        uv[i] = new float[]{rect.get(0).getAsFloat(), rect.get(1).getAsFloat(), rect.get(2).getAsFloat(), rect.get(3).getAsFloat()};
                    }
                }
                addBox(out.computeIfAbsent(b.get("g").getAsString(), k -> new ArrayList<>()), vec(b, "f"), vec(b, "t"), ry,
                        t.getOrDefault(m, new int[]{0, 0}), uv, glow.contains(m), fit.contains(m), texels);
            }
        } catch (Exception e) {
            LOGGER.error("Industrial miner model {} not loaded", SPEC, e);
        }
        return out;
    }

    private static Set<String> strings(JsonArray a) {
        Set<String> s = new HashSet<>();
        for (JsonElement e : a) s.add(e.getAsString());
        return s;
    }

    private static float[] vec(JsonObject o, String key) {
        JsonArray a = o.getAsJsonArray(key);
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    /**
     * Грани коробки: левый верхний угол (если смотреть снаружи), направления «вправо» и «вниз», размеры.
     * Порядок вершин TL, BL, BR, TR — против часовой снаружи.
     * uv — если задан, у каждой грани свой прямоугольник атласа {u, v, ширина, высота} на всю грань
     * (порядок граней как в faces ниже); иначе плитка материала повтором по клеткам в 2 блока.
     */
    private static void addBox(List<Quad> to, float[] f, float[] t, float ry, int[] tile, float[][] faceUv, boolean glow, boolean fit, float texels) {
        float x1 = f[0], y1 = f[1], z1 = f[2], x2 = t[0], y2 = t[1], z2 = t[2];
        float[][] faces = {
                // TL               вправо       вниз         нормаль   ширина   высота
                {x2, y2, z2,        0, 0, -1,    0, -1, 0,    1, 0, 0,  z2 - z1, y2 - y1},   // восток
                {x1, y2, z1,        0, 0, 1,     0, -1, 0,    -1, 0, 0, z2 - z1, y2 - y1},   // запад
                {x1, y2, z2,        1, 0, 0,     0, -1, 0,    0, 0, 1,  x2 - x1, y2 - y1},   // юг
                {x2, y2, z1,        -1, 0, 0,    0, -1, 0,    0, 0, -1, x2 - x1, y2 - y1},   // север
                {x1, y2, z1,        1, 0, 0,     0, 0, 1,     0, 1, 0,  x2 - x1, z2 - z1},   // верх
                {x1, y1, z2,        1, 0, 0,     0, 0, -1,    0, -1, 0, x2 - x1, z2 - z1}};  // низ
        float step = 16f / texels, cos = (float) Math.cos(Math.toRadians(ry)), sin = (float) Math.sin(Math.toRadians(ry));
        for (int fi = 0; fi < faces.length; fi++) {
            float[] fc = faces[fi], rect = faceUv == null ? null : faceUv[fi];
            float A = fc[12], B = fc[13];
            if (A <= 0 || B <= 0) continue;
            boolean whole = fit || rect != null;
            float sa = whole ? A : step, sb = whole ? B : step;
            for (float a0 = 0; a0 < A - 1e-4f; a0 += sa)
                for (float b0 = 0; b0 < B - 1e-4f; b0 += sb) {
                    float a1 = Math.min(A, a0 + sa), b1 = Math.min(B, b0 + sb);
                    float u1 = rect != null ? rect[2] : fit ? 16 : (a1 - a0) * texels, v1 = rect != null ? rect[3] : fit ? 16 : (b1 - b0) * texels;
                    float[] pos = new float[12];
                    float[][] ab = {{a0, b0}, {a0, b1}, {a1, b1}, {a1, b0}};
                    for (int i = 0; i < 4; i++) {
                        float px = fc[0] + fc[3] * ab[i][0] + fc[6] * ab[i][1];
                        float py = fc[1] + fc[4] * ab[i][0] + fc[7] * ab[i][1];
                        float pz = fc[2] + fc[5] * ab[i][0] + fc[8] * ab[i][1];
                        pos[i * 3] = px * cos + pz * sin;      // поворот вокруг Y как у three.js: (0,0,1) -> (sin, 0, cos)
                        pos[i * 3 + 1] = py;
                        pos[i * 3 + 2] = -px * sin + pz * cos;
                    }
                    float tu = rect != null ? rect[0] : tile[0], tv = rect != null ? rect[1] : tile[1];
                    float[] uv = {tu / atlas, tv / atlas, tu / atlas, (tv + v1) / atlas, (tu + u1) / atlas, (tv + v1) / atlas, (tu + u1) / atlas, tv / atlas};
                    float nx = fc[9] * cos + fc[11] * sin, nz = -fc[9] * sin + fc[11] * cos;
                    to.add(new Quad(pos, uv, nx, fc[10], nz, glow));
                }
        }
    }

    /** Нарисовать группу в текущей позе. lit = false гасит свечение (маяк без линзы). */
    public static void render(String group, PoseStack ps, VertexConsumer vc, int light, int overlay, boolean lit) {
        List<Quad> qs = groups().get(group);
        if (qs == null) return;
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        for (Quad q : qs) quad(q, pose, normal, vc, q.glow() && lit ? LightTexture.FULL_BRIGHT : light, overlay);
    }

    /** На сколько светящийся слой вынесен из грани наружу: без зазора он мерцает с обычным и проигрывает ему при смене порядка проходов. */
    private static final float GLOW_LIFT = 0.002f;

    /**
     * Второй проход по светящимся материалам текстурой {@link #TEX_GLOW} (только зелёное), в буфер {@code RenderType.eyes}:
     * свет мира его не гасит, а шейдеры (Oculus) рисуют его как светящееся. Первый проход делается с {@code lit = false}.
     */
    public static void renderGlow(String group, PoseStack ps, VertexConsumer vc, int overlay) {
        List<Quad> qs = groups().get(group);
        if (qs == null) return;
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        for (Quad q : qs) if (q.glow()) quad(q, pose, normal, vc, LightTexture.FULL_BRIGHT, overlay, GLOW_LIFT);
    }

    private static void quad(Quad q, Matrix4f pose, Matrix3f normal, VertexConsumer vc, int lt, int overlay) {
        quad(q, pose, normal, vc, lt, overlay, 0);
    }

    private static void quad(Quad q, Matrix4f pose, Matrix3f normal, VertexConsumer vc, int lt, int overlay, float lift) {
        float ox = q.nx() * lift, oy = q.ny() * lift, oz = q.nz() * lift;
        for (int i = 0; i < 4; i++)
            vc.vertex(pose, q.pos()[i * 3] + ox, q.pos()[i * 3 + 1] + oy, q.pos()[i * 3 + 2] + oz).color(1f, 1f, 1f, 1f)
                    .uv(q.uv()[i * 2], q.uv()[i * 2 + 1]).overlayCoords(overlay).uv2(lt)
                    .normal(normal, q.nx(), q.ny(), q.nz()).endVertex();
    }
}
