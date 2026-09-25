package com.jykito.industrialcore.client;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.item.custom.StructureWandItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Каркас области, отмеченной DEV-жезлом: два угла — коробка между ними, видно сквозь стены.
 * <p>
 * Рисуется только пока жезл в руке, поэтому в обычной игре не стоит ничего: без предмета
 * обработчик выходит на первой же проверке.
 */
@Mod.EventBusSubscriber(modid = IndustrialCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class StructureSelectionRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack wand = findWand(player);
        if (wand == null) return;

        BlockPos a = StructureWandItem.getCorner(wand, true);
        BlockPos b = StructureWandItem.getCorner(wand, false);
        if (a == null) return;
        if (b == null) b = a;                       // отмечен один угол — показываем одну клетку

        double x0 = Math.min(a.getX(), b.getX()),     x1 = Math.max(a.getX(), b.getX()) + 1;
        double y0 = Math.min(a.getY(), b.getY()),     y1 = Math.max(a.getY(), b.getY()) + 1;
        double z0 = Math.min(a.getZ(), b.getZ()),     z1 = Math.max(a.getZ(), b.getZ()) + 1;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.lineWidth(2.5f);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        Matrix4f m = ps.last().pose();

        final float r = 0.2f, g = 0.95f, bl = 1f, al = 0.9f;
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        // четыре ребра по X
        for (double y : new double[]{ y0, y1 }) for (double z : new double[]{ z0, z1 }) {
            buf.vertex(m, (float) x0, (float) y, (float) z).color(r, g, bl, al).endVertex();
            buf.vertex(m, (float) x1, (float) y, (float) z).color(r, g, bl, al).endVertex();
        }
        // четыре по Y
        for (double x : new double[]{ x0, x1 }) for (double z : new double[]{ z0, z1 }) {
            buf.vertex(m, (float) x, (float) y0, (float) z).color(r, g, bl, al).endVertex();
            buf.vertex(m, (float) x, (float) y1, (float) z).color(r, g, bl, al).endVertex();
        }
        // четыре по Z
        for (double x : new double[]{ x0, x1 }) for (double y : new double[]{ y0, y1 }) {
            buf.vertex(m, (float) x, (float) y, (float) z0).color(r, g, bl, al).endVertex();
            buf.vertex(m, (float) x, (float) y, (float) z1).color(r, g, bl, al).endVertex();
        }

        tess.end();

        RenderSystem.lineWidth(1f);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        ps.popPose();
    }

    private static ItemStack findWand(LocalPlayer player) {
        for (ItemStack s : new ItemStack[]{ player.getMainHandItem(), player.getOffhandItem() })
            if (s.getItem() instanceof StructureWandItem) return s;
        return null;
    }
}
