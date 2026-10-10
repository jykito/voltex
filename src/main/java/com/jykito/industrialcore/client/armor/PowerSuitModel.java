package com.jykito.industrialcore.client.armor;

import com.jykito.industrialcore.IndustrialCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.EntityRenderersEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Силовая броня (Nexite и Plasma) своей моделью: тёмный комбинезон и тонкие пластины с наклоном поверх него, экзоскелет,
 * огни-точки (приёмы MekaSuit, раскладка своя). Четыре модели по слотам, каждая показывает только свои детали.
 * Модель общая на оба набора: разница только в текстуре, её выбирает предмет (nexite_suit.png / plasma_suit.png,
 * одинаковая раскладка участков).
 * <p>
 * Детали заданы в координатах наброска (tools/texture_viewer/armor_suit2.html): ноги на y=0, голова 24..32,
 * лицо на +z, +x — левая сторона тела. В модель они переводятся поворотом на 180° вокруг X (y вниз, лицо на −z),
 * поэтому наклон по X сохраняется, а по Y и Z меняет знак. Огни рисуются вторым проходом на полной яркости.
 * Под шлемом голова игрока скрыта (ClientForgeEvents), поэтому шлем может быть скруглённым, без торчащих углов скина.
 */
public class PowerSuitModel extends HumanoidModel<LivingEntity> {

    public enum Piece { HELMET, CHEST, LEGS, BOOTS }

    /** Кость и её точка в координатах наброска (центр плеча/бедра, низ шеи). */
    private enum Bone {
        HEAD("head", 0, 24), BODY("body", 0, 24),
        LEFT_ARM("left_arm", 5, 22), RIGHT_ARM("right_arm", -5, 22),
        LEFT_LEG("left_leg", 2, 12), RIGHT_LEG("right_leg", -2, 12);
        final String name; final float ax, ay;
        Bone(String name, float ax, float ay) { this.name = name; this.ax = ax; this.ay = ay; }
        Bone mirror() { return switch (this) { case LEFT_ARM -> RIGHT_ARM; case RIGHT_ARM -> LEFT_ARM; case LEFT_LEG -> RIGHT_LEG; case RIGHT_LEG -> LEFT_LEG; default -> this; }; }
    }

    /** Материал детали = участок 80×40 текстуры костюма (256×128): плиты — узор брони, огни и камень — сплошной цвет. */
    private enum Mat {
        PLATE(0, 0), PLATE_L(80, 0), DARK(160, 0), UNDER(0, 40), VISOR(80, 40), LED(160, 40), CORE(0, 80), FACET(80, 80), VISOR_HI(160, 80);
        final int u, v;
        Mat(int u, int v) { this.u = u; this.v = v; }
        boolean glow() { return this == VISOR || this == LED || this == CORE || this == FACET || this == VISOR_HI; }
    }

    private record Part(String name, Bone bone, float[] from, float[] to, float[] rot, Mat mat, float inflate) {}

