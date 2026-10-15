package com.jykito.industrialcore.client.drill;

import com.jykito.industrialcore.item.custom.EnergyDrillItem;
import com.jykito.industrialcore.item.custom.IndustrialDrillItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Volumetric drills in hand, on the ground and in item frames (in the inventory the item keeps a flat animated icon:
 * the item model is forge:separate_transforms, the gui view is item/generated). Geometry from the sketch
 * tools/texture_viewer/drills2.html: an octagon = 4 bars rotated 0/45/90/135° around the axis, a "cone" tip
 * of two twisted segments (drills3.html, variant B3). The drill sits in item space like a vanilla tool on an icon: bit diagonally
 * up and to the right, handle at the bottom left, so the item/handheld display settings fit.
 * The bit always spins, and while the player breaks a block the drill shakes slightly (the drills' arm swing is cancelled); the charge lights on the left side of the body (in first person it faces the centre of the screen) light up by energy level; on the industrial one in 3x3 mode
 * three small bits extend.
 */
public final class DrillRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation TEX = new ResourceLocation("industrial_core", "textures/models/drill_parts.png");
    private static final float PX = 1 / 16f;

    /** Atlas tiles 64x64 (16x16, 4 per row), in the same order as drills2.html exportAtlas. */
    enum Mat { BLUE, BLUE_D, CYAN, GRAPH, STEEL, RUBBER, IRON, NETH, LED_ON, LED_OFF, CYAN_GLOW, HOT;
        boolean glow() { return this == LED_ON || this == CYAN_GLOW || this == HOT; } }

    /** A w x h x d bar (pixels) centred at (x, y, z) and rotated around the z axis. */
    private record Cube(float w, float h, float d, float x, float y, float z, float zRot, Mat mat) {}

    /** Drill shape. grip: handle bars (rotated gripAngle around x at gripPivot), hand: where the hand holds it. */
    private static final class Shape {
        final List<Cube> body = new ArrayList<>(), grip = new ArrayList<>(), bit = new ArrayList<>(), leds = new ArrayList<>(),
                sub = new ArrayList<>(), subBit = new ArrayList<>();
        float pivotY, pivotZ, gripAngle = 14, handY, handZ, scale, subRadius, subZ;

        static void oct(List<Cube> to, float a, float z1, float z2, Mat m, float tw) {
            float s = 2 * a * (float) Math.tan(Math.toRadians(22.5)), len = Math.abs(z2 - z1), zc = (z1 + z2) / 2;
            for (float r : new float[]{0, 45, 90, 135}) to.add(new Cube(2 * a, s, len, 0, 0, zc, r + tw, m));
        }
        /** "Cone" tip: segments {apothem, length}, each twisted by twist, with a blunt end tip x tip x tipLen. */
        static void bit(List<Cube> to, float z0, float[][] segs, Mat m, float twist, float tip, float tipLen) {
            float z = z0; int k = 0;
            for (float[] s : segs) { oct(to, s[0], z, z - s[1], m, k * twist); z -= s[1]; k++; }
            to.add(new Cube(tip, tip, tipLen, 0, 0, z - tipLen / 2 + 0.05f, 0, m));
        }
    }

    private static final Shape ELECTRIC = new Shape(), INDUSTRIAL = new Shape();
    static {
        Shape e = ELECTRIC;
        Shape.oct(e.body, 4, -2, 8, Mat.BLUE, 0);            // motor housing
        Shape.oct(e.body, 3.4f, 8, 9, Mat.BLUE_D, 0);        // back cover
        Shape.oct(e.body, 4.15f, 2.6f, 3.2f, Mat.CYAN, 0);   // blue ring
        Shape.oct(e.body, 4.5f, -3, -2, Mat.GRAPH, 0);       // front flange
        Shape.oct(e.body, 3, -5, -3, Mat.GRAPH, 0);          // nose
        Shape.oct(e.body, 2, -8, -5, Mat.STEEL, 0);          // chuck
        Shape.oct(e.body, 2.35f, -6.8f, -6.2f, Mat.STEEL, 22.5f);
        e.pivotY = -3; e.pivotZ = 5.5f;
        e.grip.add(new Cube(3, 9, 3.6f, 0, -4.5f, 0, 0, Mat.RUBBER));
        e.body.add(new Cube(1, 2.2f, 1, 0, -4.4f, 2.6f, 0, Mat.CYAN_GLOW));   // trigger
        e.body.add(new Cube(6, 3, 7, 0, -12.2f, 6.6f, 0, Mat.BLUE_D));        // battery
        e.body.add(new Cube(6.2f, 1, 7.2f, 0, -10.6f, 6.6f, 0, Mat.GRAPH));
        e.body.add(new Cube(0.4f, 1.6f, 4.2f, -4.1f, 0, 5.7f, 0, Mat.GRAPH));    // charge bar on the left side, behind the ring
        for (int i = 0; i < 3; i++) e.leds.add(new Cube(0.4f, 1, 1, -4.25f, 0, 7 - i * 1.3f, 0, Mat.LED_ON));
        e.body.add(new Cube(1, 0.6f, 1, 0, 4.1f, 6.5f, 0, Mat.LED_ON));       // status light
        Shape.bit(e.bit, -8, new float[][]{{1.6f, 2.6f}, {1.1f, 2.2f}}, Mat.IRON, 22.5f, 0.8f, 0.5f);
        e.handY = -7.4f; e.handZ = 4.4f; e.scale = 0.55f;

        Shape n = INDUSTRIAL;
        Shape.oct(n.body, 5, -3, 11, Mat.BLUE, 0);
        for (float z : new float[]{1.5f, 4, 6.5f}) Shape.oct(n.body, 5.6f, z, z + 0.9f, Mat.GRAPH, 0);   // cooling fins
        Shape.oct(n.body, 4.2f, 11, 12.2f, Mat.BLUE_D, 0);
        n.body.add(new Cube(2.4f, 2.4f, 0.4f, 0, 0, 12.3f, 0, Mat.HOT));       // exhaust
        Shape.oct(n.body, 5.4f, -4.2f, -3, Mat.CYAN, 0);
        Shape.oct(n.body, 4, -6.5f, -4.2f, Mat.GRAPH, 0);
        Shape.oct(n.body, 2.8f, -9.5f, -6.5f, Mat.STEEL, 0);
        Shape.oct(n.body, 3.2f, -8.4f, -7.6f, Mat.STEEL, 22.5f);
        n.body.add(new Cube(1.4f, 3.4f, 1.4f, 0, 6.6f, 0.5f, 0, Mat.GRAPH));  // upper handle posts
        n.body.add(new Cube(1.4f, 3.4f, 1.4f, 0, 6.6f, 8, 0, Mat.GRAPH));
        n.body.add(new Cube(2, 1.6f, 9.6f, 0, 8.8f, 4.25f, 0, Mat.RUBBER));
        n.pivotY = -4; n.pivotZ = 8;
        n.grip.add(new Cube(3.6f, 10, 4.2f, 0, -5, 0, 0, Mat.RUBBER));
        n.body.add(new Cube(1.2f, 2.4f, 1.2f, 0, -5.4f, 4.6f, 0, Mat.CYAN_GLOW));
        n.body.add(new Cube(8, 4, 9, 0, -15, 9.4f, 0, Mat.BLUE_D));
        n.body.add(new Cube(8.2f, 1, 9.2f, 0, -12.6f, 9.4f, 0, Mat.GRAPH));
        n.body.add(new Cube(0.4f, 1.9f, 3.7f, -5.1f, 0, -0.75f, 0, Mat.GRAPH));  // charge bar behind the blue ring
        for (int i = 0; i < 4; i++) n.leds.add(new Cube(0.4f, 1.2f, 0.7f, -5.25f, 0, 0.6f - i * 0.9f, 0, Mat.LED_ON));
        n.body.add(new Cube(1.2f, 0.6f, 1.2f, 0, 5.1f, 9.5f, 0, Mat.LED_ON));
        Shape.bit(n.bit, -9.5f, new float[][]{{2.4f, 3.4f}, {1.65f, 3}}, Mat.NETH, 22.5f, 1.1f, 0.6f);
        Shape.oct(n.sub, 0.9f, 0, 2.2f, Mat.STEEL, 0);
        Shape.bit(n.subBit, 0, new float[][]{{0.8f, 1.8f}}, Mat.NETH, 0, 0.5f, 0.4f);
        n.subRadius = 4.2f; n.subZ = -12;
        n.handY = -8.9f; n.handZ = 6.8f; n.scale = 0.48f;
    }

    /**
     * Drill axes (bit −z, top +y, right +x) -> item space: bit diagonally up-right, drill top
     * down-right, right away from the viewer. That way in hand (handheld transforms) the handle goes into the fist and the battery down;
     * with the top up-left the drill was upside down in hand (checked in game 2026-10-04).
     */
    private static final Quaternionf TO_ITEM;
    static {
        float r = (float) (1 / Math.sqrt(2));
        Matrix3f m = new Matrix3f(0, 0, -1,  r, -r, 0,  -r, -r, 0);   // columns: images of +x, +y, +z
        TO_ITEM = new Quaternionf().setFromNormalized(m);
    }

    /** Working: feed forward along the bit axis (model pixels) and the nose tilting down (degrees), smoothly over ~0.1 s. */
    private static final float DIG_PUSH = 3f, DIG_TILT = 7f;
    private static float dig;
    private static long digNanos;

    private static float digAmount(boolean mining) {
        long now = System.nanoTime();
        float dt = digNanos == 0 ? 0 : Math.min(0.1f, (now - digNanos) / 1e9f);
        digNanos = now;
        dig += ((mining ? 1f : 0f) - dig) * Math.min(1f, dt * 10f);
        return dig * dig * (3 - 2 * dig);   // smooth spin-up and stop
    }

    private static DrillRenderer instance;

    public static DrillRenderer get() {
        if (instance == null) instance = new DrillRenderer();
        return instance;
    }

    private DrillRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(stack.getItem() instanceof EnergyDrillItem drill)) return;
        boolean industrial = drill instanceof IndustrialDrillItem;
        Shape s = industrial ? INDUSTRIAL : ELECTRIC;
        Minecraft mc = Minecraft.getInstance();
        boolean mining = mc.player != null && mc.gameMode != null && mc.gameMode.isDestroying() && mc.player.getMainHandItem() == stack;
        float time = (mc.level != null ? mc.level.getGameTime() : 0) + mc.getFrameTime();
        float spin = time * 0.9f;   // the bit always spins
        float charge = drill.getCapacity() > 0 ? EnergyDrillItem.getStoredEnergy(stack) / (float) drill.getCapacity() : 0;

        VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buf, RenderType.entityCutoutNoCull(TEX), false, stack.hasFoil());
        ps.pushPose();
        ps.translate(5.5 * PX, 5.5 * PX, 0.5);          // handle in the same place as a vanilla tool
        ps.mulPose(TO_ITEM);
        ps.scale(s.scale, s.scale, s.scale);
        // while working the drill feeds forward and dips its nose slightly, rotating around the handle (author, 2026-10-10: "tilt while working and a bit forward")
        boolean inHand = ctx.firstPerson() || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        float dig = inHand && mc.player != null && mc.player.getMainHandItem() == stack ? digAmount(mining) : 0;
        if (dig > 0) {
            ps.translate(0, 0, -DIG_PUSH * dig * PX);
            ps.mulPose(Axis.XP.rotationDegrees(-DIG_TILT * dig));
        }
        ps.translate(0, -s.handY * PX, -s.handZ * PX);
        if (mining) ps.translate(Math.sin(time * 7.3) * 0.12 * PX, Math.sin(time * 9.1) * 0.12 * PX, 0);   // shake while drilling

        for (Cube c : s.body) cube(ps, vc, c, light, overlay);
        ps.pushPose();
        ps.translate(0, s.pivotY * PX, s.pivotZ * PX);
        ps.mulPose(Axis.XP.rotationDegrees(s.gripAngle));
        for (Cube c : s.grip) cube(ps, vc, c, light, overlay);
        ps.popPose();
        for (int i = 0; i < s.leds.size(); i++) {
            Cube c = s.leds.get(i);
            cube(ps, vc, charge > (float) i / s.leds.size() + 0.01f ? c : new Cube(c.w, c.h, c.d, c.x, c.y, c.z, 0, Mat.LED_OFF), light, overlay);
        }
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotation(spin));
        for (Cube c : s.bit) cube(ps, vc, c, light, overlay);
        ps.popPose();
        if (industrial && IndustrialDrillItem.getMode(stack) == 1) {   // 3x3 mode: three small bits around the main one
            for (int a : new int[]{90, 210, 330}) {
                ps.pushPose();
                double rad = Math.toRadians(a);
                ps.translate(Math.cos(rad) * s.subRadius * PX, Math.sin(rad) * s.subRadius * PX, s.subZ * PX);
                for (Cube c : s.sub) cube(ps, vc, c, light, overlay);
                ps.mulPose(Axis.ZP.rotation(-spin * 1.3f));
                for (Cube c : s.subBit) cube(ps, vc, c, light, overlay);
                ps.popPose();
            }
        }
        ps.popPose();
    }

    /** Bar: each face takes the tile of its material, 1 texel per model pixel (up to 16). */
    private static void cube(PoseStack ps, VertexConsumer vc, Cube c, int light, int overlay) {
        ps.pushPose();
        ps.translate(c.x * PX, c.y * PX, c.z * PX);
        if (c.zRot != 0) ps.mulPose(Axis.ZP.rotationDegrees(c.zRot));
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        int lt = c.mat.glow() ? LightTexture.FULL_BRIGHT : light;
        float[] half = {c.w * PX / 2, c.h * PX / 2, c.d * PX / 2};
        float[] size = {c.w, c.h, c.d};
        float u0 = (c.mat.ordinal() % 4) * 16, v0 = (c.mat.ordinal() / 4) * 16;
        for (int axis = 0; axis < 3; axis++) {
            int b = (axis + 1) % 3, cc = (axis + 2) % 3;
            float du = Math.min(16, size[b]) / 64f, dv = Math.min(16, size[cc]) / 64f, u = u0 / 64f, v = v0 / 64f;
            for (int sign = -1; sign <= 1; sign += 2) {
                float[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
                float[][] uv = {{u, v + dv}, {u + du, v + dv}, {u + du, v}, {u, v}};
                float nx = axis == 0 ? sign : 0, ny = axis == 1 ? sign : 0, nz = axis == 2 ? sign : 0;
                for (int k = 0; k < 4; k++) {
                    int kk = sign > 0 ? k : 3 - k;   // counter-clockwise winding from outside
                    float[] p = new float[3];
                    p[axis] = sign * half[axis]; p[b] = corners[kk][0] * half[b]; p[cc] = corners[kk][1] * half[cc];
                    vc.vertex(pose, p[0], p[1], p[2]).color(1f, 1f, 1f, 1f).uv(uv[kk][0], uv[kk][1])
                            .overlayCoords(overlay).uv2(lt).normal(normal, nx, ny, nz).endVertex();
                }
            }
        }
        ps.popPose();
    }
}
