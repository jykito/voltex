package com.jykito.industrialcore.client.wind;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turning parts of the wind turbine nacelle from {@code models/wind_turbine.json}: ready quads per group (body, rotor, cups)
 * and the pivots of the groups. The file and the atlas are exported by tools/texture_viewer/windmill.html from the same
 * model the sketch draws; the static parts (platform, mast sections) are OBJ block models from the same export.
 */
public final class WindTurbineModel {

    public static final ResourceLocation TEX = new ResourceLocation(IndustrialCore.MODID, "textures/block/wind_turbine.png");
    /** Only the cyan tile of the atlas: the second pass that makes the status strips glow. */
    public static final ResourceLocation TEX_GLOW = new ResourceLocation(IndustrialCore.MODID, "textures/models/wind_turbine_glow.png");
    private static final ResourceLocation SPEC = new ResourceLocation(IndustrialCore.MODID, "models/wind_turbine.json");
    private static final Logger LOGGER = LogUtils.getLogger();
    /** How far the glowing layer is pushed out of the face, so it does not flicker with the normal one. */
    private static final float GLOW_LIFT = 0.002f;

    /** Four vertices (x, y, z) counter-clockwise from outside, their UVs, the normal and whether the quad glows. */
    private record Quad(float[] pos, float[] uv, float nx, float ny, float nz, boolean glow) {}

    private static Map<String, List<Quad>> groups;
    private static Map<String, float[]> pivots = new HashMap<>();

    private WindTurbineModel() {}

    /** Reset after a resource reload. */
    public static void clear() { groups = null; }

    /** Pivot of a group in blocks: yaw from the bottom centre of the nacelle block, rotor and cups from the yaw pivot. */
    public static float[] pivot(String name) {
        groups();
        return pivots.getOrDefault(name, new float[3]);
    }

    private static Map<String, List<Quad>> groups() {
        if (groups == null) groups = load();
        return groups;
    }

    private static Map<String, List<Quad>> load() {
        Map<String, List<Quad>> out = new HashMap<>();
        try (Reader r = Minecraft.getInstance().getResourceManager().openAsReader(SPEC)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            Map<String, float[]> pv = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("pivots").entrySet()) pv.put(e.getKey(), floats(e.getValue().getAsJsonArray()));
            pivots = pv;
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("groups").entrySet()) {
                List<Quad> list = new ArrayList<>();
                for (JsonElement el : e.getValue().getAsJsonArray()) {
                    JsonObject q = el.getAsJsonObject();
                    float[] n = floats(q.getAsJsonArray("n"));
                    list.add(new Quad(floats(q.getAsJsonArray("p")), floats(q.getAsJsonArray("uv")), n[0], n[1], n[2], q.get("glow").getAsBoolean()));
                }
                out.put(e.getKey(), list);
            }
        } catch (Exception e) {
            LOGGER.error("Wind turbine model {} not loaded", SPEC, e);
        }
        return out;
    }

    private static float[] floats(JsonArray a) {
        float[] f = new float[a.size()];
        for (int i = 0; i < f.length; i++) f[i] = a.get(i).getAsFloat();
        return f;
    }

    /** Draw a group in the current pose with the world light; glowing quads too, the glow comes in {@link #renderGlow}. */
    public static void render(String group, PoseStack ps, VertexConsumer vc, int light, int overlay) {
        render(group, ps, vc, light, overlay, 0xFFFFFF);
    }

    /** The same with an RGB tint over the texture (a pack's rotor). */
    public static void render(String group, PoseStack ps, VertexConsumer vc, int light, int overlay, int tint) {
        List<Quad> qs = groups().get(group);
        if (qs == null) return;
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        float r = (tint >> 16 & 255) / 255f, g = (tint >> 8 & 255) / 255f, b = (tint & 255) / 255f;
        for (Quad q : qs) quad(q, pose, normal, vc, light, overlay, 0, r, g, b);
    }

    /** Second pass over the glowing quads with {@link #TEX_GLOW} into the eyes buffer; {@code strength} 0..1 dims it. */
    public static void renderGlow(String group, PoseStack ps, VertexConsumer vc, int overlay, float strength) {
        List<Quad> qs = groups().get(group);
        if (qs == null) return;
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        for (Quad q : qs) if (q.glow()) quad(q, pose, normal, vc, LightTexture.FULL_BRIGHT, overlay, GLOW_LIFT, strength, strength, strength);
    }

    private static void quad(Quad q, Matrix4f pose, Matrix3f normal, VertexConsumer vc, int light, int overlay, float lift, float r, float g, float b) {
        float ox = q.nx() * lift, oy = q.ny() * lift, oz = q.nz() * lift;
        for (int i = 0; i < 4; i++)
            vc.vertex(pose, q.pos()[i * 3] + ox, q.pos()[i * 3 + 1] + oy, q.pos()[i * 3 + 2] + oz).color(r, g, b, 1f)
                    .uv(q.uv()[i * 2], q.uv()[i * 2 + 1]).overlayCoords(overlay).uv2(light)
                    .normal(normal, q.nx(), q.ny(), q.nz()).endVertex();
    }
}
