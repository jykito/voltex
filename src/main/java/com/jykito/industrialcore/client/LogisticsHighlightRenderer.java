package com.jykito.industrialcore.client;

import com.jykito.industrialcore.IndustrialCore;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = IndustrialCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LogisticsHighlightRenderer {

    public static BlockPos highlightPos   = null;
    public static long     highlightEndMs = 0;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (highlightPos == null || System.currentTimeMillis() >= highlightEndMs) {
            highlightPos = null;
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(
            highlightPos.getX() - cam.x,
            highlightPos.getY() - cam.y,
            highlightPos.getZ() - cam.z
        );

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        Matrix4f m = ps.last().pose();

        float s = -0.002f, e = 1.002f;
        float r = 1f, g = 0.08f, b = 0.08f, a = 0.45f;

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        buf.vertex(m, s, s, s).color(r, g, b, a).endVertex();
        buf.vertex(m, e, s, s).color(r, g, b, a).endVertex();
        buf.vertex(m, e, s, e).color(r, g, b, a).endVertex();
        buf.vertex(m, s, s, e).color(r, g, b, a).endVertex();

        buf.vertex(m, s, e, s).color(r, g, b, a).endVertex();
        buf.vertex(m, s, e, e).color(r, g, b, a).endVertex();
        buf.vertex(m, e, e, e).color(r, g, b, a).endVertex();
        buf.vertex(m, e, e, s).color(r, g, b, a).endVertex();

        buf.vertex(m, s, s, s).color(r, g, b, a).endVertex();
        buf.vertex(m, s, e, s).color(r, g, b, a).endVertex();
        buf.vertex(m, e, e, s).color(r, g, b, a).endVertex();
        buf.vertex(m, e, s, s).color(r, g, b, a).endVertex();

        buf.vertex(m, s, s, e).color(r, g, b, a).endVertex();
        buf.vertex(m, e, s, e).color(r, g, b, a).endVertex();
        buf.vertex(m, e, e, e).color(r, g, b, a).endVertex();
        buf.vertex(m, s, e, e).color(r, g, b, a).endVertex();

        buf.vertex(m, s, s, s).color(r, g, b, a).endVertex();
        buf.vertex(m, s, s, e).color(r, g, b, a).endVertex();
        buf.vertex(m, s, e, e).color(r, g, b, a).endVertex();
        buf.vertex(m, s, e, s).color(r, g, b, a).endVertex();

        buf.vertex(m, e, s, s).color(r, g, b, a).endVertex();
        buf.vertex(m, e, e, s).color(r, g, b, a).endVertex();
        buf.vertex(m, e, e, e).color(r, g, b, a).endVertex();
        buf.vertex(m, e, s, e).color(r, g, b, a).endVertex();

        tess.end();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        ps.popPose();
    }
}
