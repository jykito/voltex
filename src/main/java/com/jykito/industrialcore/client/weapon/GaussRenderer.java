package com.jykito.industrialcore.client.weapon;

import com.jykito.industrialcore.item.custom.weapon.EnergyWeaponItem;
import com.jykito.industrialcore.item.custom.weapon.GaussRifleItem;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Volumetric gauss rifle G5 "Fork", steel with teal (author, 2026-10-07: "teal, the middle variant"; sketch
 * tools/texture_viewer/weapons3d.html). In the inventory a flat icon (forge:separate_transforms, gui view item/generated).
 * <p>
 * Rifle axes as on the drill: barrel −z, top +y, right +x, and it lies in item space the same way (TO_ITEM from
 * DrillRenderer), so the item model uses the drill displays too. Body: a receiver with a stepped cover and
 * a rail, slots, rivets, a stage indicator, an energy cell, cables, a scope, a trigger guard, a grip, a frame stock;
 * in front two steel rails with coils. While your player holds RMB, an arc grows between the rails (frames from gauss_arc.png
 * every 2 ticks), and the coils, slots and indicator light up by charge stage; the energy cell window lights up when there is enough charge
 * for a shot.
 */
public final class GaussRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation PARTS = ResourceLocation.fromNamespaceAndPath("industrial_core", "textures/models/gauss_parts.png");
    private static final ResourceLocation ARC = ResourceLocation.fromNamespaceAndPath("industrial_core", "textures/models/gauss_arc.png");
    private static final float PX = 1 / 16f;

    /** Atlas tiles 64x64, in the same order as gauss_atlas.js. */
    enum Mat { BODY, DARK, GRIP, TRIM, COIL, STEEL, GLOW, DIM, LAMP, LAMP_OFF;
        boolean glow() { return this == GLOW || this == LAMP; } }

    /** A w x h x d bar (pixels) centred at (x, y, z), rotated around x (xRot). */
    private record Cube(float w, float h, float d, float x, float y, float z, float xRot, Mat mat) {
        Cube(float w, float h, float d, float x, float y, float z, Mat mat) { this(w, h, d, x, y, z, 0, mat); }
        Cube as(Mat m) { return new Cube(w, h, d, x, y, z, xRot, m); }
    }

    private static final List<Cube> BODY = new ArrayList<>();
    /** Lights: slots (lit from stage i % 3 + 1), indicator (from stage i + 1), rail coils (a pair per stage). */
    private static final List<Cube> VENTS = new ArrayList<>(), LEDS = new ArrayList<>(), COILS = new ArrayList<>();
    private static final Cube CELL_WINDOW = new Cube(3.2f, 0.7f, 3.6f, 0, -3.5f, 1.2f, Mat.GLOW);
    private static final float ARC_LEN = 20, ARC_W = 3, ARC_Z = -2.4f;
    private static final int ARC_FRAMES = 16;
    static {
        List<Cube> b = BODY;
        b.add(new Cube(5, 4, 11, 0, 1.5f, 4, Mat.BODY)); b.add(new Cube(4, 1, 10, 0, 3.9f, 4.2f, Mat.BODY)); b.add(new Cube(4.4f, 0.4f, 10.4f, 0, 3.4f, 4.2f, Mat.TRIM));   // the strip sticks out from under the cover noticeably, not by 0.05
        b.add(new Cube(1.6f, 0.7f, 9, 0, 4.7f, 4, Mat.DARK)); for (int i = 0; i < 5; i++) b.add(new Cube(2, 0.4f, 0.6f, 0, 5.1f, 0.6f + i * 1.7f, Mat.DARK));
        b.add(new Cube(4.6f, 2.6f, 9, 0, -1.6f, 4.5f, Mat.DARK)); b.add(new Cube(4.2f, 4.6f, 1.6f, 0, 1, -1.2f, Mat.DARK)); b.add(new Cube(3.2f, 3.2f, 0.6f, 0, 1, -2.1f, Mat.STEEL));
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int i = 0; i < 4; i++) VENTS.add(new Cube(0.25f, 2, 0.5f, sx * 2.55f, 1.6f, 0.5f + i * 1.2f, Mat.GLOW));
            for (float[] yz : new float[][]{{3.1f, 6.2f}, {3.1f, 9.2f}, {-0.1f, 6.2f}, {-0.1f, 9.2f}}) b.add(new Cube(0.3f, 0.45f, 0.45f, sx * 2.65f, yz[0], yz[1], Mat.STEEL));
            b.add(new Cube(0.2f, 2.4f, 3.2f, sx * 2.6f, 1.4f, 7.7f, Mat.DARK));   // the panel is thinner, so the rivets stand out above it
            b.add(new Cube(0.9f, 0.9f, 9, sx * 1.4f, -1.3f, -5, Mat.GRIP)); b.add(new Cube(1.1f, 1.1f, 0.6f, sx * 1.4f, -1.3f, -0.6f, Mat.STEEL));
        }
        for (int i = 0; i < 3; i++) LEDS.add(new Cube(0.3f, 0.7f, 0.7f, -2.75f, 1.6f, 6.9f + i * 0.9f, Mat.LAMP));
        b.add(new Cube(3, 3, 4.4f, 0, -3.9f, 1.2f, Mat.DARK)); b.add(new Cube(3.2f, 0.5f, 4.6f, 0, -5.5f, 1.2f, Mat.STEEL));
        b.add(new Cube(1.8f, 1.8f, 3, 0, 5.9f, 3, Mat.DARK)); b.add(new Cube(1.3f, 1.3f, 0.3f, 0, 5.9f, 1.4f, Mat.LAMP)); b.add(new Cube(1.9f, 0.5f, 1, 0, 5, 2, Mat.STEEL));
        b.add(new Cube(0.8f, 0.6f, 3.2f, 0, -3.4f, 5.6f, Mat.DARK)); b.add(new Cube(0.8f, 2.2f, 0.6f, 0, -2.6f, 4.1f, Mat.DARK));
        b.add(new Cube(2.6f, 6, 3, 0, -5.2f, 6.6f, -15, Mat.GRIP)); b.add(new Cube(2.9f, 0.8f, 3.3f, 0, -8.1f, 7.4f, -15, Mat.DARK));
        b.add(new Cube(1.4f, 1.2f, 7, 0, 2.2f, 12.6f, Mat.DARK)); b.add(new Cube(1.4f, 1.2f, 6.4f, 0, -1.8f, 12.4f, -9, Mat.DARK)); b.add(new Cube(2.6f, 1.6f, 4, 0, 3.1f, 12.2f, Mat.BODY));
        b.add(new Cube(3, 6.4f, 1.4f, 0, 0, 16.4f, Mat.BODY)); b.add(new Cube(3.2f, 6.6f, 0.6f, 0, 0, 17.3f, Mat.GRIP));
        // fork: two steel rails with six coils each, with glowing strips
        for (float[] ys : new float[][]{{3.2f, 1}, {-1.6f, -1}}) {
            float y = ys[0], s = ys[1];
            b.add(new Cube(1.8f, 1.4f, 21.3f, 0, y, -12.35f, Mat.STEEL));   // the rail goes into the front block b.add(new Cube(2.4f, 1.6f, 2, 0, y + s * 0.4f, -23, Mat.DARK));
            for (int i = 0; i < 6; i++) { float z = -4.5f - i * 3.2f;
                b.add(new Cube(2.6f, 0.8f, 1.6f, 0, y + s * 1.05f, z, Mat.COIL)); COILS.add(new Cube(2.7f, 0.4f, 1.7f, 0, y + s * 1.4f, z, Mat.GLOW)); }   // the coil is sunk into the rail, the strip is above the coil
        }
    }

    /** Rifle axes -> item space, as on the drill (DrillRenderer.TO_ITEM): barrel diagonally up-right. */
    private static final Quaternionf TO_ITEM;
    static {
        float r = (float) (1 / Math.sqrt(2));
        TO_ITEM = new Quaternionf().setFromNormalized(new Matrix3f(0, 0, -1, r, -r, 0, -r, -r, 0));
    }

    /**
     * In the slot, pose G "tilted profile" (the author picked it from four in weapons3d.html, POSES): slot rotations in order
     * Y −72°, X 10°, Z 14°. Scale from the rifle bounds (x ±2.8, y −8.8..6.8, z −24..17.6) in this pose, with 6% margins.
     */
    private static final Quaternionf GUI_POSE = new Quaternionf().rotationZ((float) Math.toRadians(14)).rotateX((float) Math.toRadians(10)).rotateY((float) Math.toRadians(-72));
    private static final float[] GUI_C = { 0, -1, -3.2f };
    private static final float GUI_SCALE;
    static {
        float w = 0, h = 0;
        for (int i = 0; i < 8; i++) {
            org.joml.Vector3f p = new org.joml.Vector3f((i & 1) == 0 ? -2.8f : 2.8f, ((i & 2) == 0 ? -8.8f : 6.8f) - GUI_C[1], ((i & 4) == 0 ? -24 : 17.6f) - GUI_C[2]);
            GUI_POSE.transform(p); w = Math.max(w, Math.abs(p.x)); h = Math.max(h, Math.abs(p.y));
        }
        GUI_SCALE = 0.94f * 8 / Math.max(w, h);   // half the cell is 8 model px at scale 1
    }

    private static GaussRenderer instance;

    public static GaussRenderer get() {
        if (instance == null) instance = new GaussRenderer();
        return instance;
    }

    private GaussRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(stack.getItem() instanceof GaussRifleItem)) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        int stage = p != null && p.isUsingItem() && p.getUseItem() == stack ? GaussRifleItem.stage(p.getTicksUsingItem(), GaussRifleItem.fullPlasma(p)) : 0;
        boolean ready = EnergyWeaponItem.getEnergy(stack) >= GaussRifleItem.COST[1];
        float time = (mc.level != null ? mc.level.getGameTime() : 0) + mc.getTimer().getGameTimeDeltaPartialTick(true);
        int frame = ((int) (time / 2)) % ARC_FRAMES;

        ps.pushPose();
        if (ctx == ItemDisplayContext.GUI) {            // 3D in the slot in pose G "tilted profile", fitted into the cell centre
            ps.translate(0.5, 0.5, 0.5);
            ps.mulPose(GUI_POSE);
            ps.scale(GUI_SCALE, GUI_SCALE, GUI_SCALE);
            ps.translate(-GUI_C[0] * PX, -GUI_C[1] * PX, -GUI_C[2] * PX);
        } else {
            ps.translate(5.5 * PX, 5.5 * PX, 0.5);      // handle in the same place as a vanilla tool
            ps.mulPose(TO_ITEM);
            ps.scale(0.62f, 0.62f, 0.62f);              // author: "make the gun bigger" (was 0.42)
            ps.translate(0, 6 * PX, -6.8f * PX);        // grip on the handle
        }

        VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buf, RenderType.entityCutoutNoCull(PARTS), false, stack.hasFoil());
        for (Cube c : BODY) cube(ps, vc, c, light, overlay);
        for (int i = 0; i < VENTS.size(); i++) cube(ps, vc, stage > i % 3 ? VENTS.get(i) : VENTS.get(i).as(Mat.DIM), light, overlay);
        for (int i = 0; i < LEDS.size(); i++) cube(ps, vc, stage > i ? LEDS.get(i) : LEDS.get(i).as(Mat.LAMP_OFF), light, overlay);
        for (int i = 0; i < COILS.size(); i++) cube(ps, vc, stage * 2 > i % 6 ? COILS.get(i) : COILS.get(i).as(Mat.DIM), light, overlay);
        cube(ps, vc, ready ? CELL_WINDOW : CELL_WINDOW.as(Mat.DIM), light, overlay);
        if (stage > 0) {   // the arc in its own buffer after all the metal
            VertexConsumer arc = buf.getBuffer(RenderType.entityTranslucentEmissive(ARC));
            float len = ARC_LEN * stage / 3f;
            arcQuad(ps, arc, 0, frame, len, 2.5f, -1, overlay);
            arcQuad(ps, arc, 1, frame, len, -0.9f, 1, overlay);
        }
        ps.popPose();
    }

    /** Arc sheet in the plane x = 0: from the rail (y0) by ARC_W towards the middle (dir), from the body to the muzzle by len; a frame from the 8x320 strip. */
    private static void arcQuad(PoseStack ps, VertexConsumer vc, int sheet, int frame, float len, float y0, int dir, int overlay) {
        Matrix4f pose = ps.last().pose();
        PoseStack.Pose normal = ps.last();
        float u0 = sheet * ARC_W / 8f, u1 = (sheet * ARC_W + ARC_W) / 8f;
        float v0 = frame * ARC_LEN / 320f, v1 = (frame * ARC_LEN + len) / 320f;
        float ya = y0 * PX, yb = (y0 + dir * ARC_W) * PX, za = ARC_Z * PX, zb = (ARC_Z - len) * PX;
        float[][] p = {{ya, za}, {yb, za}, {yb, zb}, {ya, zb}}, uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
        for (int k = 0; k < 4; k++)
            vc.addVertex(pose, 0, p[k][0], p[k][1]).setColor(1f, 1f, 1f, 1f).setUv(uv[k][0], uv[k][1])
                    .setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(normal, 1, 0, 0);
    }

    /** Bar: each face takes the tile of its material, 1 texel per model pixel (up to 16). */
    private static void cube(PoseStack ps, VertexConsumer vc, Cube c, int light, int overlay) {
        ps.pushPose();
        ps.translate(c.x * PX, c.y * PX, c.z * PX);
        if (c.xRot != 0) ps.mulPose(Axis.XP.rotationDegrees(c.xRot));
        Matrix4f pose = ps.last().pose();
        PoseStack.Pose normal = ps.last();
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
                    vc.addVertex(pose, p[0], p[1], p[2]).setColor(1f, 1f, 1f, 1f).setUv(uv[kk][0], uv[kk][1])
                            .setOverlay(overlay).setLight(lt).setNormal(normal, nx, ny, nz);
                }
            }
        }
        ps.popPose();
    }
}
