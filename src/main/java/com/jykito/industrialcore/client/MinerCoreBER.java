package com.jykito.industrialcore.client;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MinerCoreBER implements BlockEntityRenderer<MinerCoreBlockEntity> {

    public static final ResourceLocation DRILL_MODEL_RL =
            new ResourceLocation("industrial_core", "block/drill_3_ber");
    public static final ResourceLocation SCREW_MODEL_RL =
            new ResourceLocation("industrial_core", "block/screw_ber");

    public MinerCoreBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(MinerCoreBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {

        BlockState state = be.getBlockState();
        if (!state.getValue(MinerCoreBlock.FORMED)) return;

        var mc = Minecraft.getInstance();
        BakedModel drillModel = mc.getModelManager().getModel(DRILL_MODEL_RL);
        BakedModel screwModel = mc.getModelManager().getModel(SCREW_MODEL_RL);
        if (drillModel == mc.getModelManager().getMissingModel()) return;

        Direction facing = state.getValue(MinerCoreBlock.FACING);
        float yDeg = switch (facing) {
            case NORTH -> 0f;
            case WEST  -> 90f;
            case SOUTH -> 180f;
            case EAST  -> 270f;
            default    -> 0f;
        };

        boolean isWorking = state.getValue(MinerCoreBlock.LIT);
        float degreesPerTick = isWorking ? 3f : 0.5f;
        float screwAngle = ((be.getLevel().getGameTime() + partialTick) * degreesPerTick) % 360f;

        RenderType rt = ModRenderTypes.entityCutoutNoCullOffset(TextureAtlas.LOCATION_BLOCKS);

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yDeg));
        ps.translate(0.5, -1.5, -0.5);
        ps.scale(2f, 2f, 2f);
        renderQuads(drillModel, ps, buf.getBuffer(rt), overlay);
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
            renderQuads(screwModel, ps, buf.getBuffer(rt), overlay);
            ps.popPose();
        }
    }

    private static void renderQuads(BakedModel model, PoseStack ps,
                                    VertexConsumer vc, int overlay) {
        PoseStack.Pose pose = ps.last();
        for (BakedQuad quad : allQuads(model)) {
            vc.putBulkData(pose, quad, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, overlay);
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
    public boolean shouldRenderOffScreen(MinerCoreBlockEntity be) { return true; }

    @Override
    public int getViewDistance() { return 128; }
}
