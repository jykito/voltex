package com.jykito.industrialcore.client.weapon;

import com.jykito.industrialcore.item.custom.weapon.EnergyBladeItem;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Volumetric energy blade B5 "Curve" in hand, on the ground and in item frames (in the inventory a flat icon: the item model is
 * forge:separate_transforms, the gui view is item/generated). Author, 2026-10-07: "the blade has a metal spine behind the edge,
 * and the edge itself comes out like gas"; the curve was chosen from the variants in tools/texture_viewer/weapons3d.html.
 * <p>
 * Blade axes: blade +y, spine at the back (−x), gas forward (+x), thickness along z. The spine is seven segments, each turned 4°
 * towards the spine, curved like a katana. The gas is a sheet in front of the spine, frames from energy_blade_gas.png flip every 2 ticks, it glows
 * and is see-through; without charge there is no gas and the nozzle slit goes dark. In item space the blade lies like a vanilla sword on its icon:
 * handle at the bottom left, tip at the top right, so the item/handheld displays fit.
 */
public final class BladeRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation PARTS = new ResourceLocation("industrial_core", "textures/models/energy_blade_parts.png");
    private static final ResourceLocation GAS = new ResourceLocation("industrial_core", "textures/models/energy_blade_gas.png");
    private static final float PX = 1 / 16f;

    /** Atlas tiles 64x64, in the same order as blade_atlas.js. */
    enum Mat { BODY, DARK, GRIP, TRIM, STEEL, LAMP, SLIT_ON, SLIT_OFF;
        boolean glow() { return this == LAMP || this == SLIT_ON; } }

    /** A w x h x d bar (pixels) centred at (x, y, z), rotated around y (yRot), then around z (zRot). */
    private record Cube(float w, float h, float d, float x, float y, float z, float yRot, float zRot, Mat mat) {
        Cube(float w, float h, float d, float x, float y, float z, Mat mat) { this(w, h, d, x, y, z, 0, 0, mat); }
    }

    private static final int SEGMENTS = 7, GAS_FRAMES = 16;   // author: "make the blade longer" (was 5)
    private static final float SEG_LEN = 4.6f, SEG_BEND = 4f, GAS_W = 6f, GAS_H = 5f, GAS_X = 0.25f, GAS_TW = 48f;
    /** In the slot: middle of the sabre (from pommel to the tip with gas, x −8.1..6.3, y −11..35.2) and a scale so the length lies along the cell diagonal. */
    private static final float GUI_CX = -0.9f, GUI_CY = 12.1f, GUI_SCALE = 0.45f;
    private static final List<Cube> HILT = new ArrayList<>(), SEG = new ArrayList<>(), SEG_ODD = new ArrayList<>(), TIP = new ArrayList<>();
    static {
        // octagonal handle, pommel with a light, guard with trim
        float a = 1.25f, s = 2 * a * (float) Math.tan(Math.toRadians(22.5));
        for (float r : new float[]{0, 45, 90, 135}) HILT.add(new Cube(2 * a, 8.8f, s, 0, -5, 0, r, 0, Mat.GRIP));   // ends inside the pommel and guard: no shared faces
        HILT.add(new Cube(3.2f, 2, 3.2f, 0, -10, 0, Mat.DARK));
        HILT.add(new Cube(1, 1, 3.4f, 0, -10.2f, 0, Mat.LAMP));
        HILT.add(new Cube(8, 2, 3.2f, 1.5f, 0, 0, Mat.BODY));
        HILT.add(new Cube(8.2f, 0.6f, 3.4f, 1.5f, 0.6f, 0, Mat.TRIM));
        HILT.add(new Cube(2.4f, 1.4f, 2.6f, -0.5f, 1.6f, 0, Mat.DARK));   // back face not in the spine plane
        // spine segment: steel, trim on the sides; the nozzle slit is added separately (lit by charge).
        // Against face flicker: the steel is 0.6 longer than the segment and goes into the next one, strips are shorter than the steel and 0.15 wider;
        // segments bend around z, so their side faces lie in one plane with the neighbours; on odd ones everything is 0.05-0.1 thicker
        for (List<Cube> to : List.of(SEG, SEG_ODD)) {
            boolean odd = to == SEG_ODD;
            float d = odd ? 2.3f : 2.2f, tz = odd ? 1.27f : 1.2f;
            to.add(new Cube(1.6f, SEG_LEN + 0.6f, d, -0.8f, SEG_LEN / 2, 0, Mat.STEEL));
            to.add(new Cube(1.9f, SEG_LEN + 0.3f, 0.5f, -0.8f, SEG_LEN / 2, -tz, Mat.TRIM));
            to.add(new Cube(1.9f, SEG_LEN + 0.3f, 0.5f, -0.8f, SEG_LEN / 2, tz, Mat.TRIM));
        }
        TIP.add(new Cube(1.6f, 3, 2.0f, -0.2f, 1.4f, 0, 0, -28, Mat.STEEL));   // thinner than the last segment
    }

    private static BladeRenderer instance;

    public static BladeRenderer get() {
        if (instance == null) instance = new BladeRenderer();
        return instance;
    }

    private BladeRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(stack.getItem() instanceof EnergyBladeItem)) return;
        boolean on = EnergyBladeItem.charged(stack);
        Minecraft mc = Minecraft.getInstance();
        float time = (mc.level != null ? mc.level.getGameTime() : 0) + mc.getFrameTime();
        int frame = ((int) (time / 2)) % GAS_FRAMES;

        ps.pushPose();
        if (ctx == ItemDisplayContext.GUI) {                 // 3D in the slot (author: "3D in the slot, do it"): diagonal, fitted into the cell centre
            ps.translate(0.5, 0.5, 0.5);
            ps.mulPose(Axis.ZP.rotationDegrees(-45));
            ps.mulPose(Axis.YP.rotationDegrees(180));
            ps.scale(GUI_SCALE, GUI_SCALE, GUI_SCALE);
            ps.translate(-GUI_CX * PX, -GUI_CY * PX, 0);
        } else {
            ps.translate(5.5 * PX, 5.5 * PX, 0.5);          // handle in the same place as a vanilla sword
            ps.mulPose(Axis.ZP.rotationDegrees(-45));        // blade diagonally up-right
            ps.mulPose(Axis.YP.rotationDegrees(180));        // edge (gas) down, spine up (author from a screenshot: "turn the blade over")
            ps.scale(0.62f, 0.62f, 0.62f);
            ps.translate(0, 5 * PX, 0);                      // grip in the middle of the handle
        }

        // two passes: first all the metal in one buffer, then the gas in its own (switching buffers mid-draw closes it)
        VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buf, RenderType.entityCutoutNoCull(PARTS), false, stack.hasFoil());
        for (Cube c : HILT) cube(ps, vc, c, light, overlay);
        segments(ps, i -> {
            for (Cube c : i % 2 == 0 ? SEG : SEG_ODD) cube(ps, vc, c, light, overlay);
            cube(ps, vc, new Cube(0.4f, SEG_LEN - 1, i % 2 == 0 ? 0.9f : 1.0f, 0.05f, SEG_LEN / 2, 0, on ? Mat.SLIT_ON : Mat.SLIT_OFF), light, overlay);
        }, () -> { for (Cube c : TIP) cube(ps, vc, c, light, overlay); });
        if (on) {
            VertexConsumer gas = buf.getBuffer(RenderType.entityTranslucentEmissive(GAS));
            segments(ps, i -> gasQuad(ps, gas, i, frame, overlay), () -> {});
        }
        ps.popPose();
    }

    /** Walk the curve: segment by segment, each next one SEG_LEN higher and turned by SEG_BEND; the tip at the end. */
    private static void segments(PoseStack ps, java.util.function.IntConsumer seg, Runnable tip) {
        ps.pushPose();
        ps.translate(0, PX, 0);
        for (int i = 0; i < SEGMENTS; i++) {
            if (i > 0) { ps.translate(0, SEG_LEN * PX, 0); ps.mulPose(Axis.ZP.rotationDegrees(SEG_BEND)); }
            seg.accept(i);
        }
        ps.translate(0, SEG_LEN * PX, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(SEG_BEND));
        tip.run();
        ps.popPose();
    }

    /** Segment gas: a GAS_W x GAS_H sheet in front of the spine, two layers half a pixel apart; a frame from the 48x80 strip. */
    private static void gasQuad(PoseStack ps, VertexConsumer vc, int seg, int frame, int overlay) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        float u0 = seg * GAS_W / GAS_TW, u1 = (seg * GAS_W + GAS_W) / GAS_TW, v0 = frame * GAS_H / 80f, v1 = (frame * GAS_H + GAS_H) / 80f;
        float x0 = GAS_X * PX, x1 = (GAS_X + GAS_W) * PX, y0 = 0, y1 = GAS_H * PX;
        float dz = (seg % 2 == 0 ? 0.45f : 0.52f) * PX;   // neighbouring gas segments in different layers
        for (float z : new float[]{-dz, dz}) {
            float[][] p = {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}}, uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
            for (int k = 0; k < 4; k++)
                vc.vertex(pose, p[k][0], p[k][1], z).color(1f, 1f, 1f, 1f).uv(uv[k][0], uv[k][1])
                        .overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0, 0, 1).endVertex();
        }
    }

    /** Bar: each face takes the tile of its material, 1 texel per model pixel (up to 16). */
    private static void cube(PoseStack ps, VertexConsumer vc, Cube c, int light, int overlay) {
        ps.pushPose();
        ps.translate(c.x * PX, c.y * PX, c.z * PX);
        if (c.yRot != 0) ps.mulPose(Axis.YP.rotationDegrees(c.yRot));
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
