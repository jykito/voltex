package com.jykito.industrialcore.client;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public class ElectromagnetCoreBER implements BlockEntityRenderer<ElectromagnetCoreBlockEntity> {

    // DEBUG: рисовать чистую геометрию сплошным цветом без текстур (диагностика бейка). false = обычный рендер.
    private static final boolean DEBUG_GEOMETRY = false;

    public static final ResourceLocation BODY_MODEL   = new ResourceLocation("industrial_core", "block/electromagnet_body");
    public static final ResourceLocation PILONS_MODEL = new ResourceLocation("industrial_core", "block/electromagnet_pilons");
    public static final ResourceLocation COIL_MODEL   = new ResourceLocation("industrial_core", "block/electromagnet_copper_coil");
    public static final ResourceLocation COIL_MODEL_2 = new ResourceLocation("industrial_core", "block/electromagnet_superconductor_coil");
    public static final ResourceLocation COIL_MODEL_3 = new ResourceLocation("industrial_core", "block/electromagnet_graviton_coil");

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
        int coilTier = be.getCoilTier(); // катушка рисуется ТОЛЬКО когда вставлена (тир>0), модель по тиру
        BakedModel coil = coilTier == 0 ? missing : mc.getModelManager().getModel(
                coilTier == 3 ? COIL_MODEL_3 : coilTier == 2 ? COIL_MODEL_2 : COIL_MODEL);
        if (body == missing && pilons == missing && coil == missing) return;

        float yDeg = switch (state.getValue(ElectromagnetCoreBlock.FACING)) {
            case WEST  -> 90f;
            case SOUTH -> 180f;
            case EAST  -> 270f;
            default    -> 0f;
        };
        float ang = be.getSpinAngle(partialTick); // накопленный угол столбов/катушки (плавный разгон в BE)

        renderField(be, ps, buf, partialTick, yDeg); // магнитное поле: синий/красный веера дуг по бокам

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yDeg));
        ps.translate(OFF_X, OFF_Y, OFF_Z);
        ps.scale(SCALE, SCALE, SCALE);

        if (DEBUG_GEOMETRY) {
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

        RenderType rt = ModRenderTypes.solidModel(TextureAtlas.LOCATION_BLOCKS); // cull закрытых моделей (конфиг cullSolidFaces)
        VertexConsumer vc = buf.getBuffer(rt);

        if (body != missing) renderQuads(body, ps, vc, overlay);

        // столбы и катушка крутятся В РАЗНЫЕ стороны вокруг вертикальной оси
        if (pilons != missing) {
            ps.pushPose();
            ps.translate(0, 0, AXIS_Z);
            ps.mulPose(Axis.YP.rotationDegrees(ang));
            ps.translate(0, 0, -AXIS_Z);
            renderQuads(pilons, ps, vc, overlay);
            ps.popPose();
        }
        if (coil != missing) {
            ps.pushPose();
            ps.translate(0, 0, AXIS_Z);
            ps.mulPose(Axis.YP.rotationDegrees(-ang));
            ps.translate(0, 0, -AXIS_Z);
            renderQuads(coil, ps, vc, overlay);
            ps.popPose();
        }

        ps.popPose();
    }

    // ── Магнитное поле: два веера дуг (синий/красный) по бокам ────────────────────────────────
    // Канонич. NORTH-фрейм: ось катушки = вертикаль x=0, z=3; центр (0,3,3). YP(yDeg) доворачивает по фейсингу.
    private static final int ARC_SEGS = 24;
    private static final float CY = 3f, CZ = 3f;
    // {полу-высота, вынос наружу} — вложенные дуги; вынос < высоты → пологие вертикальные луки, не петли.
    // Макс. вынос 2.9 < 3.5 (полу-ширина корпуса) → дуги держатся в габаритах.
    private static final float[][] ARCS = {
        {1.4f, 0.8f}, {1.9f, 1.3f}, {2.4f, 1.8f}, {2.9f, 2.3f}, {3.3f, 2.9f}
    };

    // Scratch поля: рендер-тред один → переиспользуем буферы, ноль аллокаций в горячих циклах (вывод 1:1 тот же).
    private static final Vector3f[] PTS = new Vector3f[ARC_SEGS + 1];
    static { for (int i = 0; i <= ARC_SEGS; i++) PTS[i] = new Vector3f(); }
    private static final Matrix4f SCR_INV = new Matrix4f();
    private static final Vector3f SCR_CAM = new Vector3f();
    private static final Vector3f SCR_TAN = new Vector3f(), SCR_TOCAM = new Vector3f(), SCR_SIDE = new Vector3f();
    private static final Vector3f SCR_A0 = new Vector3f(), SCR_B0 = new Vector3f(), SCR_A1 = new Vector3f(), SCR_B1 = new Vector3f();

    /** Рисует магнитное поле: синий веер слева (−X), красный справа (+X). Дуги/яркость растут с нестабильностью. */
    private void renderField(ElectromagnetCoreBlockEntity be, PoseStack ps, MultiBufferSource buf,
                             float partialTick, float yDeg) {
        if (be.getCoilTier() == 0) return;                              // поле генерит катушка — нет катушки, нет поля
        float instab = be.getInstability();                             // 0..100 %
        float energy = Math.max(be.getSpinLevel(), instab / 100f);      // 0..1 общая «энергия» поля
        float time = (float) (be.getLevel().getGameTime() % 100000L) + partialTick;
        float pulse = 0.55f + 0.45f * Mth.sin(time * (0.07f + 0.10f * energy)); // «дыхание»
        float flowSpeed = 0.15f + 0.30f * energy;                              // скорость бегущей искры
        float baseAlpha = 0.12f + 0.16f * energy;                             // ярче под нагрузкой/нестабильностью

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yDeg));
        Matrix4f mat = ps.last().pose();
        Vector3f cam = mat.invert(SCR_INV).transformPosition(SCR_CAM.set(0, 0, 0)); // камера в локале (scratch)

        VertexConsumer vc = buf.getBuffer(ModRenderTypes.magnetField());
        for (float[] a : ARCS) {                                        // базовые дуги (в габаритах)
            drawArc(vc, mat, cam, -1f, a[0], a[1], time, flowSpeed, baseAlpha, pulse, 0.35f, 0.65f, 1.0f);
            drawArc(vc, mat, cam, +1f, a[0], a[1], time, flowSpeed, baseAlpha, pulse, 1.0f, 0.35f, 0.35f);
        }
        int extra = (int) (instab / 10f);                              // каждые 10% → +1 дуга наружу (+1 блок)
        for (int k = 1; k <= extra; k++) {
            float h = 3.3f + k * 0.45f, b = 2.9f + k * 1.0f;
            drawArc(vc, mat, cam, -1f, h, b, time, flowSpeed, baseAlpha, pulse, 0.35f, 0.65f, 1.0f);
            drawArc(vc, mat, cam, +1f, h, b, time, flowSpeed, baseAlpha, pulse, 1.0f, 0.35f, 0.35f);
        }
        ps.popPose();
    }

    /** Одна дуга = 2 слоя ленты: широкий тусклый ореол + тонкий яркий кор. Точки строятся в scratch PTS. */
    private static void drawArc(VertexConsumer vc, Matrix4f mat, Vector3f cam, float sign,
                                float h, float b, float time, float flowSpeed, float baseAlpha,
                                float pulse, float cr, float cg, float cb) {
        float bx = sign * b * 1.33f; // контрольная точка так, что середина дуги выходит на ±b
        for (int i = 0; i <= ARC_SEGS; i++) {
            float t = (float) i / ARC_SEGS;
            cubicInto(PTS[i], t, 0, CY + h, CZ, bx, CY + h * 0.45f, CZ, bx, CY - h * 0.45f, CZ, 0, CY - h, CZ);
        }
        ribbon(vc, mat, cam, 0.14f,  baseAlpha * 0.35f * pulse, time, flowSpeed, cr, cg, cb, false); // ореол
        ribbon(vc, mat, cam, 0.045f, baseAlpha * pulse,         time, flowSpeed, cr, cg, cb, true);  // кор
    }

    /** Билборд-лента вдоль scratch PTS: ширина отклоняется перпендикулярно взгляду. Ноль аллокаций (scratch). */
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
            SCR_TAN.cross(SCR_TOCAM, SCR_SIDE);                        // перпендикуляр касательной и взгляду
            if (SCR_SIDE.lengthSquared() < 1e-9f) continue;
            SCR_SIDE.normalize().mul(halfW);
            float t0 = (float) i / ARC_SEGS;
            float wave = core ? (0.45f + 0.55f * Mth.sin(t0 * 6.2831f - time * flowSpeed)) : 1f; // бегущая искра
            int a = clampByte(alpha * wave * 255f);
            SCR_A0.set(p0).sub(SCR_SIDE); SCR_B0.set(p0).add(SCR_SIDE);
            SCR_A1.set(p1).sub(SCR_SIDE); SCR_B1.set(p1).add(SCR_SIDE);
            vc.vertex(mat, SCR_A0.x, SCR_A0.y, SCR_A0.z).color(R, G, B, a).endVertex();
            vc.vertex(mat, SCR_B0.x, SCR_B0.y, SCR_B0.z).color(R, G, B, a).endVertex();
            vc.vertex(mat, SCR_B1.x, SCR_B1.y, SCR_B1.z).color(R, G, B, a).endVertex();
            vc.vertex(mat, SCR_A1.x, SCR_A1.y, SCR_A1.z).color(R, G, B, a).endVertex();
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

    /** DEBUG: рисует все квады модели сплошным цветом (позиции из baked-вершин). */
    private static void renderGeoDebug(BakedModel model, PoseStack ps, VertexConsumer vc, int r, int g, int b) {
        Matrix4f mat = ps.last().pose();
        for (BakedQuad q : allQuads(model)) {
            int[] d = q.getVertices();
            int stride = d.length / 4;
            for (int i = 0; i < 4; i++) {
                float x = Float.intBitsToFloat(d[i * stride]);
                float y = Float.intBitsToFloat(d[i * stride + 1]);
                float z = Float.intBitsToFloat(d[i * stride + 2]);
                vc.vertex(mat, x, y, z).color(r, g, b, 255).endVertex();
            }
        }
    }

    private static void renderQuads(BakedModel model, PoseStack ps, VertexConsumer vc, int overlay) {
        PoseStack.Pose pose = ps.last();
        for (BakedQuad q : allQuads(model)) {
            vc.putBulkData(pose, q, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, overlay);
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
    public boolean shouldRenderOffScreen(ElectromagnetCoreBlockEntity be) { return false; } // фрустум-куллинг (bbox покрывает корпус+поле)

    @Override
    public int getViewDistance() { return 128; }
}
