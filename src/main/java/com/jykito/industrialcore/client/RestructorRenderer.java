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
                       PoseStack ps, MultiBufferSource buf, int light, int overlay) {}

    @Override public boolean shouldRenderOffScreen(RestructorBlockEntity be) { return false; }
}
