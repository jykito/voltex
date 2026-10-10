package com.jykito.industrialcore.client.miner;

import com.jykito.industrialcore.block.entity.DrillBeaconBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Буровой маяк: тренога и мачта, сверху кристалл (крутится и чуть покачивается; без подходящей линзы гаснет),
 * пока идёт скан — от маяка по земле расходятся три квадратные волны.
 */
public class DrillBeaconBER implements BlockEntityRenderer<DrillBeaconBlockEntity> {

    public DrillBeaconBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(DrillBeaconBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Level level = be.getLevel();
        float time = (level == null ? 0 : level.getGameTime()) + pt;
        boolean lit = be.getState() != DrillBeaconBlockEntity.State.NO_LENS;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(IndustrialMinerModel.TEX));
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        IndustrialMinerModel.render("beacon", ps, vc, light, overlay, lit);
        ps.pushPose();
        ps.translate(0, 1.72 + 0.05 * Mth.sin(time * 0.1f), 0);
        ps.mulPose(Axis.YP.rotation(lit ? time * 0.06f : 0.8f));
        ps.mulPose(Axis.XP.rotationDegrees(35));
        IndustrialMinerModel.render("crystal", ps, vc, light, overlay, lit);
        ps.popPose();
        if (be.getState() == DrillBeaconBlockEntity.State.SCANNING) waves(ps, buf, time);
        ps.popPose();
    }

    private static void waves(PoseStack ps, MultiBufferSource buf, float time) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(IndustrialMinerModel.TEX));
        float[] uv = IndustrialMinerModel.tileCenter("crystal");
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        for (int k = 0; k < 3; k++) {
            float f = (time * 0.0225f + k / 3f) % 1f, r = 0.6f + 5.4f * f, a = 0.85f * (1 - f), w = 0.06f, y = 0.02f;
            strip(vc, pose, normal, -r - w, -r - w, r + w, -r + w, y, a, uv);
            strip(vc, pose, normal, -r - w, r - w, r + w, r + w, y, a, uv);
            strip(vc, pose, normal, -r - w, -r + w, -r + w, r - w, y, a, uv);
            strip(vc, pose, normal, r - w, -r + w, r + w, r - w, y, a, uv);
        }
    }

    private static void strip(VertexConsumer vc, Matrix4f pose, Matrix3f normal, float x1, float z1, float x2, float z2, float y, float a, float[] uv) {
        float[][] p = {{x1, z1}, {x1, z2}, {x2, z2}, {x2, z1}};
        for (float[] c : p)
            vc.vertex(pose, c[0], y, c[1]).color(1f, 1f, 1f, a).uv(uv[0], uv[1]).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT).normal(normal, 0, 1, 0).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(DrillBeaconBlockEntity be) { return be.getState() == DrillBeaconBlockEntity.State.SCANNING; }
}
