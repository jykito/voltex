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
 * Drill beacon as an item: the same model as {@link DrillBeaconBER}, only still (the crystal does not spin,
 * so the inventory does not flicker). The beacon with its crystal is ~1.85 blocks tall; the vertical centre is moved to the item centre,
 * the size in the slot, in hand and on the ground is set by display in models/item/drill_beacon.json.
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
