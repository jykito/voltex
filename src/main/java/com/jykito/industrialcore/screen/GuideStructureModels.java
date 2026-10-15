package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.client.ElectromagnetCoreBER;
import com.jykito.industrialcore.client.MinerCoreBER;
import com.jykito.industrialcore.client.miner.IndustrialMinerModel;
import com.jykito.industrialcore.miner.IndustrialMinerStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Formed view of structures with their own model, for the guide preview: in the world their blocks hide when formed and the
 * core BER draws instead of them. Here the same model is drawn from the schema's core block (front to the north, like the schema rows), without
 * a block entity; motion comes from the clock, not the world.
 */
final class GuideStructureModels {

    private GuideStructureModels() {}

    /** The core block the model is drawn from; null means the model is unknown. */
    static @Nullable String coreOf(String model) {
        return switch (model) {
            case "industrial_miner" -> "industrial_core:industrial_miner_core";
            case "miner" -> "industrial_core:miner_core";
            case "magnet" -> "industrial_core:electromagnet_core";
            default -> null;
        };
    }

    /** The pose is already set in the core block. */
    static void render(String model, PoseStack ps, MultiBufferSource buf) {
        float t = (System.currentTimeMillis() % 3_600_000L) / 50f;   // in ticks
        int light = LightTexture.FULL_BRIGHT, ov = OverlayTexture.NO_OVERLAY;
        switch (model) {
            case "industrial_miner" -> industrialMiner(ps, buf, t, light, ov);
            case "miner" -> MinerCoreBER.renderFormed(ps, buf, Direction.NORTH, (t * 0.5f) % 360f, light, ov);
            case "magnet" -> ElectromagnetCoreBER.renderModels(ps, buf, 0f, (t * 2f) % 360f, 1, ov);   // with a copper coil
            default -> { }
        }
    }

    /** Like IndustrialMinerBER with the front facing north. */
    private static void industrialMiner(PoseStack ps, MultiBufferSource buf, float t, int light, int ov) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(IndustrialMinerModel.TEX));
        ps.pushPose();
        ps.translate(0.5, -IndustrialMinerStructure.CORE_LAYER, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(180f));
        ps.translate(0, 0, -2);
        IndustrialMinerModel.render("static", ps, vc, light, ov, true);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(t * 0.12f));
        IndustrialMinerModel.render("drill", ps, vc, light, ov, true);
        ps.popPose();
        IndustrialMinerModel.render("ring", ps, vc, light, ov, true);
        ps.pushPose();
        ps.translate(0, 5.2, 0);
        ps.mulPose(Axis.YP.rotation(Mth.sin(t * 0.0175f) * 1.4f));
        IndustrialMinerModel.render("head", ps, vc, light, ov, true);
        ps.translate(0, 0.35, 0.15);
        ps.mulPose(Axis.XP.rotationDegrees(-32));
        IndustrialMinerModel.render("dish", ps, vc, light, ov, true);
        ps.popPose();
        ps.popPose();
    }
}
