package com.jykito.industrialcore.client.miner;

import com.jykito.industrialcore.block.custom.IndustrialMinerCoreBlock;
import com.jykito.industrialcore.block.entity.IndustrialMinerCoreBlockEntity;
import com.jykito.industrialcore.miner.IndustrialMinerStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Собранный промышленный шахтёр целиком: модель от центра основания структуры, фасад к взгляду ядра.
 * Бур крутится, пока машина работает (угол копится, поэтому при остановке бур замирает, а не прыгает),
 * кольцо скважины пульсирует, антенна на мачте медленно водит из стороны в сторону.
 */
public class IndustrialMinerBER implements BlockEntityRenderer<IndustrialMinerCoreBlockEntity> {

    public IndustrialMinerBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(IndustrialMinerCoreBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState st = be.getBlockState();
        Level level = be.getLevel();
        if (level == null || !st.getValue(IndustrialMinerCoreBlock.FORMED)) return;
        Direction f = st.getValue(IndustrialMinerCoreBlock.FACING);
        boolean on = st.getValue(IndustrialMinerCoreBlock.LIT);
        float time = level.getGameTime() + pt;
        if (be.clientLastTime >= 0 && on) be.clientDrillAngle += (time - be.clientLastTime) * 0.12f;
        be.clientLastTime = time;

        int lt = LevelRenderer.getLightColor(level, be.getBlockPos().relative(f));   // само ядро внутри корпуса, свет — перед фасадом
        // два прохода (юзер: «убери свечение ядра, светиться должны только зелёные части»): вся модель при обычном свете,
        // затем светящиеся материалы ещё раз маской из одних зелёных пикселей. Маска идёт в буфер eyes, как глаза паука:
        // полная яркость в entityCutout при шейдерах не светилась и проигрывала обычному слою (юзер 10-10: «щас он тёмный»).
        // Буферы не перемешивать: взятый следом буфер другого типа закрывает предыдущий.
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(IndustrialMinerModel.TEX));
        draw(be, ps, f, on, time, g -> IndustrialMinerModel.render(g, ps, vc, lt, overlay, false));
        VertexConsumer glow = buf.getBuffer(RenderType.eyes(IndustrialMinerModel.TEX_GLOW));
        draw(be, ps, f, on, time, g -> IndustrialMinerModel.renderGlow(g, ps, glow, overlay));
    }

    /** Позы частей модели; {@code part} рисует группу в текущей позе. */
    private static void draw(IndustrialMinerCoreBlockEntity be, PoseStack ps, Direction f, boolean on, float time, java.util.function.Consumer<String> part) {
        ps.pushPose();
        ps.translate(0.5, -IndustrialMinerStructure.CORE_LAYER, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(switch (f) { case EAST -> 90f; case NORTH -> 180f; case WEST -> 270f; default -> 0f; }));
        ps.translate(0, 0, -2);   // ядро в первом ряду, центр основания на два блока вглубь

        part.accept("static");
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(be.clientDrillAngle));
        part.accept("drill");
        ps.popPose();
        ps.pushPose();
        float s = on ? 1 + 0.06f * Mth.sin(time * 0.3f) : 1;
        ps.scale(s, 1, s);
        part.accept("ring");
        ps.popPose();
        ps.pushPose();
        ps.translate(0, 5.2, 0);
        ps.mulPose(Axis.YP.rotation(Mth.sin(time * 0.0175f) * 1.4f));
        part.accept("head");
        ps.translate(0, 0.35, 0.15);
        ps.mulPose(Axis.XP.rotationDegrees(-32));
        part.accept("dish");
        ps.popPose();
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(IndustrialMinerCoreBlockEntity be) { return true; }

    @Override
    public int getViewDistance() { return 128; }
}
