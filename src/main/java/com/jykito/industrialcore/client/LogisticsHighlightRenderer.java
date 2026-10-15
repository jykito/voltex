package com.jykito.industrialcore.client;
import com.mojang.blaze3d.vertex.PoseStack;


import net.neoforged.fml.common.EventBusSubscriber;
import com.jykito.industrialcore.IndustrialCore;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = IndustrialCore.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
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
        Matrix4f m = ps.last().pose();

        float s = -0.002f, e = 1.002f;
        float r = 1f, g = 0.08f, b = 0.08f, a = 0.45f;

        BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        buf.addVertex(m, s, s, s).setColor(r, g, b, a);
        buf.addVertex(m, e, s, s).setColor(r, g, b, a);
        buf.addVertex(m, e, s, e).setColor(r, g, b, a);
        buf.addVertex(m, s, s, e).setColor(r, g, b, a);

        buf.addVertex(m, s, e, s).setColor(r, g, b, a);
        buf.addVertex(m, s, e, e).setColor(r, g, b, a);
        buf.addVertex(m, e, e, e).setColor(r, g, b, a);
        buf.addVertex(m, e, e, s).setColor(r, g, b, a);

        buf.addVertex(m, s, s, s).setColor(r, g, b, a);
        buf.addVertex(m, s, e, s).setColor(r, g, b, a);
        buf.addVertex(m, e, e, s).setColor(r, g, b, a);
        buf.addVertex(m, e, s, s).setColor(r, g, b, a);

        buf.addVertex(m, s, s, e).setColor(r, g, b, a);
        buf.addVertex(m, e, s, e).setColor(r, g, b, a);
        buf.addVertex(m, e, e, e).setColor(r, g, b, a);
        buf.addVertex(m, s, e, e).setColor(r, g, b, a);

        buf.addVertex(m, s, s, s).setColor(r, g, b, a);
        buf.addVertex(m, s, s, e).setColor(r, g, b, a);
        buf.addVertex(m, s, e, e).setColor(r, g, b, a);
        buf.addVertex(m, s, e, s).setColor(r, g, b, a);

        buf.addVertex(m, e, s, s).setColor(r, g, b, a);
        buf.addVertex(m, e, e, s).setColor(r, g, b, a);
        buf.addVertex(m, e, e, e).setColor(r, g, b, a);
        buf.addVertex(m, e, s, e).setColor(r, g, b, a);

        com.mojang.blaze3d.vertex.MeshData mesh = buf.build();
        if (mesh != null) com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        ps.popPose();
    }
}
