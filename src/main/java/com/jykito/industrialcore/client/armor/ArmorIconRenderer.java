package com.jykito.industrialcore.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 3D icon of the power suit: in the inventory, JEI and the guide the item is drawn as the {@link PowerSuitModel} part itself
 * (the item model is builtin/entity, rotations per display context are in its display). The part is centred
 * and scaled to the slot; arms and legs are spread slightly, like vanilla armor icons.
 */
public final class ArmorIconRenderer extends BlockEntityWithoutLevelRenderer {

    private static ArmorIconRenderer instance;

    public static ArmorIconRenderer get() {
        if (instance == null) instance = new ArmorIconRenderer();
        return instance;
    }

    private ArmorIconRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** Centre of the part in model pixels (y down, from the neck) and the scale at which it fits into 16 px at gui rotation [25,-35,0]. */
    private record Fit(float cx, float cy, float cz, float scale) {}

    private static Fit fit(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> new Fit(0, -4.73f, -0.26f, 1.0f);
            case LEGS -> new Fit(0, 17.65f, -0.5f, 0.86f);
            case FEET -> new Fit(0, 22.8f, -0.5f, 1.07f);
            default -> new Fit(0, 5.85f, 0.25f, 0.667f);
        };
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(stack.getItem() instanceof ArmorItem armor)) return;
        EquipmentSlot slot = armor.getEquipmentSlot();
        PowerSuitModel model = PowerSuitModel.forSlot(slot);
        model.poseForIcon();
        ResourceLocation tex = armor.getArmorTexture(stack, null, slot, null, false);
        Fit f = fit(slot);
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.scale(f.scale, f.scale, f.scale);
        ps.mulPose(Axis.XP.rotationDegrees(180));   // model: y down, face at −z -> item: y up, face towards the viewer
        ps.translate(-f.cx / 16, -f.cy / 16, -f.cz / 16);
        VertexConsumer vc = ItemRenderer.getArmorFoilBuffer(buf, RenderType.armorCutoutNoCull(tex), stack.hasFoil());
        model.renderToBuffer(ps, vc, light, overlay, 0xFFFFFFFF);
        ps.popPose();
    }
}
