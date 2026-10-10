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
 * Объёмный энергоклинок Б5 «Изгиб» в руке, на земле и в рамке (в интерфейсе — плоская иконка: модель предмета
 * forge:separate_transforms, вид gui — item/generated). Юзер 2026-10-07: «у клинка кромка металлическая сзади лезвия,
 * и само лезвие выходит как будто газом»; из вариантов tools/texture_viewer/weapons3d.html выбран изгиб.
 * <p>
 * Оси клинка: лезвие +y, обух сзади (−x), газ вперёд (+x), толщина по z. Обух из семи сегментов, каждый повёрнут на 4°
 * к обуху — изгиб как у катаны. Газ — полотно перед обухом, кадры из energy_blade_gas.png листаются по 2 тика, светится
 * и просвечивает; без заряда газа нет, щель-сопло гаснет. В пространство предмета клинок ложится как ванильный меч на иконке:
 * рукоять внизу слева, остриё вверху справа, поэтому подходят показы item/handheld.
 */
public final class BladeRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation PARTS = new ResourceLocation("industrial_core", "textures/models/energy_blade_parts.png");
    private static final ResourceLocation GAS = new ResourceLocation("industrial_core", "textures/models/energy_blade_gas.png");
    private static final float PX = 1 / 16f;

    /** Плитки атласа 64×64 — порядок как в blade_atlas.js. */
    enum Mat { BODY, DARK, GRIP, TRIM, STEEL, LAMP, SLIT_ON, SLIT_OFF;
        boolean glow() { return this == LAMP || this == SLIT_ON; } }

    /** Брусок w×h×d (пиксели) с центром (x, y, z), поворотом вокруг y (yRot), затем вокруг z (zRot). */
    private record Cube(float w, float h, float d, float x, float y, float z, float yRot, float zRot, Mat mat) {
        Cube(float w, float h, float d, float x, float y, float z, Mat mat) { this(w, h, d, x, y, z, 0, 0, mat); }
    }

    private static final int SEGMENTS = 7, GAS_FRAMES = 16;   // юзер: «удлинни клинок» (было 5)
    private static final float SEG_LEN = 4.6f, SEG_BEND = 4f, GAS_W = 6f, GAS_H = 5f, GAS_X = 0.25f, GAS_TW = 48f;
    /** В слоте: середина сабли (от навершия до острия с газом, x −8.1..6.3, y −11..35.2) и масштаб, чтобы длина легла по диагонали клетки. */
    private static final float GUI_CX = -0.9f, GUI_CY = 12.1f, GUI_SCALE = 0.45f;
    private static final List<Cube> HILT = new ArrayList<>(), SEG = new ArrayList<>(), SEG_ODD = new ArrayList<>(), TIP = new ArrayList<>();
    static {
        // рукоять-восьмигранник, навершие с огоньком, гарда с отделкой
        float a = 1.25f, s = 2 * a * (float) Math.tan(Math.toRadians(22.5));
        for (float r : new float[]{0, 45, 90, 135}) HILT.add(new Cube(2 * a, 8.8f, s, 0, -5, 0, r, 0, Mat.GRIP));   // концы внутри навершия и гарды: без общих граней
        HILT.add(new Cube(3.2f, 2, 3.2f, 0, -10, 0, Mat.DARK));
        HILT.add(new Cube(1, 1, 3.4f, 0, -10.2f, 0, Mat.LAMP));
        HILT.add(new Cube(8, 2, 3.2f, 1.5f, 0, 0, Mat.BODY));
        HILT.add(new Cube(8.2f, 0.6f, 3.4f, 1.5f, 0.6f, 0, Mat.TRIM));
        HILT.add(new Cube(2.4f, 1.4f, 2.6f, -0.5f, 1.6f, 0, Mat.DARK));   // задняя грань не в плоскости обуха
        // сегмент обуха: сталь, отделка по бокам; щель-сопло добавляется отдельно (горит по заряду).
        // Против мерцания граней: сталь длиннее сегмента на 0.6 и заходит в соседний, полосы короче стали и шире её на 0.15;
        // сегменты гнутся вокруг z, поэтому их боковые грани у соседей в одной плоскости — у нечётных всё на 0.05–0.1 толще
        for (List<Cube> to : List.of(SEG, SEG_ODD)) {
            boolean odd = to == SEG_ODD;
            float d = odd ? 2.3f : 2.2f, tz = odd ? 1.27f : 1.2f;
            to.add(new Cube(1.6f, SEG_LEN + 0.6f, d, -0.8f, SEG_LEN / 2, 0, Mat.STEEL));
            to.add(new Cube(1.9f, SEG_LEN + 0.3f, 0.5f, -0.8f, SEG_LEN / 2, -tz, Mat.TRIM));
            to.add(new Cube(1.9f, SEG_LEN + 0.3f, 0.5f, -0.8f, SEG_LEN / 2, tz, Mat.TRIM));
        }
        TIP.add(new Cube(1.6f, 3, 2.0f, -0.2f, 1.4f, 0, 0, -28, Mat.STEEL));   // тоньше последнего сегмента
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
        if (ctx == ItemDisplayContext.GUI) {                 // 3D в слоте (юзер: «3D в слоте, делай»): по диагонали, вписана в клетку по центру
            ps.translate(0.5, 0.5, 0.5);
            ps.mulPose(Axis.ZP.rotationDegrees(-45));
            ps.mulPose(Axis.YP.rotationDegrees(180));
            ps.scale(GUI_SCALE, GUI_SCALE, GUI_SCALE);
            ps.translate(-GUI_CX * PX, -GUI_CY * PX, 0);
        } else {
            ps.translate(5.5 * PX, 5.5 * PX, 0.5);          // рукоять там же, где у ванильного меча
            ps.mulPose(Axis.ZP.rotationDegrees(-45));        // лезвие по диагонали вправо-вверх
            ps.mulPose(Axis.YP.rotationDegrees(180));        // кромкой (газом) вниз, обухом вверх (юзер по скрину: «развернуть лезвие»)
            ps.scale(0.62f, 0.62f, 0.62f);
            ps.translate(0, 5 * PX, 0);                      // хват посередине рукояти
        }

        // два прохода: сначала весь металл одним буфером, потом газ своим (смена буфера посреди рисования его закрывает)
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

    /** Обход изгиба: сегмент за сегментом, каждый следующий выше на SEG_LEN и повёрнут на SEG_BEND; в конце остриё. */
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

    /** Газ сегмента: полотно GAS_W×GAS_H перед обухом, два слоя на полпикселя друг от друга; кадр из ленты 48×80. */
    private static void gasQuad(PoseStack ps, VertexConsumer vc, int seg, int frame, int overlay) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        float u0 = seg * GAS_W / GAS_TW, u1 = (seg * GAS_W + GAS_W) / GAS_TW, v0 = frame * GAS_H / 80f, v1 = (frame * GAS_H + GAS_H) / 80f;
        float x0 = GAS_X * PX, x1 = (GAS_X + GAS_W) * PX, y0 = 0, y1 = GAS_H * PX;
        float dz = (seg % 2 == 0 ? 0.45f : 0.52f) * PX;   // соседние сегменты газа в разных слоях
        for (float z : new float[]{-dz, dz}) {
            float[][] p = {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}}, uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
            for (int k = 0; k < 4; k++)
                vc.vertex(pose, p[k][0], p[k][1], z).color(1f, 1f, 1f, 1f).uv(uv[k][0], uv[k][1])
                        .overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0, 0, 1).endVertex();
        }
    }

    /** Брусок: каждая грань берёт плитку своего материала, 1 тексель на пиксель модели (до 16). */
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
                    int kk = sign > 0 ? k : 3 - k;   // обход против часовой снаружи
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
