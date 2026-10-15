package com.jykito.industrialcore.client;
import net.minecraft.client.resources.model.ModelResourceLocation;

import com.jykito.industrialcore.block.custom.MinerCoreBlock;
import com.jykito.industrialcore.block.entity.MinerCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.jykito.industrialcore.client.ModRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MinerCoreBER implements BlockEntityRenderer<MinerCoreBlockEntity> {

    public static final ModelResourceLocation DRILL_MODEL_RL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("industrial_core", "block/drill_3_ber"));
    /**
     * Drill glow layer: the same geometry, but a texture where everything that does not glow is erased to transparent.
     * It is drawn in a second pass at full brightness, so only the remaining pixels glow,
     * and the drill itself takes real world light. The layout must match {@code drill_3}.
     */
    public static final ModelResourceLocation DRILL_GLOW_MODEL_RL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("industrial_core", "block/drill_3_glow_ber"));
    public static final ModelResourceLocation SCREW_MODEL_RL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("industrial_core", "block/screw_ber"));

    public MinerCoreBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(MinerCoreBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {

        BlockState state = be.getBlockState();
        if (!state.getValue(MinerCoreBlock.FORMED)) return;

        Direction facing = state.getValue(MinerCoreBlock.FACING);
        boolean isWorking = state.getValue(MinerCoreBlock.LIT);
        float degreesPerTick = isWorking ? 3f : 0.5f;
        float screwAngle = ((be.getLevel().getGameTime() + partialTick) * degreesPerTick) % 360f;
        renderFormed(ps, buf, facing, screwAngle, light, overlay);
    }

    /**
     * A formed miner from the core block: the drill (at light {@code light}, glow layer at full brightness) and the auger at angle
     * {@code screwAngle}. Called by the BER and by the structure preview in the guide (where there is no block entity).
     */
    public static void renderFormed(PoseStack ps, MultiBufferSource buf, Direction facing, float screwAngle, int light, int overlay) {
        var mc = Minecraft.getInstance();
        BakedModel drillModel = mc.getModelManager().getModel(DRILL_MODEL_RL);
        BakedModel glowModel  = mc.getModelManager().getModel(DRILL_GLOW_MODEL_RL);
        BakedModel screwModel = mc.getModelManager().getModel(SCREW_MODEL_RL);
        if (drillModel == mc.getModelManager().getMissingModel()) return;
        boolean hasGlow = glowModel != mc.getModelManager().getMissingModel();

        float yDeg = switch (facing) {
            case NORTH -> 0f;
            case WEST  -> 90f;
            case SOUTH -> 180f;
            case EAST  -> 270f;
            default    -> 0f;
        };

        RenderType rt = ModRenderTypes.entityCutoutNoCullOffset(TextureAtlas.LOCATION_BLOCKS);

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yDeg));
        ps.translate(0.5, -1.5, -0.5);
        ps.scale(2f, 2f, 2f);
        // The body takes world light, the glow layer goes on top with the same transform.
        renderQuads(drillModel, ps, buf.getBuffer(rt), light, overlay);
        if (hasGlow) renderQuads(glowModel, ps, buf.getBuffer(rt), LightTexture.FULL_BRIGHT, overlay);
        ps.popPose();

        if (screwModel != mc.getModelManager().getMissingModel()) {
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(yDeg));

            ps.translate(-0.012f, -0.806f, -1.076f);
            ps.mulPose(Axis.YP.rotationDegrees(screwAngle));
            ps.translate(0.012f, 0.806f, 1.076f);
            ps.translate(0.5, -1.5, -0.5);
            ps.scale(2f, 2f, 2f);
            renderQuads(screwModel, ps, buf.getBuffer(rt), light, overlay);
            ps.popPose();
        }
    }

    private static void renderQuads(BakedModel model, PoseStack ps,
                                    VertexConsumer vc, int light, int overlay) {
        PoseStack.Pose pose = ps.last();
        for (BakedQuad quad : allQuads(model)) {
            vc.putBulkData(pose, quad, 1f, 1f, 1f, 1f, light, overlay);
        }
    }

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
    public boolean shouldRenderOffScreen(MinerCoreBlockEntity be) { return false; } // frustum culling (bbox p-2..p+3 covers the drill)

    @Override
    public int getViewDistance() { return 96; }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(MinerCoreBlockEntity be) {
        return be.renderBounds();
    }
}
