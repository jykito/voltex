package com.jykito.industrialcore.client;
import net.minecraft.client.resources.model.ModelResourceLocation;

import com.jykito.industrialcore.block.custom.ElectromagnetCoreBlock;
import com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public class ElectromagnetCoreBER implements BlockEntityRenderer<ElectromagnetCoreBlockEntity> {

    // DEBUG: draw the bare geometry in a solid colour without textures (bake diagnostics). false = normal render.
    private static final boolean DEBUG_GEOMETRY = false;

    public static final ModelResourceLocation BODY_MODEL   = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("industrial_core", "block/electromagnet_body"));
    public static final ModelResourceLocation PILONS_MODEL = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("industrial_core", "block/electromagnet_pilons"));
    public static final ModelResourceLocation COIL_MODEL   = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("industrial_core", "block/electromagnet_copper_coil"));
    public static final ModelResourceLocation COIL_MODEL_2 = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("industrial_core", "block/electromagnet_superconductor_coil"));
    public static final ModelResourceLocation COIL_MODEL_3 = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("industrial_core", "block/electromagnet_graviton_coil"));
    // "Current": the same OBJs with a mask of glowing pixels (tools/texture_viewer/export_magnet_glow.js), drawn on top at full brightness
    public static final ModelResourceLocation BODY_GLOW    = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("industrial_core", "block/electromagnet_body_glow"));
    public static final ModelResourceLocation PILONS_GLOW  = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("industrial_core", "block/electromagnet_pilons_glow"));
    public static final ModelResourceLocation COIL_GLOW_2  = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("industrial_core", "block/electromagnet_superconductor_coil_glow"));
    public static final ModelResourceLocation COIL_GLOW_3  = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("industrial_core", "block/electromagnet_graviton_coil_glow"));

    private static final float SCALE = 8f;
    private static final float OFF_X = 0f;
    private static final float OFF_Y = -1.5f;
    private static final float OFF_Z = 3.5f;
    private static final float AXIS_Z = -0.0625f;

    public ElectromagnetCoreBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ElectromagnetCoreBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof ElectromagnetCoreBlock)) return;
        if (!state.getValue(ElectromagnetCoreBlock.FORMED)) return;

        Minecraft mc = Minecraft.getInstance();
        BakedModel missing = mc.getModelManager().getMissingModel();
        BakedModel body   = mc.getModelManager().getModel(BODY_MODEL);
        BakedModel pilons = mc.getModelManager().getModel(PILONS_MODEL);
        int coilTier = be.getCoilTier(); // the coil is drawn ONLY when inserted (tier>0), model by tier
        BakedModel coil = coilTier == 0 ? missing : mc.getModelManager().getModel(
                coilTier == 3 ? COIL_MODEL_3 : coilTier == 2 ? COIL_MODEL_2 : COIL_MODEL);
        if (body == missing && pilons == missing && coil == missing) return;

        float yDeg = switch (state.getValue(ElectromagnetCoreBlock.FACING)) {
            case WEST  -> 90f;
            case SOUTH -> 180f;
            case EAST  -> 270f;
            default    -> 0f;
        };
        float ang = be.getSpinAngle(partialTick); // accumulated angle of the columns/coil (smooth spin-up in the BE)

        renderField(be, ps, buf, partialTick, yDeg); // magnetic field: blue/red fans of arcs on the sides

        if (DEBUG_GEOMETRY) {
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(yDeg));
            ps.translate(OFF_X, OFF_Y, OFF_Z);
            ps.scale(SCALE, SCALE, SCALE);
            VertexConsumer dvc = buf.getBuffer(ModRenderTypes.debugGeo());
            if (body != missing) renderGeoDebug(body, ps, dvc, 40, 255, 40);
            ps.pushPose();
            ps.translate(0, 0, AXIS_Z);
            ps.mulPose(Axis.YP.rotationDegrees(ang));
            ps.translate(0, 0, -AXIS_Z);
            if (coil   != missing) renderGeoDebug(coil,   ps, dvc, 255, 40, 40);
            if (pilons != missing) renderGeoDebug(pilons, ps, dvc, 40, 40, 255);
            ps.popPose();
            ps.popPose();
            return;
        }
        var mm = mc.getModelManager();
        BakedModel coilGlow = coilTier == 3 ? mm.getModel(COIL_GLOW_3) : coilTier == 2 ? mm.getModel(COIL_GLOW_2) : missing; // copper does not glow
        renderModels(ps, buf, yDeg, ang, body, pilons, coil, mm.getModel(BODY_GLOW), mm.getModel(PILONS_GLOW), coilGlow,
                sampleLight(be), overlay);
    }

    // Light for the whole model. The core sits inside the structure where it is dark (this caused the "dark rectangles", which is why
    // FULL_BRIGHT used to be there); we take the brightest block/sky light from points around the magnet. The points are symmetric: facing does not matter.
    private static final int[][] LIGHT_PROBES;
    static {
        java.util.List<int[]> p = new java.util.ArrayList<>();
        for (int d : new int[]{-9, -5, 5, 9}) for (int y : new int[]{0, 4, 8}) { p.add(new int[]{d, y, 0}); p.add(new int[]{0, y, d}); }
        p.add(new int[]{0, 10, 0});
        LIGHT_PROBES = p.toArray(new int[0][]);
    }

    private static int sampleLight(ElectromagnetCoreBlockEntity be) {
        Level level = be.getLevel();
        if (level == null) return LightTexture.FULL_BRIGHT;
        BlockPos c = be.getBlockPos();
        int block = 0, sky = 0;
        for (int[] o : LIGHT_PROBES) {
            int packed = LevelRenderer.getLightColor(level, c.offset(o[0], o[1], o[2]));
            block = Math.max(block, LightTexture.block(packed));
            sky = Math.max(sky, LightTexture.sky(packed));
        }
        return LightTexture.pack(block, sky);
    }

    /**
     * Body, columns and coil (tier 0 = no coil) from the core block, without the field. For the structure preview in the guide, where
     * there is no block entity.
     */
    public static void renderModels(PoseStack ps, MultiBufferSource buf, float yDeg, float ang, int coilTier, int overlay) {
        var mm = Minecraft.getInstance().getModelManager();
        BakedModel coil = coilTier == 0 ? mm.getMissingModel() : mm.getModel(coilTier == 3 ? COIL_MODEL_3 : coilTier == 2 ? COIL_MODEL_2 : COIL_MODEL);
        BakedModel none = mm.getMissingModel();   // the guide has no world light: everything at full brightness, no masks needed
        renderModels(ps, buf, yDeg, ang, mm.getModel(BODY_MODEL), mm.getModel(PILONS_MODEL), coil, none, none, none,
                LightTexture.FULL_BRIGHT, overlay);
    }

    /** Models at light {@code light}, on top the "current" masks (glow) at full brightness with the same render type: depth matches. */
    private static void renderModels(PoseStack ps, MultiBufferSource buf, float yDeg, float ang,
                                     BakedModel body, BakedModel pilons, BakedModel coil,
                                     BakedModel bodyGlow, BakedModel pilonsGlow, BakedModel coilGlow, int light, int overlay) {
        BakedModel missing = Minecraft.getInstance().getModelManager().getMissingModel();
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yDeg));
        ps.translate(OFF_X, OFF_Y, OFF_Z);
        ps.scale(SCALE, SCALE, SCALE);

        RenderType rt = ModRenderTypes.solidModel(TextureAtlas.LOCATION_BLOCKS); // cull of closed models (config cullSolidFaces)
        VertexConsumer vc = buf.getBuffer(rt);

        if (body != missing) renderQuads(body, ps, vc, light, overlay);
        if (bodyGlow != missing) renderQuads(bodyGlow, ps, vc, LightTexture.FULL_BRIGHT, overlay);

        // the columns and the coil spin in OPPOSITE directions around the vertical axis
        if (pilons != missing) {
            ps.pushPose();
            ps.translate(0, 0, AXIS_Z);
            ps.mulPose(Axis.YP.rotationDegrees(ang));
            ps.translate(0, 0, -AXIS_Z);
            renderQuads(pilons, ps, vc, light, overlay);
            if (pilonsGlow != missing) renderQuads(pilonsGlow, ps, vc, LightTexture.FULL_BRIGHT, overlay);
            ps.popPose();
        }
        if (coil != missing) {
            ps.pushPose();
            ps.translate(0, 0, AXIS_Z);
            ps.mulPose(Axis.YP.rotationDegrees(-ang));
            ps.translate(0, 0, -AXIS_Z);
            renderQuads(coil, ps, vc, light, overlay);
            if (coilGlow != missing) renderQuads(coilGlow, ps, vc, LightTexture.FULL_BRIGHT, overlay);
            ps.popPose();
        }

        ps.popPose();
    }

    // ── Magnetic field: two fans of arcs (blue/red) on the sides ────────────────────────────────
    // Canonical NORTH frame: coil axis = vertical x=0, z=3; centre (0,3,3). YP(yDeg) rotates by facing.
    private static final int ARC_SEGS = 24;
    private static final float CY = 3f, CZ = 3f;
    // {half-height, outward reach}: nested arcs; reach < height -> gentle vertical bows, not loops.
    // Max reach 2.9 < 3.5 (half-width of the body) -> the arcs stay within the footprint.
    private static final float[][] ARCS = {
        {1.4f, 0.8f}, {1.9f, 1.3f}, {2.4f, 1.8f}, {2.9f, 2.3f}, {3.3f, 2.9f}
    };

    // Field scratch: there is one render thread -> buffers are reused, zero allocations in hot loops (output 1:1 the same).
    private static final Vector3f[] PTS = new Vector3f[ARC_SEGS + 1];
    static { for (int i = 0; i <= ARC_SEGS; i++) PTS[i] = new Vector3f(); }
    private static final Matrix4f SCR_INV = new Matrix4f();
    private static final Vector3f SCR_CAM = new Vector3f();
    private static final Vector3f SCR_TAN = new Vector3f(), SCR_TOCAM = new Vector3f(), SCR_SIDE = new Vector3f();
    private static final Vector3f SCR_A0 = new Vector3f(), SCR_B0 = new Vector3f(), SCR_A1 = new Vector3f(), SCR_B1 = new Vector3f();

    /** Draws the magnetic field: blue fan on the left (−X), red on the right (+X). Arcs and brightness grow with instability. */
    private void renderField(ElectromagnetCoreBlockEntity be, PoseStack ps, MultiBufferSource buf,
                             float partialTick, float yDeg) {
        if (be.getCoilTier() == 0) return;                              // the coil generates the field: no coil, no field
        float instab = be.getInstability();                             // 0..100 %
        float energy = Math.max(be.getSpinLevel(), instab / 100f);      // 0..1 total field "energy"
        float time = (float) (be.getLevel().getGameTime() % 100000L) + partialTick;
        float pulse = 0.55f + 0.45f * Mth.sin(time * (0.07f + 0.10f * energy)); // "breathing"
        float flowSpeed = 0.15f + 0.30f * energy;                              // speed of the running spark
        float baseAlpha = 0.12f + 0.16f * energy;                             // brighter under load/instability

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yDeg));
        Matrix4f mat = ps.last().pose();
        Vector3f cam = mat.invert(SCR_INV).transformPosition(SCR_CAM.set(0, 0, 0)); // camera in local space (scratch)

        VertexConsumer vc = buf.getBuffer(ModRenderTypes.magnetField());
        for (float[] a : ARCS) {                                        // base arcs (within the footprint)
            drawArc(vc, mat, cam, -1f, a[0], a[1], time, flowSpeed, baseAlpha, pulse, 0.35f, 0.65f, 1.0f);
            drawArc(vc, mat, cam, +1f, a[0], a[1], time, flowSpeed, baseAlpha, pulse, 1.0f, 0.35f, 0.35f);
        }
        int extra = (int) (instab / 10f);                              // every 10% -> +1 arc outwards (+1 block)
        for (int k = 1; k <= extra; k++) {
            float h = 3.3f + k * 0.45f, b = 2.9f + k * 1.0f;
            drawArc(vc, mat, cam, -1f, h, b, time, flowSpeed, baseAlpha, pulse, 0.35f, 0.65f, 1.0f);
            drawArc(vc, mat, cam, +1f, h, b, time, flowSpeed, baseAlpha, pulse, 1.0f, 0.35f, 0.35f);
        }
        ps.popPose();
    }

    /** One arc = 2 ribbon layers: a wide dim halo + a thin bright core. Points are built in scratch PTS. */
    private static void drawArc(VertexConsumer vc, Matrix4f mat, Vector3f cam, float sign,
                                float h, float b, float time, float flowSpeed, float baseAlpha,
                                float pulse, float cr, float cg, float cb) {
        float bx = sign * b * 1.33f; // control point placed so that the middle of the arc reaches ±b
        for (int i = 0; i <= ARC_SEGS; i++) {
            float t = (float) i / ARC_SEGS;
            cubicInto(PTS[i], t, 0, CY + h, CZ, bx, CY + h * 0.45f, CZ, bx, CY - h * 0.45f, CZ, 0, CY - h, CZ);
        }
        ribbon(vc, mat, cam, 0.14f,  baseAlpha * 0.35f * pulse, time, flowSpeed, cr, cg, cb, false); // halo
        ribbon(vc, mat, cam, 0.045f, baseAlpha * pulse,         time, flowSpeed, cr, cg, cb, true);  // core
    }

    /** Billboard ribbon along scratch PTS: width is offset perpendicular to the view. Zero allocations (scratch). */
    private static void ribbon(VertexConsumer vc, Matrix4f mat, Vector3f cam,
                               float halfW, float alpha, float time, float flowSpeed,
                               float cr, float cg, float cb, boolean core) {
        int R = clampByte(cr * 255f), G = clampByte(cg * 255f), B = clampByte(cb * 255f);
        for (int i = 0; i < ARC_SEGS; i++) {
            Vector3f p0 = PTS[i], p1 = PTS[i + 1];
            SCR_TAN.set(p1).sub(p0);
            if (SCR_TAN.lengthSquared() < 1e-9f) continue;
            SCR_TAN.normalize();
            SCR_TOCAM.set(cam).sub(p0);
            SCR_TAN.cross(SCR_TOCAM, SCR_SIDE);                        // perpendicular to the tangent and the view
            if (SCR_SIDE.lengthSquared() < 1e-9f) continue;
            SCR_SIDE.normalize().mul(halfW);
            float t0 = (float) i / ARC_SEGS;
            float wave = core ? (0.45f + 0.55f * Mth.sin(t0 * 6.2831f - time * flowSpeed)) : 1f; // running spark
            int a = clampByte(alpha * wave * 255f);
            SCR_A0.set(p0).sub(SCR_SIDE); SCR_B0.set(p0).add(SCR_SIDE);
            SCR_A1.set(p1).sub(SCR_SIDE); SCR_B1.set(p1).add(SCR_SIDE);
            vc.addVertex(mat, SCR_A0.x, SCR_A0.y, SCR_A0.z).setColor(R, G, B, a);
            vc.addVertex(mat, SCR_B0.x, SCR_B0.y, SCR_B0.z).setColor(R, G, B, a);
            vc.addVertex(mat, SCR_B1.x, SCR_B1.y, SCR_B1.z).setColor(R, G, B, a);
            vc.addVertex(mat, SCR_A1.x, SCR_A1.y, SCR_A1.z).setColor(R, G, B, a);
        }
    }

    private static void cubicInto(Vector3f out, float t, float x0, float y0, float z0, float x1, float y1, float z1,
                                  float x2, float y2, float z2, float x3, float y3, float z3) {
        float u = 1 - t, w0 = u * u * u, w1 = 3 * u * u * t, w2 = 3 * u * t * t, w3 = t * t * t;
        out.set(w0 * x0 + w1 * x1 + w2 * x2 + w3 * x3,
                w0 * y0 + w1 * y1 + w2 * y2 + w3 * y3,
                w0 * z0 + w1 * z1 + w2 * z2 + w3 * z3);
    }

    private static int clampByte(float v) { return (int) Mth.clamp(v, 0f, 255f); }

    /** DEBUG: draws all model quads in a solid colour (positions from the baked vertices). */
    private static void renderGeoDebug(BakedModel model, PoseStack ps, VertexConsumer vc, int r, int g, int b) {
        Matrix4f mat = ps.last().pose();
        for (BakedQuad q : allQuads(model)) {
            int[] d = q.getVertices();
            int stride = d.length / 4;
            for (int i = 0; i < 4; i++) {
                float x = Float.intBitsToFloat(d[i * stride]);
                float y = Float.intBitsToFloat(d[i * stride + 1]);
                float z = Float.intBitsToFloat(d[i * stride + 2]);
                vc.addVertex(mat, x, y, z).setColor(r, g, b, 255);
            }
        }
    }

    private static void renderQuads(BakedModel model, PoseStack ps, VertexConsumer vc, int light, int overlay) {
        PoseStack.Pose pose = ps.last();
        for (BakedQuad q : allQuads(model)) {
            vc.putBulkData(pose, q, 1f, 1f, 1f, 1f, light, overlay);
        }
    }

    private static final WeakHashMap<BakedModel, List<BakedQuad>> QUAD_CACHE = new WeakHashMap<>();
    private static List<BakedQuad> allQuads(BakedModel model) {
        return QUAD_CACHE.computeIfAbsent(model, m -> {
            RandomSource r = RandomSource.create();
            List<BakedQuad> list = new ArrayList<>(m.getQuads(null, null, r, ModelData.EMPTY, null));
            for (Direction d : Direction.values()) list.addAll(m.getQuads(null, d, r, ModelData.EMPTY, null));
            return list;
        });
    }

    @Override
    public boolean shouldRenderOffScreen(ElectromagnetCoreBlockEntity be) { return false; } // frustum culling (the bbox covers body + field)

    @Override
    public int getViewDistance() { return 128; }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(ElectromagnetCoreBlockEntity be) {
        return be.renderBounds();
    }
}
