package com.jykito.industrialcore.client;

import com.jykito.industrialcore.block.entity.NuclearReactorBlockEntity;
import com.jykito.industrialcore.block.NuclearReactorBlock;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class NuclearReactorBER implements BlockEntityRenderer<NuclearReactorBlockEntity> {

    public static final ResourceLocation IDLE_MODEL_RL =
            new ResourceLocation("industrial_core", "block/nuclear_reactor_3d_ber");
    public static final ResourceLocation ACTIVE_MODEL_RL =
            new ResourceLocation("industrial_core", "block/nuclear_reactor_3d_on_ber");
    public static final ResourceLocation EMISSIVE_ACTIVE_MODEL_RL =
            new ResourceLocation("industrial_core", "block/nuclear_reactor_3d_emissive_on_ber");

    private static final ResourceLocation GLOW_TEX =
            new ResourceLocation("industrial_core", "textures/effect/sphere_glow.png");

    public NuclearReactorBER(BlockEntityRendererProvider.Context ctx) {}

    private static final java.util.Map<BakedModel, List<BakedQuad>> QUAD_CACHE = new java.util.WeakHashMap<>();
    private static List<BakedQuad> allQuads(BakedModel model) {
        return QUAD_CACHE.computeIfAbsent(model, m -> {
            RandomSource r = RandomSource.create();
            List<BakedQuad> list = new ArrayList<>(m.getQuads(null, null, r, ModelData.EMPTY, null));
            for (Direction d : Direction.values()) list.addAll(m.getQuads(null, d, r, ModelData.EMPTY, null));
            return list;
        });
    }

    @Override
    public void render(NuclearReactorBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {

        if (!be.isFormed()) return;

        boolean lit = be.getBlockState().getValue(NuclearReactorBlock.LIT);
        ResourceLocation modelRl = lit ? ACTIVE_MODEL_RL : IDLE_MODEL_RL;

        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelRl);
        if (model == Minecraft.getInstance().getModelManager().getMissingModel()) return;

        Direction facing = be.getBlockState().getValue(NuclearReactorBlock.FACING);
        float yDeg = switch (facing) {
            case NORTH -> 0f;
            case WEST  -> 90f;
            case SOUTH -> 180f;
            case EAST  -> 270f;
            default    -> 0f;
        };

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yDeg));
        ps.translate(-0.5, -0.5, -0.5);
        ps.translate(1.0, -1.0, 0.0);
        ps.scale(2f, 2f, 2f);

        RenderType rt = ModRenderTypes.entityCutoutNoCullOffset(TextureAtlas.LOCATION_BLOCKS);
        VertexConsumer vc = buf.getBuffer(rt);
        PoseStack.Pose pose = ps.last();

        for (BakedQuad quad : allQuads(model)) {
            vc.putBulkData(pose, quad, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, overlay);
        }

        if (lit) {
            BakedModel emissiveModel = Minecraft.getInstance().getModelManager().getModel(EMISSIVE_ACTIVE_MODEL_RL);
            if (emissiveModel != Minecraft.getInstance().getModelManager().getMissingModel()) {
                VertexConsumer evc = buf.getBuffer(ModRenderTypes.additiveEntityGlow(TextureAtlas.LOCATION_BLOCKS));
                PoseStack.Pose epose = ps.last();
                for (BakedQuad quad : allQuads(emissiveModel)) {
                    evc.putBulkData(epose, quad, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, overlay);
                }
            }
        }

        ps.popPose();

        if (lit) renderSurfaceGlows(be, ps, buf, partialTick);
    }

    private void renderSurfaceGlows(NuclearReactorBlockEntity be, PoseStack ps,
                                     MultiBufferSource buf, float partialTick) {
        Direction facing = be.getBlockState().getValue(NuclearReactorBlock.FACING);
        Direction rightDir = facing.getClockWise();
        Direction leftDir  = facing.getCounterClockWise();

        long gameTime = Objects.requireNonNull(be.getLevel()).getGameTime();
        float pulse = 0.82f + 0.18f * Mth.sin((gameTime + partialTick) * 0.05f);

        double cx = 0.5, cy = 0.5, cz = 0.5, d = 1.53;

        double lx = cx + leftDir.getStepX() * d;
        double lz = cz + leftDir.getStepZ() * d;
        surfaceGlow(ps, buf, lx, cy, lz, leftDir,
                0.1f, 1.0f, 0.1f, 0.07f * pulse, 2.2f, 2.2f);
        volumeGlow(ps, buf, lx + leftDir.getStepX() * 0.2, cy, lz + leftDir.getStepZ() * 0.2,
                0.1f, 1.0f, 0.1f, 0.06f * pulse, 0.8f);

        double rx = cx + rightDir.getStepX() * d;
        double rz = cz + rightDir.getStepZ() * d;
        surfaceGlow(ps, buf, rx, cy, rz, rightDir,
                0.1f, 1.0f, 0.1f, 0.07f * pulse, 2.2f, 2.2f);
        volumeGlow(ps, buf, rx + rightDir.getStepX() * 0.2, cy, rz + rightDir.getStepZ() * 0.2,
                0.1f, 1.0f, 0.1f, 0.06f * pulse, 0.8f);

        double fx = cx + facing.getStepX() * d;
        double fz = cz + facing.getStepZ() * d;

        surfaceGlow(ps, buf, fx, cy + 0.1, fz, facing,
                0.55f, 1.0f, 0.65f, 0.05f * pulse, 2.5f, 1.8f);

        volumeGlow(ps, buf, fx + facing.getStepX() * 0.15, cy + 0.1, fz + facing.getStepZ() * 0.15,
                0.55f, 1.0f, 0.65f, 0.04f * pulse, 0.5f);
    }

    private void surfaceGlow(PoseStack ps, MultiBufferSource buf,
                              double x, double y, double z, Direction faceNormal,
                              float r, float g, float b, float a,
                              float halfW, float halfH) {
        ps.pushPose();
        ps.translate(x, y, z);
        alignToFaceNormal(ps, faceNormal);
        Matrix4f m = ps.last().pose();
        VertexConsumer vc = buf.getBuffer(ModRenderTypes.ADDITIVE_GLOW.apply(GLOW_TEX));
        vc.vertex(m, -halfW, -halfH, 0).color(r, g, b, a).uv(0f, 1f).endVertex();
        vc.vertex(m,  halfW, -halfH, 0).color(r, g, b, a).uv(1f, 1f).endVertex();
        vc.vertex(m,  halfW,  halfH, 0).color(r, g, b, a).uv(1f, 0f).endVertex();
        vc.vertex(m, -halfW,  halfH, 0).color(r, g, b, a).uv(0f, 0f).endVertex();
        ps.popPose();
    }

    private void volumeGlow(PoseStack ps, MultiBufferSource buf,
                             double x, double y, double z,
                             float r, float g, float b, float a, float size) {
        ps.pushPose();
        ps.translate(x, y, z);
        Matrix4f m0 = ps.last().pose();
        VertexConsumer vc = buf.getBuffer(ModRenderTypes.ADDITIVE_GLOW.apply(GLOW_TEX));
        for (int i = 0; i < 3; i++) {
            ps.pushPose();
            if (i == 1) ps.mulPose(Axis.YP.rotationDegrees(60f));
            if (i == 2) ps.mulPose(Axis.XP.rotationDegrees(60f));
            Matrix4f m = ps.last().pose();
            vc.vertex(m, -size, -size, 0).color(r, g, b, a).uv(0f, 0f).endVertex();
            vc.vertex(m,  size, -size, 0).color(r, g, b, a).uv(1f, 0f).endVertex();
            vc.vertex(m,  size,  size, 0).color(r, g, b, a).uv(1f, 1f).endVertex();
            vc.vertex(m, -size,  size, 0).color(r, g, b, a).uv(0f, 1f).endVertex();
            ps.popPose();
        }
        ps.popPose();
    }

    private void alignToFaceNormal(PoseStack ps, Direction dir) {
        switch (dir) {
            case EAST  -> ps.mulPose(Axis.YP.rotationDegrees( 90f));
            case WEST  -> ps.mulPose(Axis.YP.rotationDegrees(-90f));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180f));
            case UP    -> ps.mulPose(Axis.XP.rotationDegrees(-90f));
            case DOWN  -> ps.mulPose(Axis.XP.rotationDegrees( 90f));
            default    -> {}
        }
    }

    @Override public boolean shouldRenderOffScreen(NuclearReactorBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 128; }
}
