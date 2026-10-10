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
 * Объёмная гаусс-винтовка Г5 «Вилка», сталь с бирюзой (юзер 2026-10-07: «бирюза, вариант по центру»; эскиз
 * tools/texture_viewer/weapons3d.html). В интерфейсе — плоская иконка (forge:separate_transforms, вид gui — item/generated).
 * <p>
 * Оси винтовки как у бура: ствол −z, верх +y, право +x — и в пространство предмета она ложится так же (TO_ITEM из
 * DrillRenderer), поэтому и показы у модели предмета бурные. Корпус: ствольная коробка со ступенчатой крышкой и
 * планкой, прорези, заклёпки, индикатор ступеней, энергоячейка, кабели, прицел, скоба, рукоять, рамочный приклад;
 * впереди две стальные шины с катушками. Пока свой игрок держит ПКМ, между шинами растёт дуга (кадры из gauss_arc.png
 * по 2 тика), а катушки, прорези и индикатор загораются по ступеням заряда; окно энергоячейки горит, когда заряда хватает
 * на выстрел.
 */
public final class GaussRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation PARTS = new ResourceLocation("industrial_core", "textures/models/gauss_parts.png");
    private static final ResourceLocation ARC = new ResourceLocation("industrial_core", "textures/models/gauss_arc.png");
    private static final float PX = 1 / 16f;

    /** Плитки атласа 64×64 — порядок как в gauss_atlas.js. */
    enum Mat { BODY, DARK, GRIP, TRIM, COIL, STEEL, GLOW, DIM, LAMP, LAMP_OFF;
        boolean glow() { return this == GLOW || this == LAMP; } }

    /** Брусок w×h×d (пиксели) с центром (x, y, z) и поворотом вокруг x (xRot). */
    private record Cube(float w, float h, float d, float x, float y, float z, float xRot, Mat mat) {
        Cube(float w, float h, float d, float x, float y, float z, Mat mat) { this(w, h, d, x, y, z, 0, mat); }
        Cube as(Mat m) { return new Cube(w, h, d, x, y, z, xRot, m); }
    }

    private static final List<Cube> BODY = new ArrayList<>();
    /** Огни: прорези (горят со ступени i % 3 + 1), индикатор (со ступени i + 1), катушки шин (по паре на ступень). */
    private static final List<Cube> VENTS = new ArrayList<>(), LEDS = new ArrayList<>(), COILS = new ArrayList<>();
    private static final Cube CELL_WINDOW = new Cube(3.2f, 0.7f, 3.6f, 0, -3.5f, 1.2f, Mat.GLOW);
    private static final float ARC_LEN = 20, ARC_W = 3, ARC_Z = -2.4f;
    private static final int ARC_FRAMES = 16;
    static {
        List<Cube> b = BODY;
        b.add(new Cube(5, 4, 11, 0, 1.5f, 4, Mat.BODY)); b.add(new Cube(4, 1, 10, 0, 3.9f, 4.2f, Mat.BODY)); b.add(new Cube(4.4f, 0.4f, 10.4f, 0, 3.4f, 4.2f, Mat.TRIM));   // полоса выступает из-под крышки заметно, не на 0.05
        b.add(new Cube(1.6f, 0.7f, 9, 0, 4.7f, 4, Mat.DARK)); for (int i = 0; i < 5; i++) b.add(new Cube(2, 0.4f, 0.6f, 0, 5.1f, 0.6f + i * 1.7f, Mat.DARK));
        b.add(new Cube(4.6f, 2.6f, 9, 0, -1.6f, 4.5f, Mat.DARK)); b.add(new Cube(4.2f, 4.6f, 1.6f, 0, 1, -1.2f, Mat.DARK)); b.add(new Cube(3.2f, 3.2f, 0.6f, 0, 1, -2.1f, Mat.STEEL));
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int i = 0; i < 4; i++) VENTS.add(new Cube(0.25f, 2, 0.5f, sx * 2.55f, 1.6f, 0.5f + i * 1.2f, Mat.GLOW));
            for (float[] yz : new float[][]{{3.1f, 6.2f}, {3.1f, 9.2f}, {-0.1f, 6.2f}, {-0.1f, 9.2f}}) b.add(new Cube(0.3f, 0.45f, 0.45f, sx * 2.65f, yz[0], yz[1], Mat.STEEL));
            b.add(new Cube(0.2f, 2.4f, 3.2f, sx * 2.6f, 1.4f, 7.7f, Mat.DARK));   // панель тоньше — заклёпки выступают над ней
            b.add(new Cube(0.9f, 0.9f, 9, sx * 1.4f, -1.3f, -5, Mat.GRIP)); b.add(new Cube(1.1f, 1.1f, 0.6f, sx * 1.4f, -1.3f, -0.6f, Mat.STEEL));
        }
        for (int i = 0; i < 3; i++) LEDS.add(new Cube(0.3f, 0.7f, 0.7f, -2.75f, 1.6f, 6.9f + i * 0.9f, Mat.LAMP));
        b.add(new Cube(3, 3, 4.4f, 0, -3.9f, 1.2f, Mat.DARK)); b.add(new Cube(3.2f, 0.5f, 4.6f, 0, -5.5f, 1.2f, Mat.STEEL));
        b.add(new Cube(1.8f, 1.8f, 3, 0, 5.9f, 3, Mat.DARK)); b.add(new Cube(1.3f, 1.3f, 0.3f, 0, 5.9f, 1.4f, Mat.LAMP)); b.add(new Cube(1.9f, 0.5f, 1, 0, 5, 2, Mat.STEEL));
        b.add(new Cube(0.8f, 0.6f, 3.2f, 0, -3.4f, 5.6f, Mat.DARK)); b.add(new Cube(0.8f, 2.2f, 0.6f, 0, -2.6f, 4.1f, Mat.DARK));
        b.add(new Cube(2.6f, 6, 3, 0, -5.2f, 6.6f, -15, Mat.GRIP)); b.add(new Cube(2.9f, 0.8f, 3.3f, 0, -8.1f, 7.4f, -15, Mat.DARK));
        b.add(new Cube(1.4f, 1.2f, 7, 0, 2.2f, 12.6f, Mat.DARK)); b.add(new Cube(1.4f, 1.2f, 6.4f, 0, -1.8f, 12.4f, -9, Mat.DARK)); b.add(new Cube(2.6f, 1.6f, 4, 0, 3.1f, 12.2f, Mat.BODY));
        b.add(new Cube(3, 6.4f, 1.4f, 0, 0, 16.4f, Mat.BODY)); b.add(new Cube(3.2f, 6.6f, 0.6f, 0, 0, 17.3f, Mat.GRIP));
        // вилка: две стальные шины, на них по шесть катушек со светящимися полосами
        for (float[] ys : new float[][]{{3.2f, 1}, {-1.6f, -1}}) {
            float y = ys[0], s = ys[1];
            b.add(new Cube(1.8f, 1.4f, 21.3f, 0, y, -12.35f, Mat.STEEL));   // шина заходит в передний блок b.add(new Cube(2.4f, 1.6f, 2, 0, y + s * 0.4f, -23, Mat.DARK));
            for (int i = 0; i < 6; i++) { float z = -4.5f - i * 3.2f;
                b.add(new Cube(2.6f, 0.8f, 1.6f, 0, y + s * 1.05f, z, Mat.COIL)); COILS.add(new Cube(2.7f, 0.4f, 1.7f, 0, y + s * 1.4f, z, Mat.GLOW)); }   // катушка утоплена в шину, полоса выше катушки
        }
    }

    /** Оси винтовки -> пространство предмета, как у бура (DrillRenderer.TO_ITEM): ствол по диагонали вправо-вверх. */
    private static final Quaternionf TO_ITEM;
    static {
        float r = (float) (1 / Math.sqrt(2));
        TO_ITEM = new Quaternionf().setFromNormalized(new Matrix3f(0, 0, -1, r, -r, 0, -r, -r, 0));
    }

    /**
     * В слоте — поза Г «профиль с наклоном» (юзер выбрал из четырёх в weapons3d.html, POSES): повороты слота по порядку
     * Y −72°, X 10°, Z 14°. Масштаб — по габаритам винтовки (x ±2.8, y −8.8..6.8, z −24..17.6) в этой позе, с полями 6%.
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
        GUI_SCALE = 0.94f * 8 / Math.max(w, h);   // полуширина клетки 8 px модели при масштабе 1
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
        float time = (mc.level != null ? mc.level.getGameTime() : 0) + mc.getFrameTime();
        int frame = ((int) (time / 2)) % ARC_FRAMES;

        ps.pushPose();
        if (ctx == ItemDisplayContext.GUI) {            // 3D в слоте в позе Г «профиль с наклоном», вписана в клетку по центру
            ps.translate(0.5, 0.5, 0.5);
            ps.mulPose(GUI_POSE);
            ps.scale(GUI_SCALE, GUI_SCALE, GUI_SCALE);
            ps.translate(-GUI_C[0] * PX, -GUI_C[1] * PX, -GUI_C[2] * PX);
        } else {
            ps.translate(5.5 * PX, 5.5 * PX, 0.5);      // рукоять там же, где у ванильного инструмента
            ps.mulPose(TO_ITEM);
            ps.scale(0.62f, 0.62f, 0.62f);              // юзер: «надо увеличить пушку» (было 0.42)
            ps.translate(0, 6 * PX, -6.8f * PX);        // хват на рукояти
        }

        VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buf, RenderType.entityCutoutNoCull(PARTS), false, stack.hasFoil());
        for (Cube c : BODY) cube(ps, vc, c, light, overlay);
        for (int i = 0; i < VENTS.size(); i++) cube(ps, vc, stage > i % 3 ? VENTS.get(i) : VENTS.get(i).as(Mat.DIM), light, overlay);
        for (int i = 0; i < LEDS.size(); i++) cube(ps, vc, stage > i ? LEDS.get(i) : LEDS.get(i).as(Mat.LAMP_OFF), light, overlay);
        for (int i = 0; i < COILS.size(); i++) cube(ps, vc, stage * 2 > i % 6 ? COILS.get(i) : COILS.get(i).as(Mat.DIM), light, overlay);
        cube(ps, vc, ready ? CELL_WINDOW : CELL_WINDOW.as(Mat.DIM), light, overlay);
        if (stage > 0) {   // дуга своим буфером после всего металла
            VertexConsumer arc = buf.getBuffer(RenderType.entityTranslucentEmissive(ARC));
            float len = ARC_LEN * stage / 3f;
            arcQuad(ps, arc, 0, frame, len, 2.5f, -1, overlay);
            arcQuad(ps, arc, 1, frame, len, -0.9f, 1, overlay);
        }
        ps.popPose();
    }

    /** Полотно дуги в плоскости x = 0: от шины (y0) на ARC_W к середине (dir), от корпуса к дулу на len; кадр из ленты 8×320. */
    private static void arcQuad(PoseStack ps, VertexConsumer vc, int sheet, int frame, float len, float y0, int dir, int overlay) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        float u0 = sheet * ARC_W / 8f, u1 = (sheet * ARC_W + ARC_W) / 8f;
        float v0 = frame * ARC_LEN / 320f, v1 = (frame * ARC_LEN + len) / 320f;
        float ya = y0 * PX, yb = (y0 + dir * ARC_W) * PX, za = ARC_Z * PX, zb = (ARC_Z - len) * PX;
        float[][] p = {{ya, za}, {yb, za}, {yb, zb}, {ya, zb}}, uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
        for (int k = 0; k < 4; k++)
            vc.vertex(pose, 0, p[k][0], p[k][1]).color(1f, 1f, 1f, 1f).uv(uv[k][0], uv[k][1])
                    .overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(normal, 1, 0, 0).endVertex();
    }

    /** Брусок: каждая грань берёт плитку своего материала, 1 тексель на пиксель модели (до 16). */
    private static void cube(PoseStack ps, VertexConsumer vc, Cube c, int light, int overlay) {
        ps.pushPose();
        ps.translate(c.x * PX, c.y * PX, c.z * PX);
        if (c.xRot != 0) ps.mulPose(Axis.XP.rotationDegrees(c.xRot));
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
