package com.jykito.industrialcore.client;

import com.jykito.industrialcore.block.entity.CultivatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CultivatorRenderer implements BlockEntityRenderer<CultivatorBlockEntity> {

    public CultivatorRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(CultivatorBlockEntity be, float partialTick,
                       PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        MachineItemRender.render(be.getDisplayResult(), be.getLevel(), partialTick, ps, buf, overlay, 0.5, 0.55, 0.5);
    }

    @Override public int getViewDistance() { return 32; }
    @Override public boolean shouldRenderOffScreen(CultivatorBlockEntity be) { return false; }
}
