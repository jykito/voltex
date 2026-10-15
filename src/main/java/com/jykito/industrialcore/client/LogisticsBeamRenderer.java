package com.jykito.industrialcore.client;

import com.jykito.industrialcore.block.entity.HubBlockEntity;
import com.jykito.industrialcore.block.entity.LogisticsNodeBlockEntity;
import com.jykito.industrialcore.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Shows logistics links as simple straight lines (when the Linker is in hand). */
@OnlyIn(Dist.CLIENT)
public class LogisticsBeamRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    public static final float[] COL_PREVIEW = {1.0f, 0.85f, 0.2f};

    // palette for colouring the lines, so identical blocks are not confused
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
        ConnectorSnap(BlockPos o, Set<BlockPos> l) { origin = o; links = l; }
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
        PENDING.add(new ConnectorSnap(be.getBlockPos(), new HashSet<>(links)));
    }

    public static void renderPending(Level level, PoseStack ps,
                                     MultiBufferSource.BufferSource buf,
                                     Vec3 cam, float partialTick) {
        if (PENDING.isEmpty()) return;

        Set<Long> inPending = new HashSet<>();
        for (ConnectorSnap snap : PENDING) inPending.add(snap.origin.asLong());

        VertexConsumer vc = buf.getBuffer(RenderType.lines());

        for (ConnectorSnap snap : PENDING) {
            ps.pushPose();
            ps.translate(snap.origin.getX() - cam.x, snap.origin.getY() - cam.y, snap.origin.getZ() - cam.z);
            Matrix4f pose = ps.last().pose();
            PoseStack.Pose norm = ps.last();

            for (BlockPos target : snap.links) {
                // each pair is drawn once
                if (inPending.contains(target.asLong()) && snap.origin.asLong() >= target.asLong()) continue;
                if (!level.isLoaded(target) || level.getBlockEntity(target) == null) continue;

                float[] col = PALETTE[colorFor(snap.origin, target)];
                line(vc, pose, norm,
                        0.5f, 0.5f, 0.5f,
                        (target.getX() - snap.origin.getX()) + 0.5f,
                        (target.getY() - snap.origin.getY()) + 0.5f,
                        (target.getZ() - snap.origin.getZ()) + 0.5f,
                        col[0], col[1], col[2]);
            }
            ps.popPose();
        }

        buf.endBatch(RenderType.lines());
        PENDING.clear();
    }

    /** Preview line from the selected source to the block under the crosshair. */
    public static void renderPreviewLine(PoseStack ps, MultiBufferSource.BufferSource buf,
                                         Vec3 cam, BlockPos from, BlockPos to, float[] col) {
        VertexConsumer vc = buf.getBuffer(RenderType.lines());
        ps.pushPose();
        ps.translate(from.getX() - cam.x, from.getY() - cam.y, from.getZ() - cam.z);
        line(vc, ps.last().pose(), ps.last(),
                0.5f, 0.5f, 0.5f,
                (to.getX() - from.getX()) + 0.5f,
                (to.getY() - from.getY()) + 0.5f,
                (to.getZ() - from.getZ()) + 0.5f,
                col[0], col[1], col[2]);
        ps.popPose();
        buf.endBatch(RenderType.lines());
    }

    private static void line(VertexConsumer vc, Matrix4f pose, PoseStack.Pose norm,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float r, float g, float b) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4f) return;
        dx /= len; dy /= len; dz /= len;
        vc.addVertex(pose, ax, ay, az).setColor(r, g, b, 1.0f).setNormal(norm, dx, dy, dz);
        vc.addVertex(pose, bx, by, bz).setColor(r, g, b, 1.0f).setNormal(norm, dx, dy, dz);
    }

    public static Collection<BlockPos> getLinks(BlockEntity be) {
        if (be instanceof HubBlockEntity h)           return h.linkedPositions;
        if (be instanceof LogisticsNodeBlockEntity n) return n.linkedPositions;
        return null;
    }

    @Override public boolean shouldRenderOffScreen(T be) { return true; }
    @Override public int getViewDistance() { return 64; }
}