    /** Детали одного слота. Шлем слегка увеличен вокруг центра головы, чтобы второй слой скина не пролезал. */
    private static List<Part> spec(Piece piece) {
        List<Part> out = new ArrayList<>();
        Spec s = new Spec(out);
        switch (piece) {
            case HELMET -> {
                // восьмигранный шлем: ядро, грани, срезанные вертикальные рёбра и скосы темени; голова игрока под ним скрыта
                s.scale = 1.05f;
                s.p(Bone.HEAD, -3.4f, 24.6f, -3.4f, 3.4f, 31.4f, 3.4f, 0, 0, 0, Mat.PLATE);      // ядро
                s.sym(Bone.HEAD, 3.4f, 25.2f, -2.6f, 4.2f, 30.8f, 2.6f, 0, 0, 0, Mat.PLATE);     // боковые грани
                s.p(Bone.HEAD, -2.6f, 25.2f, -4.2f, 2.6f, 30.8f, -3.4f, 0, 0, 0, Mat.PLATE);     // задняя грань
                s.p(Bone.HEAD, -2.6f, 25.2f, 3.4f, 2.6f, 30.8f, 4.0f, 0, 0, 0, Mat.DARK);        // лицевая пластина
                s.p(Bone.HEAD, -2.6f, 31.4f, -2.6f, 2.6f, 32.2f, 2.6f, 0, 0, 0, Mat.PLATE_L);    // темя
                s.sym(Bone.HEAD, 1.97f, 25.2f, 2.72f, 4.27f, 30.8f, 3.52f, 0, 45, 0, Mat.PLATE);   // срез переднего ребра
                s.sym(Bone.HEAD, 1.97f, 25.2f, -3.52f, 4.27f, 30.8f, -2.72f, 0, -45, 0, Mat.PLATE); // срез заднего ребра
                s.p(Bone.HEAD, -2.6f, 30.8f, 2.0f, 2.6f, 31.6f, 4.2f, 41, 0, 0, Mat.PLATE);      // скос темени спереди
                s.p(Bone.HEAD, -2.6f, 30.8f, -4.2f, 2.6f, 31.6f, -2.0f, -41, 0, 0, Mat.PLATE);   // скос темени сзади
                s.sym(Bone.HEAD, 2.0f, 30.8f, -2.6f, 4.2f, 31.6f, 2.6f, 0, 0, -41, Mat.PLATE);   // скосы темени по бокам
                s.sym(Bone.HEAD, 2.3f, 30.4f, 2.3f, 3.7f, 31.6f, 3.7f, 0, 45, 0, Mat.PLATE);     // заглушки углов темени
                s.sym(Bone.HEAD, 2.3f, 30.4f, -3.7f, 3.7f, 31.6f, -2.3f, 0, 45, 0, Mat.PLATE);
                // визор: стекло с перемычкой на две линзы, светлая полоса по верху, тёмная окантовка снизу,
                // косой блик на левой линзе (признак стекла) и две риски HUD на правой; загибы на срезах
                s.p(Bone.HEAD, -2.6f, 27.0f, 4.0f, 2.6f, 29.4f, 4.5f, 0, 0, 0, Mat.VISOR);       // стекло
                s.p(Bone.HEAD, -2.6f, 28.9f, 4.45f, 2.6f, 29.4f, 4.65f, 0, 0, 0, Mat.VISOR_HI);  // светлая полоса
                s.p(Bone.HEAD, -2.8f, 26.7f, 3.9f, 2.8f, 27.1f, 4.7f, 0, 0, 0, Mat.DARK);        // окантовка снизу
                s.p(Bone.HEAD, -0.15f, 27.1f, 4.45f, 0.15f, 28.9f, 4.62f, 0, 0, 0, Mat.DARK);    // перемычка
                s.p(Bone.HEAD, -2.0f, 27.65f, 4.45f, -0.9f, 27.95f, 4.62f, 0, 0, 35, Mat.FACET);  // блик
                s.p(Bone.HEAD, -0.75f, 27.8f, 4.45f, -0.4f, 28.05f, 4.62f, 0, 0, 35, Mat.FACET);
                s.p(Bone.HEAD, 0.6f, 28.2f, 4.45f, 1.8f, 28.4f, 4.6f, 0, 0, 0, Mat.VISOR_HI);    // риски HUD
                s.p(Bone.HEAD, 0.6f, 27.7f, 4.45f, 1.2f, 27.9f, 4.6f, 0, 0, 0, Mat.VISOR_HI);
                s.sym(Bone.HEAD, 2.15f, 27.0f, 3.05f, 4.45f, 29.4f, 3.55f, 0, 45, 0, Mat.VISOR); // загибы на срезах
                s.sym(Bone.HEAD, 2.15f, 28.9f, 3.45f, 4.45f, 29.4f, 3.65f, 0, 45, 0, Mat.VISOR_HI);
                s.p(Bone.HEAD, -3.0f, 29.6f, 3.6f, 3.0f, 30.6f, 5.0f, -22, 0, 0, Mat.PLATE_L);   // козырёк
                s.p(Bone.HEAD, -0.8f, 32.0f, -3.0f, 0.8f, 32.8f, 0.0f, -8, 0, 0, Mat.DARK);      // гребень сзади
                s.p(Bone.HEAD, -0.8f, 31.8f, -0.4f, 0.8f, 32.6f, 2.4f, 18, 0, 0, Mat.DARK);      // гребень, спадает вперёд
                s.p(Bone.HEAD, -0.8f, 30.9f, 2.2f, 0.8f, 31.9f, 3.9f, 38, 0, 0, Mat.DARK);       // гребень у лба
                s.p(Bone.HEAD, -0.3f, 32.7f, -2.4f, 0.3f, 33.0f, 0.4f, -8, 0, 0, Mat.LED);
                s.p(Bone.HEAD, -1.8f, 24.6f, 3.6f, 1.8f, 26.0f, 4.9f, -12, 0, 0, Mat.DARK);      // подбородок
                s.sym(Bone.HEAD, 0.5f, 25.0f, 4.8f, 1.1f, 25.5f, 5.1f, -12, 0, 0, Mat.LED);      // вентиляция
                s.sym(Bone.HEAD, 4.2f, 26.4f, -1.4f, 5.0f, 29.4f, 1.4f, 0, 0, 0, Mat.DARK);      // ушные модули
                s.sym(Bone.HEAD, 5.0f, 27.6f, -0.4f, 5.3f, 28.4f, 0.4f, 0, 0, 0, Mat.LED);
                s.p(Bone.HEAD, -2.8f, 24.4f, -4.6f, 2.8f, 25.8f, -3.8f, 12, 0, 0, Mat.PLATE);    // затылочный щиток
            }
            case CHEST -> {
                s.under(Bone.BODY, -4, 12, -2, 4, 24, 2);
                s.under(Bone.LEFT_ARM, 4, 12, -2, 8, 24, 2);
                s.under(Bone.RIGHT_ARM, -8, 12, -2, -4, 24, 2);
                s.p(Bone.BODY, -3.0f, 23.2f, -2.6f, 3.0f, 24.6f, 2.6f, 0, 0, 0, Mat.PLATE_L);    // воротник
                s.p(Bone.BODY, -3.8f, 20.6f, 1.9f, 3.8f, 23.3f, 3.0f, -10, 0, 0, Mat.PLATE);     // верх груди
                s.sym(Bone.BODY, 1.2f, 18.8f, 1.9f, 4.2f, 21.6f, 2.9f, -5, -8, 0, Mat.PLATE);    // грудные пластины веером
                // ядро: гранёный камень ромбом в оправе со срезанными углами, от оправы к краям груди — жилы
                s.p(Bone.BODY, -1.9f, 18.3f, 2.4f, 1.9f, 21.6f, 3.2f, 0, 0, 0, Mat.DARK);        // гнездо
                s.p(Bone.BODY, -1.9f, 21.6f, 2.6f, 1.9f, 22.2f, 3.6f, 0, 0, 0, Mat.PLATE_L);     // оправа сверху
                s.p(Bone.BODY, -1.9f, 17.7f, 2.6f, 1.9f, 18.3f, 3.6f, 0, 0, 0, Mat.PLATE_L);     // оправа снизу
                s.sym(Bone.BODY, 1.9f, 18.3f, 2.6f, 2.5f, 21.6f, 3.6f, 0, 0, 0, Mat.PLATE_L);    // оправа по бокам
                s.sym(Bone.BODY, 1.6f, 21.3f, 2.6f, 2.5f, 22.2f, 3.6f, 0, 0, 45, Mat.PLATE_L);   // срезы углов оправы
                s.sym(Bone.BODY, 1.6f, 17.7f, 2.6f, 2.5f, 18.6f, 3.6f, 0, 0, 45, Mat.PLATE_L);
                s.p(Bone.BODY, -1.1f, 18.85f, 2.9f, 1.1f, 21.05f, 3.8f, 0, 0, 45, Mat.CORE);    // камень ромбом
                s.p(Bone.BODY, -0.55f, 19.4f, 3.6f, 0.55f, 20.5f, 4.1f, 0, 0, 45, Mat.FACET);   // светлая грань
                s.sym(Bone.BODY, 2.4f, 21.6f, 2.75f, 4.0f, 21.9f, 3.05f, 0, 0, 30, Mat.CORE);   // жилы вверх к плечам
                s.sym(Bone.BODY, 2.4f, 17.4f, 2.6f, 3.8f, 17.7f, 2.9f, 0, 0, -30, Mat.CORE);    // жилы вниз к рёбрам
                s.p(Bone.BODY, -2.6f, 16.6f, 1.9f, 2.6f, 18.2f, 2.8f, 8, 0, 0, Mat.PLATE);       // пресс внахлёст
                s.p(Bone.BODY, -2.4f, 15.0f, 1.9f, 2.4f, 16.6f, 2.7f, -6, 0, 0, Mat.DARK);
                s.p(Bone.BODY, -2.8f, 13.4f, 1.9f, 2.8f, 15.0f, 2.8f, 6, 0, 0, Mat.PLATE);
                s.sym(Bone.BODY, 3.9f, 14.0f, -1.3f, 4.5f, 21.0f, 1.3f, 0, 0, 0, Mat.DARK);      // боковые рёбра
                s.sym(Bone.BODY, 4.5f, 16.0f, -0.3f, 4.7f, 19.0f, 0.3f, 0, 0, 0, Mat.LED);
                s.p(Bone.BODY, -0.8f, 13.0f, -2.9f, 0.8f, 24.0f, -2.0f, 0, 0, 0, Mat.DARK);      // хребет
                s.p(Bone.BODY, -2.6f, 17.0f, -4.4f, 2.6f, 23.0f, -2.2f, 5, 0, 0, Mat.PLATE);     // ранец
                s.p(Bone.BODY, -1.8f, 21.0f, -4.6f, 1.8f, 21.4f, -4.3f, 5, 0, 0, Mat.LED);
                s.p(Bone.BODY, -1.8f, 19.2f, -4.5f, 1.8f, 19.6f, -4.2f, 5, 0, 0, Mat.CORE);
                s.sym(Bone.BODY, 0.5f, 22.4f, -3.3f, 5.6f, 23.4f, -2.5f, 0, -12, -5, Mat.DARK);  // скобы ранец-плечи
                s.sym(Bone.LEFT_ARM, 4.0f, 21.2f, -2.7f, 8.8f, 24.6f, 2.7f, 0, 0, -12, Mat.PLATE);   // наплечник
                s.sym(Bone.LEFT_ARM, 8.3f, 22.0f, -1.0f, 8.7f, 23.0f, 1.0f, 0, 0, -12, Mat.LED);
                s.sym(Bone.LEFT_ARM, 7.7f, 17.0f, -1.9f, 8.6f, 21.0f, 1.9f, 0, 0, 0, Mat.PLATE);     // плечевая пластина
                s.sym(Bone.LEFT_ARM, 8.4f, 13.0f, -0.5f, 9.2f, 21.0f, 0.5f, 0, 0, 4, Mat.DARK);      // стержень экзоскелета
                s.sym(Bone.LEFT_ARM, 7.8f, 15.4f, -1.2f, 9.5f, 17.0f, 1.2f, 0, 0, 0, Mat.DARK);      // локтевой шарнир
                s.sym(Bone.LEFT_ARM, 3.6f, 12.4f, -2.4f, 8.4f, 16.4f, 2.4f, 0, 0, 0, Mat.PLATE);     // наруч
                s.sym(Bone.LEFT_ARM, 4.0f, 15.6f, -2.6f, 8.0f, 16.6f, 2.6f, -6, 0, 0, Mat.PLATE_L);  // накладка наруча
                s.sym(Bone.LEFT_ARM, 8.4f, 13.0f, -0.3f, 8.7f, 15.4f, 0.3f, 0, 0, 0, Mat.LED);
                s.sym(Bone.LEFT_ARM, 4.0f, 11.9f, 1.5f, 8.0f, 12.7f, 2.6f, 0, 0, 0, Mat.PLATE_L);    // костяшки
            }
            case LEGS -> {
                s.under(Bone.LEFT_LEG, 0, 0, -2, 4, 12, 2);
                s.under(Bone.RIGHT_LEG, -4, 0, -2, 0, 12, 2);
                s.p(Bone.BODY, -4.4f, 11.6f, -2.4f, 4.4f, 13.0f, 2.4f, 0, 0, 0, Mat.DARK);      // пояс
                s.p(Bone.BODY, -1.2f, 11.8f, 2.4f, 1.2f, 12.9f, 2.8f, 0, 0, 0, Mat.LED);        // пряжка
                s.sym(Bone.LEFT_LEG, 0.4f, 8.0f, 1.9f, 3.8f, 11.6f, 2.8f, -5, 0, 0, Mat.PLATE);  // бедро спереди
                s.sym(Bone.LEFT_LEG, 3.9f, 7.0f, -1.8f, 4.8f, 11.5f, 1.8f, 0, 0, 0, Mat.PLATE);  // бедро снаружи
                s.sym(Bone.LEFT_LEG, 4.0f, 10.2f, -1.0f, 5.2f, 11.6f, 1.0f, 0, 0, 0, Mat.DARK);  // тазовый шарнир
                s.sym(Bone.LEFT_LEG, 4.5f, 2.5f, -0.4f, 5.2f, 10.0f, 0.4f, 0, 0, -3, Mat.DARK);  // стержень экзоскелета
                s.sym(Bone.LEFT_LEG, 0.6f, 5.2f, 2.0f, 3.4f, 7.6f, 3.2f, 12, 0, 0, Mat.PLATE_L); // наколенник
                s.sym(Bone.LEFT_LEG, 1.6f, 6.0f, 3.1f, 2.4f, 6.6f, 3.4f, 12, 0, 0, Mat.LED);
                s.sym(Bone.LEFT_LEG, 0.5f, 2.4f, 1.9f, 3.5f, 5.4f, 2.8f, -8, 0, 0, Mat.PLATE);   // голень
            }
            case BOOTS -> {
                s.sym(Bone.LEFT_LEG, -0.5f, 0, -2.7f, 4.5f, 1.6f, 3.7f, 0, 0, 0, Mat.DARK);     // подошва
                s.sym(Bone.LEFT_LEG, 0.2f, 1.2f, 2.4f, 3.8f, 2.4f, 4.0f, -40, 0, 0, Mat.PLATE); // скошенный нос
                s.sym(Bone.LEFT_LEG, 1.0f, 0.6f, 3.7f, 3.0f, 0.9f, 3.9f, 0, 0, 0, Mat.LED);
                s.sym(Bone.LEFT_LEG, 0.4f, 0, -3.0f, 3.6f, 2.4f, -2.4f, 0, 0, 0, Mat.PLATE);    // пятка
            }
        }
        return out;
    }

