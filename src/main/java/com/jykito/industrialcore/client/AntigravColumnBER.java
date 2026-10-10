package com.jykito.industrialcore.client;

import com.jykito.industrialcore.block.entity.AntigravColumnBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public class AntigravColumnBER implements BlockEntityRenderer<AntigravColumnBlockEntity> {

    public static final ResourceLocation PILON_MODEL = new ResourceLocation("industrial_core", "block/antigrav_pilon");
    public static final ResourceLocation PILON_GLOW_MODEL = new ResourceLocation("industrial_core", "block/antigrav_pilon_glow");
    /** Маска голубых поясов (tools/texture_viewer/export_magnet_glow.js) — светится всегда, корпус при настоящем свете. */
    public static final ResourceLocation PILON_CURRENT_MODEL = new ResourceLocation("industrial_core", "block/antigrav_pilon_current");

    public AntigravColumnBER(BlockEntityRendererProvider.Context ctx) {}

    // Кэш квадов на модель: getQuads() + RandomSource дёргались КАЖДЫЙ кадр (×14 на колонну) — жрало CPU/GC.
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
    public void render(AntigravColumnBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel missing = mc.getModelManager().getMissingModel();
        BakedModel pilon = mc.getModelManager().getModel(PILON_MODEL);
        if (pilon == missing) return;

        float time = (be.getLevel().getGameTime() + partialTick);
        float eng   = be.getEngagedLevel();                          // 0..1 «работает на магнит»
        float bob   = Mth.sin(time * 0.08f) * (0.06f + eng * 0.05f); // работает → парит выше/сильнее
        float angle = be.getRenderAngle(partialTick);               // быстрее при работе (накопл. в BE)

        ps.pushPose();
        ps.translate(0.5, bob + 0.30f, 0.5);          // центр блока + бобинг + подъём над подставкой
        ps.mulPose(Axis.YP.rotationDegrees(angle));   // растяжка ×2.33 ЗАПЕЧЕНА в antigrav_pilon.obj → рендерим 1:1
        PoseStack.Pose pose = ps.last();

        // настоящий свет (раньше FULL_BRIGHT — колонна светилась сама); пилон парит над подставкой — берём светлейшее из блока и над ним
        int lit = light;
        if (be.getLevel() != null) {
            int above = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above());
            lit = LightTexture.pack(Math.max(LightTexture.block(light), LightTexture.block(above)), Math.max(LightTexture.sky(light), LightTexture.sky(above)));
        }
        VertexConsumer vc = buf.getBuffer(ModRenderTypes.solidModel(TextureAtlas.LOCATION_BLOCKS)); // cull (конфиг cullSolidFaces)
        for (BakedQuad q : allQuads(pilon))
            vc.putBulkData(pose, q, 1f, 1f, 1f, lit, overlay);
        // «ток» (голубые пояса) светится всегда: маска поверх тем же типом рендера на полной яркости
        BakedModel current = mc.getModelManager().getModel(PILON_CURRENT_MODEL);
        if (current != missing)
            for (BakedQuad q : allQuads(current))
                vc.putBulkData(pose, q, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, overlay);

        // свечение выемок/углов по нагреву: аддитив (эмиссив), яркость = тепло/макс; работает → «дышит»
        float frac = Mth.clamp(be.getRenderHeat(partialTick) / (float) AntigravColumnBlockEntity.MAX_HEAT, 0f, 1f);
        // LOD: свечение только вблизи (пилон рисуется как обычно; далёкий glow опускаем)
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double gdx = be.getBlockPos().getX() + 0.5 - cam.x, gdy = be.getBlockPos().getY() + 0.5 - cam.y, gdz = be.getBlockPos().getZ() + 0.5 - cam.z;
        if (frac > 0.03f && gdx * gdx + gdy * gdy + gdz * gdz <= 40 * 40) {
            BakedModel glow = mc.getModelManager().getModel(PILON_GLOW_MODEL);
            if (glow != missing) {
                float b = Mth.clamp((0.2f + frac * 1.6f) * (1f + eng * 0.4f * Mth.sin(time * 0.25f)), 0f, 1f); // ярче (было тускло на неполном тепле)
                VertexConsumer gvc = buf.getBuffer(ModRenderTypes.additiveEntityGlow(TextureAtlas.LOCATION_BLOCKS));
                for (BakedQuad q : allQuads(glow))
                    gvc.putBulkData(pose, q, b, b, b, LightTexture.FULL_BRIGHT, overlay);
            }
        }

        ps.popPose();
    }

    @Override public int getViewDistance() { return 64; } // пилон маленький — не рисуем издалека
}
