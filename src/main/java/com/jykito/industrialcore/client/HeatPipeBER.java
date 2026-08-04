package com.jykito.industrialcore.client;

import com.jykito.industrialcore.block.custom.HeatPipeBlock;
import com.jykito.industrialcore.block.entity.HeatPipeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public class HeatPipeBER implements BlockEntityRenderer<HeatPipeBlockEntity> {

    public static final ResourceLocation CORE_GLOW = new ResourceLocation("industrial_core", "block/heat_pipe_core_glow");
    public static final ResourceLocation ARM_GLOW  = new ResourceLocation("industrial_core", "block/full_ruka_glow");

    /** Дальше этого свечение не рисуется. Одно число и для отсечки, и для getViewDistance —
     *  иначе Minecraft зовёт render() на трубы, которые сами себя тут же отбрасывают. */
    private static final int GLOW_DISTANCE = 32;

    public HeatPipeBER(BlockEntityRendererProvider.Context ctx) {}

    private static final WeakHashMap<BakedModel, List<BakedQuad>> QUAD_CACHE = new WeakHashMap<>();
    private static List<BakedQuad> allQuads(BakedModel model) {
        return QUAD_CACHE.computeIfAbsent(model, m -> {
            RandomSource r = RandomSource.create();
            List<BakedQuad> list = new ArrayList<>(m.getQuads(null, null, r, ModelData.EMPTY, null));
            for (Direction d : Direction.values()) list.addAll(m.getQuads(null, d, r, ModelData.EMPTY, null));
            return list;
        });
    }

    @Override
    public void render(HeatPipeBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {
        float frac = Mth.clamp(be.getRenderHeat(partialTick) / (float) HeatPipeBlockEntity.MAX_HEAT, 0f, 1f);
        if (frac < 0.02f) return; // холодная — не светится

        // LOD: далёкие трубы не рисуют свечение (близко без изменений; тепло-механика серверная — не трогается)
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double gdx = be.getBlockPos().getX() + 0.5 - cam.x, gdy = be.getBlockPos().getY() + 0.5 - cam.y, gdz = be.getBlockPos().getZ() + 0.5 - cam.z;
        if (gdx * gdx + gdy * gdy + gdz * gdz > GLOW_DISTANCE * GLOW_DISTANCE) return;

        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof HeatPipeBlock)) return;

        BakedModel core = mc.getModelManager().getModel(CORE_GLOW);
        BakedModel arm  = mc.getModelManager().getModel(ARM_GLOW);
        BakedModel missing = mc.getModelManager().getMissingModel();
        if (core == missing && arm == missing) return;

        VertexConsumer vc = buf.getBuffer(ModRenderTypes.additiveEntityGlow(TextureAtlas.LOCATION_BLOCKS));
        // текстура выемки красная; яркость по нагреву (аддитивно: 0 = невидимо)
        float r = frac, g = frac * 0.55f, b = frac * 0.2f;

        if (core != missing && coreVisible(state)) {
            for (BakedQuad q : allQuads(core))
                vc.putBulkData(ps.last(), q, r, g, b, LightTexture.FULL_BRIGHT, overlay);
        }

        if (arm != missing) {
            for (Direction d : Direction.values()) {
                if (!connected(state, d)) continue;
                ps.pushPose();
                ps.translate(0.5, 0.5, 0.5);
                applyArmRotation(ps, d);
                ps.translate(-0.5, -0.5, -0.5);
                for (BakedQuad q : allQuads(arm))
                    vc.putBulkData(ps.last(), q, r, g, b, LightTexture.FULL_BRIGHT, overlay);
                ps.popPose();
            }
        }
    }

    private static boolean connected(BlockState s, Direction d) {
        return s.getValue(HeatPipeBlock.PROPERTY_BY_DIRECTION.get(d));
    }

    /** Ядро видно, если труба НЕ прямой проход (как в блокстейте). */
    private static boolean coreVisible(BlockState s) {
        boolean n = connected(s, Direction.NORTH), so = connected(s, Direction.SOUTH),
                e = connected(s, Direction.EAST),  w  = connected(s, Direction.WEST),
                u = connected(s, Direction.UP),    dn = connected(s, Direction.DOWN);
        boolean straight = (n && so && !e && !w && !u && !dn)
                || (e && w && !n && !so && !u && !dn)
                || (u && dn && !n && !so && !e && !w);
        return !straight;
    }

    /** База рукава смотрит на +Z (юг); совпадает с поворотами блокстейта. */
    private static void applyArmRotation(PoseStack ps, Direction d) {
        switch (d) {
            case SOUTH -> {}
            case WEST  -> ps.mulPose(Axis.YP.rotationDegrees(-90));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST  -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case UP    -> ps.mulPose(Axis.XP.rotationDegrees(-90));
            case DOWN  -> ps.mulPose(Axis.XP.rotationDegrees(90));
        }
    }

    @Override public int getViewDistance() { return GLOW_DISTANCE; }
}