    /** Помощник наполнения: p — деталь, sym — деталь и её зеркало, under — комбинезон по форме кости. */
    private static final class Spec {
        final List<Part> out; float scale = 1f; int n;
        Spec(List<Part> out) { this.out = out; }
        void p(Bone b, float x1, float y1, float z1, float x2, float y2, float z2, float rx, float ry, float rz, Mat m) {
            float[] f = { x1, y1, z1 }, t = { x2, y2, z2 };
            if (scale != 1f) for (int i = 0; i < 3; i++) {   // вокруг центра головы (0, 28, 0)
                float c = i == 1 ? 28 : 0;
                f[i] = c + (f[i] - c) * scale; t[i] = c + (t[i] - c) * scale;
            }
            out.add(new Part("p" + n++, b, f, t, new float[]{ rx, ry, rz }, m, 0));
        }
        void sym(Bone b, float x1, float y1, float z1, float x2, float y2, float z2, float rx, float ry, float rz, Mat m) {
            p(b, x1, y1, z1, x2, y2, z2, rx, ry, rz, m);
            p(b.mirror(), -x2, y1, z1, -x1, y2, z2, rx, -ry, -rz, m);
        }
        void under(Bone b, float x1, float y1, float z1, float x2, float y2, float z2) {
            out.add(new Part("p" + n++, b, new float[]{ x1, y1, z1 }, new float[]{ x2, y2, z2 }, new float[3], Mat.UNDER, 0.3f));
        }
    }

