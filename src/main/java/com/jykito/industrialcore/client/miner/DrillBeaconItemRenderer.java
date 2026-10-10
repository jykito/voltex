package com.jykito.industrialcore.client.miner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Буровой маяк как предмет: та же модель, что у {@link DrillBeaconBER}, только неподвижная (кристалл не крутится,
 * чтобы инвентарь не мельтешил). Маяк с кристаллом ~1.85 блока в высоту — центр по высоте сдвинут в центр предмета,
 * размер в слоте, руке и на земле задаёт display в models/item/drill_beacon.json.
 */
public final class DrillBeaconItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final float HALF_HEIGHT = 0.925f;
    private static DrillBeaconItemRenderer instance;

    public static DrillBeaconItemRenderer get() {
        if (instance == null) instance = new DrillBeaconItemRenderer();
        return instance;
    }

    private DrillBeaconItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(IndustrialMinerModel.TEX));
        ps.pushPose();
        ps.translate(0.5, 0.5 - HALF_HEIGHT, 0.5);
        IndustrialMinerModel.render("beacon", ps, vc, light, overlay, true);
        ps.translate(0, 1.72, 0);
        ps.mulPose(Axis.YP.rotation(0.8f));
        ps.mulPose(Axis.XP.rotationDegrees(35));
        IndustrialMinerModel.render("crystal", ps, vc, light, overlay, true);
        ps.popPose();
    }
}
