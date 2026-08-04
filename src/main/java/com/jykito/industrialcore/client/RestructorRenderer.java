package com.jykito.industrialcore.client;

import com.jykito.industrialcore.block.entity.RestructorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RestructorRenderer implements BlockEntityRenderer<RestructorBlockEntity> {

    public RestructorRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RestructorBlockEntity be, float partialTick,
                       PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        net.minecraft.core.Direction f = be.getBlockState().getValue(com.jykito.industrialcore.block.custom.RestructorBlock.FACING);
        // ближе к лицевой грани (FACING смотрит на игрока при установке) + чуть ниже
        MachineItemRender.render(be.getDisplayResult(), be.getLevel(), partialTick, ps, buf, overlay,
                0.5 + f.getStepX() * 0.22, 0.42, 0.5 + f.getStepZ() * 0.22);
    }

    @Override public int getViewDistance() { return 32; } // кап дистанции: дальние не рисуют предмет
    @Override public boolean shouldRenderOffScreen(RestructorBlockEntity be) { return false; }
}
