package com.jykito.industrialcore.client;

import com.jykito.industrialcore.entity.PlasmaWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class PlasmaWaveRenderer extends EntityRenderer<PlasmaWaveEntity> {

    private static final int   DOME_RINGS  = 9;
    private static final int   SEGMENTS    = 48;
    private static final float MAX_RADIUS  = 8.0f;
    private static final float THICKNESS   = 0.28f;

    private static final int CR = 80;
    private static final int CG = 180;
    private static final int CB = 255;

    public PlasmaWaveRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(PlasmaWaveEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }

    @Override
    public void render(PlasmaWaveEntity entity, float yaw, float partialTick,
                       PoseStack ps, MultiBufferSource buffer, int packedLight) {
        float progress = Math.min((entity.tickCount + partialTick) / PlasmaWaveEntity.MAX_LIFE, 1.0f);
        if (progress <= 0f) return;

        float R     = progress * MAX_RADIUS;
        float alpha = 1.0f - progress;
        if (alpha <= 0f) return;

        VertexConsumer vc = buffer.getBuffer(ModRenderTypes.plasmaRing());

        ps.pushPose();
        Matrix4f mat = ps.last().pose();

        for (int ring = 0; ring < DOME_RINGS; ring++) {
            double theta  = Math.toRadians(ring * 80.0 / (DOME_RINGS - 1));
            float  ringY  = R * (float) Math.sin(theta);
            float  ringR  = R * (float) Math.cos(theta);
            if (ringR < 0.05f) continue;

            float outerR = ringR;
            float innerR = Math.max(0f, ringR - THICKNESS);

            int ca = Math.max(0, (int)(alpha * (1.0f - ring * 0.07f) * 210));
            if (ca <= 0) continue;

            for (int seg = 0; seg < SEGMENTS; seg++) {
                float a1 = (float)(seg       * 2 * Math.PI / SEGMENTS);
                float a2 = (float)((seg + 1) * 2 * Math.PI / SEGMENTS);
                float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                float c2 = Mth.cos(a2), s2 = Mth.sin(a2);

                vc.vertex(mat, outerR * c1, ringY, outerR * s1).color(CR, CG, CB, ca).endVertex();
                vc.vertex(mat, outerR * c2, ringY, outerR * s2).color(CR, CG, CB, ca).endVertex();
                vc.vertex(mat, innerR * c2, ringY, innerR * s2).color(CR, CG, CB, ca).endVertex();
                vc.vertex(mat, innerR * c1, ringY, innerR * s1).color(CR, CG, CB, ca).endVertex();
            }
        }

        ps.popPose();
    }
}
