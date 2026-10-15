package com.jykito.industrialcore.client.weapon;
import com.mojang.blaze3d.vertex.PoseStack;


import net.neoforged.fml.common.EventBusSubscriber;
import com.jykito.industrialcore.IndustrialCore;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Gauss shot on the client (author, 2026-10-07: "give the gun a proper shot instead of crappy particles"): a beam from the muzzle to
 * the hit point, a glowing core with a glow around it, thickness by stage; around the beam a spiral of pixel squares
 * that spreads while the beam fades; a flash at the muzzle and at the hit point (bigger for the plasma shot). Everything glows
 * additively and fades within a few ticks. For the shooter, recoil: the camera kicks up by stage.
 */
@EventBusSubscriber(modid = IndustrialCore.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class GaussBeams {

    private record Beam(Vec3 from, Vec3 to, int stage, boolean plasma, boolean blast, float born) {}
    private static final List<Beam> BEAMS = new ArrayList<>();

    private GaussBeams() {}

    public static void add(Vec3 from, Vec3 to, int stage, boolean plasma, boolean blast, int shooter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BEAMS.add(new Beam(from, to, stage, plasma, blast, mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(true)));
        if (mc.player != null && mc.player.getId() == shooter) mc.player.setXRot(mc.player.getXRot() - (1.5f + 1.5f * stage));   // recoil
    }

    private static float life(Beam b) { return 8 + 2 * b.stage; }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BEAMS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { BEAMS.clear(); return; }
        float now = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(true);
        BEAMS.removeIf(b -> now - b.born > life(b) || now < b.born - 1);
        if (BEAMS.isEmpty()) return;

        Camera cam = event.getCamera();
        Vec3 cp = cam.getPosition();
        // points relative to the camera, computed in double: in world floats far from zero the beam would jitter
        Matrix4f m = event.getPoseStack().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);   // glow adds up
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Vector3f left = cam.getLeftVector(), up = cam.getUpVector();
        for (Beam b : BEAMS) draw(buf, m, b, (now - b.born) / life(b), cp, left, up);
        com.mojang.blaze3d.vertex.MeshData mesh = buf.build();
        if (mesh != null) com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void draw(BufferBuilder buf, Matrix4f m, Beam b, float age, Vec3 cp, Vector3f left, Vector3f up) {
        float a = (float) Math.pow(1 - Math.max(0, Math.min(1, age)), 1.5), st = b.stage;
        float[] outer = b.plasma ? new float[]{0.18f, 0.93f, 0f} : new float[]{0f, 1f, 0.8f};
        float[] mid = b.plasma ? new float[]{0.65f, 1f, 0.45f} : new float[]{0.5f, 1f, 0.9f};
        Vec3 from = b.from.subtract(cp), to = b.to.subtract(cp), d = to.subtract(from);
        double len = d.length();
        if (len < 1e-3) return;
        Vec3 dir = d.scale(1 / len);
        // beam: three ribbons facing the camera, a wide glow (widens as it fades), a middle, a white core
        Vec3 side = dir.cross(from.add(to).scale(0.5)).normalize();
        ribbon(buf, m, from, to, side, (0.05f + 0.06f * st) * (1 + age * 1.5f), outer, 0.35f * a);
        ribbon(buf, m, from, to, side, 0.025f + 0.025f * st, mid, 0.75f * a);
        ribbon(buf, m, from, to, side, 0.012f + 0.01f * st, new float[]{0.92f, 1f, 1f}, a);
        // spiral: pixel squares around the beam, the radius grows while the beam fades
        Vec3 u = Math.abs(dir.y) < 0.9 ? dir.cross(new Vec3(0, 1, 0)).normalize() : dir.cross(new Vec3(1, 0, 0)).normalize(), v = dir.cross(u);
        float r = 0.08f + 0.04f * st + age * 0.25f, sq = 0.035f + 0.01f * st;
        for (double t = 0.3; t < len; t += 0.12) {
            double ang = t * 5.5;
            Vec3 p = from.add(dir.scale(t)).add(u.scale(Math.cos(ang) * r)).add(v.scale(Math.sin(ang) * r));
            billboard(buf, m, p, left, up, sq, mid, 0.85f * a);
        }
        // flashes: short at the muzzle, bigger at the hit point (huge for the plasma shot)
        float flashA = Math.max(0, 1 - age * 2.5f);
        if (flashA > 0) {
            billboard(buf, m, from, left, up, (0.12f + 0.05f * st) * (1 + age), mid, flashA);
            float hit = (b.blast ? 1.4f : 0.2f + 0.12f * st) * (1 + age * 2);
            billboard(buf, m, to, left, up, hit, outer, 0.6f * flashA);
            billboard(buf, m, to, left, up, hit * 0.45f, new float[]{0.92f, 1f, 1f}, flashA);
        }
    }

    private static void ribbon(BufferBuilder buf, Matrix4f m, Vec3 a, Vec3 b, Vec3 side, float w, float[] c, float alpha) {
        Vec3 s = side.scale(w);
        vtx(buf, m, a.add(s), c, alpha); vtx(buf, m, b.add(s), c, alpha); vtx(buf, m, b.subtract(s), c, alpha); vtx(buf, m, a.subtract(s), c, alpha);
    }

    private static void billboard(BufferBuilder buf, Matrix4f m, Vec3 p, Vector3f left, Vector3f up, float size, float[] c, float alpha) {
        Vec3 l = new Vec3(left.x(), left.y(), left.z()).scale(size), u = new Vec3(up.x(), up.y(), up.z()).scale(size);
        vtx(buf, m, p.add(l).add(u), c, alpha); vtx(buf, m, p.subtract(l).add(u), c, alpha);
        vtx(buf, m, p.subtract(l).subtract(u), c, alpha); vtx(buf, m, p.add(l).subtract(u), c, alpha);
    }

    private static void vtx(BufferBuilder buf, Matrix4f m, Vec3 p, float[] c, float alpha) {
        buf.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(c[0], c[1], c[2], alpha);
    }
}