    public static LayerDefinition createLayer(Piece piece) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        Map<Bone, PartDefinition> bones = new EnumMap<>(Bone.class);
        bones.put(Bone.HEAD, root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        bones.put(Bone.BODY, root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO));
        bones.put(Bone.RIGHT_ARM, root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5, 2, 0)));
        bones.put(Bone.LEFT_ARM, root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5, 2, 0)));
        bones.put(Bone.RIGHT_LEG, root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9f, 12, 0)));
        bones.put(Bone.LEFT_LEG, root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9f, 12, 0)));
        int i = 0;
        for (Part p : spec(piece)) {
            float w = p.to[0] - p.from[0], h = p.to[1] - p.from[1], d = p.to[2] - p.from[2];
            // свой кусок узора на каждую деталь: развёртка (2(w+d) × (d+h)) сдвигается внутри участка 80×40
            int uw = (int) Math.ceil(2 * (w + d)), uh = (int) Math.ceil(d + h), k = i++;
            int du = (k * 13) % Math.max(1, 81 - uw), dv = (k * 7) % Math.max(1, 41 - uh);
            float cx = (p.from[0] + p.to[0]) / 2, cy = (p.from[1] + p.to[1]) / 2, cz = (p.from[2] + p.to[2]) / 2;
            float deg = (float) (Math.PI / 180);
            bones.get(p.bone).addOrReplaceChild(p.name,
                    CubeListBuilder.create().texOffs(p.mat.u + du, p.mat.v + dv).addBox(-w / 2, -h / 2, -d / 2, w, h, d, new CubeDeformation(p.inflate)),
                    // набросок -> модель: y вниз, лицо на −z (поворот на 180° вокруг X)
                    PartPose.offsetAndRotation(cx - p.bone.ax, -(cy - p.bone.ay), -cz, p.rot[0] * deg, -p.rot[1] * deg, -p.rot[2] * deg));
        }
        return LayerDefinition.create(mesh, 256, 128);
    }

    private final List<ModelPart> glow = new ArrayList<>(), solid = new ArrayList<>();

    public PowerSuitModel(ModelPart root, Piece piece) {
        super(root);
        for (Part p : spec(piece)) {
            ModelPart bone = root.getChild(p.bone.name).getChild(p.name);
            (p.mat.glow() ? glow : solid).add(bone);
        }
    }

    /**
     * Поза для 3D-иконки предмета (ArmorIconRenderer): кости на местах по умолчанию без поворотов, всё видно,
     * руки и ноги чуть разведены. Модель общая с бронёй на игроке — слой брони перед своим рендером
     * сам копирует позу игрока и видимость по слоту, так что после иконки ничего восстанавливать не нужно.
     */
    public void poseForIcon() {
        young = false; crouching = false; riding = false;
        setAllVisible(true);
        head.setPos(0, 0, 0); hat.setPos(0, 0, 0); body.setPos(0, 0, 0);
        rightArm.setPos(-6.2f, 2, 0); leftArm.setPos(6.2f, 2, 0);
        rightLeg.setPos(-3.5f, 12, 0); leftLeg.setPos(3.5f, 12, 0);
        for (ModelPart p : List.of(head, hat, body, rightArm, leftArm, rightLeg, leftLeg)) p.setRotation(0, 0, 0);
    }

    /** Сначала броня с освещением мира, потом огни на полной яркости. */
    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        glow.forEach(p -> p.visible = false);
        super.renderToBuffer(ps, vc, light, overlay, r, g, b, a);
        glow.forEach(p -> p.visible = true);
        solid.forEach(p -> p.visible = false);
        super.renderToBuffer(ps, vc, LightTexture.FULL_BRIGHT, overlay, r, g, b, a);
        solid.forEach(p -> p.visible = true);
    }

    /**
     * Рука от первого лица (ClientForgeEvents.onRenderArm): только кость руки в позе руки игрока, те же два прохода.
     * Модель общая с бронёй на игроке, а слой брони перед своим рендером сам выставляет позу и видимость.
     */
    public void renderArm(PoseStack ps, VertexConsumer vc, int light, ModelPart pose, boolean right) {
        ModelPart arm = right ? rightArm : leftArm;
        arm.copyFrom(pose);
        arm.visible = true;
        glow.forEach(p -> p.visible = false);
        arm.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        glow.forEach(p -> p.visible = true);
        solid.forEach(p -> p.visible = false);
        arm.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        solid.forEach(p -> p.visible = true);
    }

    // ── слои и кэш моделей ──

    private static final Map<Piece, ModelLayerLocation> LAYERS = new EnumMap<>(Piece.class);
    static {
        for (Piece p : Piece.values())
            LAYERS.put(p, new ModelLayerLocation(new ResourceLocation(IndustrialCore.MODID, "power_suit"), p.name().toLowerCase()));
    }
    private static final Map<Piece, PowerSuitModel> CACHE = new EnumMap<>(Piece.class);

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (Piece p : Piece.values()) event.registerLayerDefinition(LAYERS.get(p), () -> createLayer(p));
    }

    /** После перезагрузки ресурсов слои пересобираются — сбрасываем кэш. */
    public static void clearCache() { CACHE.clear(); }

    public static PowerSuitModel forSlot(EquipmentSlot slot) {
        Piece piece = switch (slot) {
            case HEAD -> Piece.HELMET;
            case LEGS -> Piece.LEGS;
            case FEET -> Piece.BOOTS;
            default -> Piece.CHEST;
        };
        return CACHE.computeIfAbsent(piece, p -> new PowerSuitModel(Minecraft.getInstance().getEntityModels().bakeLayer(LAYERS.get(p)), p));
    }
}
