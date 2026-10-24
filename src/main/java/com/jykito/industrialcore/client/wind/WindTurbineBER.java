package com.jykito.industrialcore.client.wind;

import com.jykito.industrialcore.block.custom.wind.WindRotors;
import com.jykito.industrialcore.block.custom.wind.WindTurbine;
import com.jykito.industrialcore.block.entity.WindTurbineNacelleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Nacelle of the wind turbine over the static platform: it turns after the wind of the dimension (a function of the game
 * time, the same on every client), the rotor in it spins with the output the server sent, the anemometer cups spin faster
 * in stronger wind. Angles accumulate between frames, so a stopping rotor slows down instead of jumping.
 * <p>
 * The rotor is drawn by the item that is in the nacelle ({@link WindRotors}): scaled to its reach, with its texture and tint.
 * The on-screen speed stops growing above the basic cap and a longer rotor turns slower, whatever numbers a pack gives it.
 */
public class WindTurbineBER implements BlockEntityRenderer<WindTurbineNacelleBlockEntity> {

    /** Output above this does not spin the rotor faster on screen. */
    private static final int SPIN_CAP = WindTurbine.MAX_OUTPUT;

    public WindTurbineBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(WindTurbineNacelleBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        double time = level.getGameTime() + pt;
        int out = be.output();
        WindRotors.Spec rotor = WindRotors.get(be.rotor());
        if (be.clientLastTime >= 0) {
            float dt = (float) Math.min(5, time - be.clientLastTime);
            float scale = rotor == null ? 1 : rotor.radius() / WindRotors.BASE_RADIUS;
            // rad per tick: about one turn a second at the basic cap, slower for a longer rotor
            float target = out > 0 && rotor != null ? (0.8f + Mth.sqrt(Math.min(out, SPIN_CAP)) * 0.26f) / 20f / scale : 0;
            be.clientSpeed += (target - be.clientSpeed) * Math.min(1, dt * 0.05f);
            be.clientRotor += be.clientSpeed * dt;
            be.clientCups += dt * (0.05f + WindTurbine.wind(time) * 0.15f);
        }
        be.clientLastTime = time;
        renderParts(ps, buf, WindTurbine.windDirection(time), be.clientRotor, be.clientCups, rotor, light, overlay,
                out > 0 ? Math.min(1f, 0.35f + Math.min(out, SPIN_CAP) / 300f) : 0f);
    }

    /**
     * The turning nacelle, the rotor (none if null) and the anemometer over a nacelle block in the current pose, with the
     * status strips glowing at {@code glow} (0 = off). Shared with the guide preview.
     */
    public static void renderParts(PoseStack ps, MultiBufferSource buf, float yaw, float rotorAngle, float cups,
                                   @Nullable WindRotors.Spec rotor, int light, int overlay, float glow) {
        float scale = rotor == null ? 1 : rotor.radius() / WindRotors.BASE_RADIUS;
        // do not interleave buffers: taking a buffer of another type closes the previous one
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WindTurbineModel.TEX));
        draw(ps, yaw, rotorAngle, cups, scale, g -> { if (!g.equals("rotor")) WindTurbineModel.render(g, ps, vc, light, overlay); });
        if (rotor != null) {
            VertexConsumer rv = buf.getBuffer(RenderType.entityCutoutNoCull(rotor.texture() != null ? rotor.texture() : WindTurbineModel.TEX));
            draw(ps, yaw, rotorAngle, cups, scale, g -> { if (g.equals("rotor")) WindTurbineModel.render(g, ps, rv, light, overlay, rotor.tint()); });
        }
        if (glow > 0) {
            VertexConsumer gv = buf.getBuffer(RenderType.eyes(WindTurbineModel.TEX_GLOW));
            draw(ps, yaw, rotorAngle, cups, scale, g -> WindTurbineModel.renderGlow(g, ps, gv, overlay, glow));
        }
    }

    /** Poses of the parts; {@code part} draws a group in the current pose. */
    private static void draw(PoseStack ps, float yaw, float rotorAngle, float cups, float rotorScale, Consumer<String> part) {
        float[] y = WindTurbineModel.pivot("yaw"), r = WindTurbineModel.pivot("rotor"), c = WindTurbineModel.pivot("cups");
        ps.pushPose();
        ps.translate(0.5 + y[0], y[1], 0.5 + y[2]);
        ps.mulPose(Axis.YP.rotation(yaw));
        part.accept("body");
        ps.pushPose();
        ps.translate(r[0], r[1], r[2]);
        ps.mulPose(Axis.ZP.rotation(-rotorAngle));
        ps.scale(rotorScale, rotorScale, rotorScale);
        part.accept("rotor");
        ps.popPose();
        ps.pushPose();
        ps.translate(c[0], c[1], c[2]);
        ps.mulPose(Axis.YP.rotation(cups));
        part.accept("cups");
        ps.popPose();
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(WindTurbineNacelleBlockEntity be) { return true; }

    /** The rotor reaches out up to 6 blocks and the nacelle turns: without this the turbine vanishes at the screen edge. */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(WindTurbineNacelleBlockEntity be) {
        return new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(WindRotors.MAX_RADIUS + 1);
    }

    /** A tall landmark: seen from further than ordinary block entities. */
    @Override
    public int getViewDistance() { return 128; }
}
