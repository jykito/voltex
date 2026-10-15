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
    /** Mask of the blue belts (tools/texture_viewer/export_magnet_glow.js): always glows, the body takes real light. */
    public static final ResourceLocation PILON_CURRENT_MODEL = new ResourceLocation("industrial_core", "block/antigrav_pilon_current");

    public AntigravColumnBER(BlockEntityRendererProvider.Context ctx) {}

    // Quad cache per model: getQuads() + RandomSource were called EVERY frame (x14 per column), which ate CPU/GC.
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
        float eng   = be.getEngagedLevel();                          // 0..1 "working for the magnet"
        float bob   = Mth.sin(time * 0.08f) * (0.06f + eng * 0.05f); // working -> floats higher/stronger
        float angle = be.getRenderAngle(partialTick);               // faster while working (accumulated in the BE)

        ps.pushPose();
        ps.translate(0.5, bob + 0.30f, 0.5);          // block centre + bobbing + lift above the base
        ps.mulPose(Axis.YP.rotationDegrees(angle));   // the x2.33 stretch is BAKED into antigrav_pilon.obj -> render 1:1
        PoseStack.Pose pose = ps.last();

        // real light (it used to be FULL_BRIGHT and the column glowed by itself); the pylon floats above the base, so take the brightest of the block and the one above
        int lit = light;
        if (be.getLevel() != null) {
            int above = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above());
            lit = LightTexture.pack(Math.max(LightTexture.block(light), LightTexture.block(above)), Math.max(LightTexture.sky(light), LightTexture.sky(above)));
        }
        VertexConsumer vc = buf.getBuffer(ModRenderTypes.solidModel(TextureAtlas.LOCATION_BLOCKS)); // cull (config cullSolidFaces)
        for (BakedQuad q : allQuads(pilon))
            vc.putBulkData(pose, q, 1f, 1f, 1f, lit, overlay);
        // the "current" (blue belts) always glows: the mask goes on top with the same render type at full brightness
        BakedModel current = mc.getModelManager().getModel(PILON_CURRENT_MODEL);
        if (current != missing)
            for (BakedQuad q : allQuads(current))
                vc.putBulkData(pose, q, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, overlay);

        // glow of the grooves/corners by heat: additive (emissive), brightness = heat/max; working -> "breathes"
        float frac = Mth.clamp(be.getRenderHeat(partialTick) / (float) AntigravColumnBlockEntity.MAX_HEAT, 0f, 1f);
        // LOD: glow only up close (the pylon is drawn as usual; the distant glow is skipped)
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double gdx = be.getBlockPos().getX() + 0.5 - cam.x, gdy = be.getBlockPos().getY() + 0.5 - cam.y, gdz = be.getBlockPos().getZ() + 0.5 - cam.z;
        if (frac > 0.03f && gdx * gdx + gdy * gdy + gdz * gdz <= 40 * 40) {
            BakedModel glow = mc.getModelManager().getModel(PILON_GLOW_MODEL);
            if (glow != missing) {
                float b = Mth.clamp((0.2f + frac * 1.6f) * (1f + eng * 0.4f * Mth.sin(time * 0.25f)), 0f, 1f); // brighter (it was dim at partial heat)
                VertexConsumer gvc = buf.getBuffer(ModRenderTypes.additiveEntityGlow(TextureAtlas.LOCATION_BLOCKS));
                for (BakedQuad q : allQuads(glow))
                    gvc.putBulkData(pose, q, b, b, b, LightTexture.FULL_BRIGHT, overlay);
            }
        }

        ps.popPose();
    }

    @Override public int getViewDistance() { return 64; } // the pylon is small, do not draw it from far away
}
