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
 * 3D-иконка силовой брони: в инвентаре, JEI и гайде предмет рисуется самой деталью {@link PowerSuitModel}
 * (у предмета модель builtin/entity, повороты по местам показа — в её display). Деталь центрируется
 * и масштабируется под ячейку; руки и ноги слегка разведены, как на ванильных иконках брони.
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

    /** Центр детали в пикселях модели (y вниз, от шеи) и масштаб, при котором она в повороте gui [25,-35,0] влезает в 16 px. */
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
        ResourceLocation tex = new ResourceLocation(armor.getArmorTexture(stack, null, slot, null));
        Fit f = fit(slot);
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.scale(f.scale, f.scale, f.scale);
        ps.mulPose(Axis.XP.rotationDegrees(180));   // модель: y вниз, лицо на −z -> предмет: y вверх, лицо к зрителю
        ps.translate(-f.cx / 16, -f.cy / 16, -f.cz / 16);
        VertexConsumer vc = ItemRenderer.getArmorFoilBuffer(buf, RenderType.armorCutoutNoCull(tex), false, stack.hasFoil());
        model.renderToBuffer(ps, vc, light, overlay, 1f, 1f, 1f, 1f);
        ps.popPose();
    }
}
