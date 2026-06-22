package com.jykito.industrialcore.client;

import com.jykito.industrialcore.block.custom.logistics.FluidConnectorBlock;
import com.jykito.industrialcore.block.custom.logistics.ItemConnectorBlock;
import com.jykito.industrialcore.block.custom.logistics.LogisticsConnectorMode;
import com.jykito.industrialcore.block.entity.FluidConnectorBlockEntity;
import com.jykito.industrialcore.block.entity.ItemConnectorBlockEntity;
import com.jykito.industrialcore.block.entity.LogisticsNodeBlockEntity;
import com.jykito.industrialcore.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class LogisticsBeamRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    private static final ResourceLocation GLOW_TEX =
            new ResourceLocation("industrial_core", "textures/effect/energy_glow.png");

    public static final float[] COL_PREVIEW = {1.0f, 0.85f, 0.2f};

    private static final float[][] PALETTE = {
        {0.0f, 0.9f, 1.0f},
        {1.0f, 0.85f, 0.0f},
        {0.75f, 0.0f, 1.0f},
        {1.0f, 0.45f, 0.0f},
        {1.0f, 0.25f, 0.65f},
        {0.1f, 1.0f, 0.35f},
        {1.0f, 0.1f, 0.1f},
        {0.3f, 0.6f, 1.0f},
    };

    private static final int   ORBS         = 7;
    private static final float TRAVEL_TICKS = 48f;

    public static final List<ConnectorSnap> PENDING = new ArrayList<>();

    private static final Map<Long, Integer> CONNECTION_COLORS = new HashMap<>();
    private static final Random RNG = new Random();

    private static int colorFor(BlockPos a, BlockPos b) {
        long lo = Math.min(a.asLong(), b.asLong());
        long hi = Math.max(a.asLong(), b.asLong());
        long key = lo ^ (hi * 0x9E3779B97F4A7C15L);
        return CONNECTION_COLORS.computeIfAbsent(key, k -> RNG.nextInt(PALETTE.length));
    }

    public static final class ConnectorSnap {
        public final BlockPos origin;
        public final Set<BlockPos> links;
        public final byte state;
        public final Direction srcDir;
        public final int srcPriority;

        ConnectorSnap(BlockPos o, Set<BlockPos> l, byte s, Direction d, int p) {
            origin = o; links = l; state = s; srcDir = d; srcPriority = p;
        }
    }

    public LogisticsBeamRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(T be, float partialTick, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var linker = ModItems.LOGISTICS_LINKER.get();
        if (!player.getMainHandItem().is(linker) && !player.getOffhandItem().is(linker)) return;
        Collection<BlockPos> links = getLinks(be);
        if (links == null || links.isEmpty()) return;

        PENDING.add(new ConnectorSnap(
                be.getBlockPos(),
                new HashSet<>(links),
                getState(be),
                getFacing(be),
                flowPriority(be)
        ));
    }

    public static void renderPending(Level level, PoseStack ps,
                                     MultiBufferSource.BufferSource buf,
                                     Vec3 cam, float partialTick) {
        if (PENDING.isEmpty()) return;
        float time  = level.getGameTime() + partialTick;
        float pulse = 0.82f + 0.18f * Mth.sin(time * 0.07f);

        Set<Long> inPending = new HashSet<>();
        for (ConnectorSnap snap : PENDING) inPending.add(snap.origin.asLong());

        for (ConnectorSnap snap : PENDING) {
            double ox = snap.origin.getX() - cam.x;
            double oy = snap.origin.getY() - cam.y;
            double oz = snap.origin.getZ() - cam.z;

            ps.pushPose();
            ps.translate(ox, oy, oz);

            for (BlockPos target : snap.links) {
                BlockEntity tgtBe = level.getBlockEntity(target);
                if (tgtBe == null) continue;
                int tgtPriority = flowPriority(tgtBe);

                BlockPos fromPos, toPos;
                Direction fromDir, toDir;

                if (snap.srcPriority < tgtPriority) {

                    fromPos = snap.origin; fromDir = snap.srcDir;
                    toPos   = target;      toDir   = getFacingFromLevel(level, target);
                } else if (snap.srcPriority > tgtPriority) {

                    if (inPending.contains(target.asLong())) continue;
                    fromPos = target;       fromDir = getFacingFromLevel(level, target);
                    toPos   = snap.origin;  toDir   = snap.srcDir;
                } else {

                    if (inPending.contains(target.asLong()) && snap.origin.asLong() >= target.asLong()) continue;
                    fromPos = snap.origin; fromDir = snap.srcDir;
                    toPos   = target;      toDir   = getFacingFromLevel(level, target);
                }

                float[] fromOut = dirStep(fromDir != null ? fromDir.getOpposite() : null);
                float[] toOut   = dirStep(toDir   != null ? toDir.getOpposite()   : null);

                float fdx = fromPos.getX() - snap.origin.getX();
                float fdy = fromPos.getY() - snap.origin.getY();
                float fdz = fromPos.getZ() - snap.origin.getZ();

                float[] p0 = { fdx + 0.5f - fromOut[0] * 0.3125f,
                               fdy + 0.5f - fromOut[1] * 0.3125f,
                               fdz + 0.5f - fromOut[2] * 0.3125f };

                float tdx = toPos.getX() - snap.origin.getX();
                float tdy = toPos.getY() - snap.origin.getY();
                float tdz = toPos.getZ() - snap.origin.getZ();
                float[] p3 = { tdx + 0.5f - toOut[0] * 0.3125f,
                               tdy + 0.5f - toOut[1] * 0.3125f,
                               tdz + 0.5f - toOut[2] * 0.3125f };

                float dist = Mth.sqrt((p3[0]-p0[0])*(p3[0]-p0[0])
                                    + (p3[1]-p0[1])*(p3[1]-p0[1])
                                    + (p3[2]-p0[2])*(p3[2]-p0[2]));
                float arm = Math.min(dist * 0.15f, 0.8f);
                float[] p1 = { p0[0] + fromOut[0] * arm, p0[1] + fromOut[1] * arm, p0[2] + fromOut[2] * arm };
                float[] p2 = { p3[0] + toOut[0]   * arm, p3[1] + toOut[1]   * arm, p3[2] + toOut[2]   * arm };

                float[] col = PALETTE[colorFor(fromPos, toPos)];

                for (int i = 0; i < ORBS; i++) {
                    float t = (time / TRAVEL_TICKS + i / (float) ORBS) % 1.0f;
                    float fade = Mth.clamp(t * 120f, 0f, 1f) * Mth.clamp((1f - t) * 120f, 0f, 1f);
                    float[] pos = cubicBezier(p0, p1, p2, p3, t);

                    BlockPos orbBlock = BlockPos.containing(
                            snap.origin.getX() + pos[0],
                            snap.origin.getY() + pos[1],
                            snap.origin.getZ() + pos[2]);
                    if (level.hasChunkAt(orbBlock) && level.getBlockState(orbBlock).isSolidRender(level, orbBlock)) continue;
                    drawOrb(ps, buf, pos[0], pos[1], pos[2],
                            col[0] * pulse * fade, col[1] * pulse * fade, col[2] * pulse * fade);
                }
            }

            ps.popPose();
        }

        buf.endBatch(ModRenderTypes.ADDITIVE_GLOW.apply(GLOW_TEX));
        PENDING.clear();
    }

    public static void drawOrb(PoseStack ps, MultiBufferSource buf,
                                float cx, float cy, float cz,
                                float r, float g, float b) {
        VertexConsumer vc = buf.getBuffer(ModRenderTypes.ADDITIVE_GLOW.apply(GLOW_TEX));
        crossedQuads(vc, ps.last().pose(), cx, cy, cz, 0.10f,  r*0.25f, g*0.25f, b*0.25f, 0.45f);
        crossedQuads(vc, ps.last().pose(), cx, cy, cz, 0.055f, r, g, b, 0.88f);
        crossedQuads(vc, ps.last().pose(), cx, cy, cz, 0.055f, r, g, b, 0.88f);
        crossedQuads(vc, ps.last().pose(), cx, cy, cz, 0.025f, 1f, 1f, 1f, 0.95f);
        crossedQuads(vc, ps.last().pose(), cx, cy, cz, 0.025f, 1f, 1f, 1f, 0.95f);
    }

    private static void crossedQuads(VertexConsumer vc, Matrix4f m,
                                      float cx, float cy, float cz, float w,
                                      float r, float g, float b, float a) {
        vc.vertex(m, cx-w, cy-w, cz).color(r,g,b,a).uv(0,0).endVertex();
        vc.vertex(m, cx+w, cy-w, cz).color(r,g,b,a).uv(1,0).endVertex();
        vc.vertex(m, cx+w, cy+w, cz).color(r,g,b,a).uv(1,1).endVertex();
        vc.vertex(m, cx-w, cy+w, cz).color(r,g,b,a).uv(0,1).endVertex();

        vc.vertex(m, cx, cy-w, cz-w).color(r,g,b,a).uv(0,0).endVertex();
        vc.vertex(m, cx, cy-w, cz+w).color(r,g,b,a).uv(1,0).endVertex();
        vc.vertex(m, cx, cy+w, cz+w).color(r,g,b,a).uv(1,1).endVertex();
        vc.vertex(m, cx, cy+w, cz-w).color(r,g,b,a).uv(0,1).endVertex();

        vc.vertex(m, cx-w, cy, cz-w).color(r,g,b,a).uv(0,0).endVertex();
        vc.vertex(m, cx+w, cy, cz-w).color(r,g,b,a).uv(1,0).endVertex();
        vc.vertex(m, cx+w, cy, cz+w).color(r,g,b,a).uv(1,1).endVertex();
        vc.vertex(m, cx-w, cy, cz+w).color(r,g,b,a).uv(0,1).endVertex();
    }

    private static float[] dirStep(Direction d) {
        if (d == null) return new float[]{0, 0, 0};
        return new float[]{d.getStepX(), d.getStepY(), d.getStepZ()};
    }

    private static float[] cubicBezier(float[] p0, float[] p1, float[] p2, float[] p3, float t) {
        float u  = 1 - t;
        float uu = u * u;
        float tt = t * t;
        float b0 = u * uu, b1 = 3 * uu * t, b2 = 3 * u * tt, b3 = tt * t;
        return new float[]{
            b0*p0[0] + b1*p1[0] + b2*p2[0] + b3*p3[0],
            b0*p0[1] + b1*p1[1] + b2*p2[1] + b3*p3[1],
            b0*p0[2] + b1*p1[2] + b2*p2[2] + b3*p3[2]
        };
    }

    public static float[] faceCenter(float bx, float by, float bz, Direction facing, float offset) {
        float cx = bx+0.5f, cy = by+0.5f, cz = bz+0.5f;
        if (facing != null) { cx += facing.getStepX()*offset; cy += facing.getStepY()*offset; cz += facing.getStepZ()*offset; }
        return new float[]{cx, cy, cz};
    }

    public static float[] outward(Direction facing) {
        if (facing == null) return new float[]{0, 0, 0};
        return new float[]{-facing.getStepX(), -facing.getStepY(), -facing.getStepZ()};
    }

    public static Direction getFacing(BlockEntity be) {
        BlockState s = be.getBlockState();
        if (s.hasProperty(ItemConnectorBlock.FACING))  return s.getValue(ItemConnectorBlock.FACING);
        if (s.hasProperty(FluidConnectorBlock.FACING)) return s.getValue(FluidConnectorBlock.FACING);
        return null;
    }

    public static Direction getFacingFromLevel(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.hasProperty(ItemConnectorBlock.FACING))  return s.getValue(ItemConnectorBlock.FACING);
        if (s.hasProperty(FluidConnectorBlock.FACING)) return s.getValue(FluidConnectorBlock.FACING);
        return null;
    }

    public static Collection<BlockPos> getLinks(BlockEntity be) {
        if (be instanceof ItemConnectorBlockEntity c) return c.linkedPositions;
        if (be instanceof FluidConnectorBlockEntity c) return c.linkedPositions;
        if (be instanceof LogisticsNodeBlockEntity n)  return n.linkedPositions;
        return null;
    }

    public static byte getState(BlockEntity be) {
        if (be instanceof ItemConnectorBlockEntity c) return c.transferState;
        if (be instanceof FluidConnectorBlockEntity c) return c.transferState;
        return 0;
    }

    private static int flowPriority(BlockEntity be) {
        if (be instanceof ItemConnectorBlockEntity c)
            return c.mode == LogisticsConnectorMode.PUSH ? 0 : 2;
        if (be instanceof FluidConnectorBlockEntity c)
            return c.mode == LogisticsConnectorMode.PUSH ? 0 : 2;
        return 1;
    }

    public static ResourceLocation glowTex() { return GLOW_TEX; }

    @Override public boolean shouldRenderOffScreen(T be) { return true; }
    @Override public int getViewDistance() { return 64; }
}
