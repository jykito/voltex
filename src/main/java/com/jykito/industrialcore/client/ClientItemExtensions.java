package com.jykito.industrialcore.client;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.client.armor.ArmorIconRenderer;
import com.jykito.industrialcore.client.armor.PowerSuitModel;
import com.jykito.industrialcore.client.drill.DrillRenderer;
import com.jykito.industrialcore.client.miner.DrillBeaconItemRenderer;
import com.jykito.industrialcore.client.weapon.BladeRenderer;
import com.jykito.industrialcore.client.weapon.GaussRenderer;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.custom.DrillBeaconItem;
import com.jykito.industrialcore.item.custom.EnergyDrillItem;
import com.jykito.industrialcore.item.custom.NexiteArmorItem;
import com.jykito.industrialcore.item.custom.PlasmaArmorItem;
import com.jykito.industrialcore.item.custom.weapon.EnergyBladeItem;
import com.jykito.industrialcore.item.custom.weapon.GaussRifleItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client behaviour of items: volumetric renderers in hand and the power suit model. */
@EventBusSubscriber(modid = IndustrialCore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientItemExtensions {
    private ClientItemExtensions() {}

    /** Drill beacon: drawn by its own item renderer. */
    private static final IClientItemExtensions DRILL_BEACON = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() { return DrillBeaconItemRenderer.get(); }
    };

    /** In hand, on the ground and in item frames a volumetric drill (DrillRenderer); in the inventory the item model gives a flat icon. */
    private static final IClientItemExtensions DRILL = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() { return DrillRenderer.get(); }
    };

    /** Nexite and Plasma: our own armor model instead of the vanilla layout, and a 3D icon drawn as the armor part itself. */
    private static final IClientItemExtensions POWER_SUIT = new IClientItemExtensions() {
        @Override
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
            return PowerSuitModel.forSlot(slot);
        }

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() { return ArmorIconRenderer.get(); }
    };

    /** In hand, on the ground and in item frames a volumetric sabre (BladeRenderer); in the inventory the item model gives a flat icon. */
    private static final IClientItemExtensions BLADE = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() { return BladeRenderer.get(); }
    };

    /** In hand, on the ground and in item frames a volumetric "Fork" (GaussRenderer); in the inventory the item model gives a flat icon. */
    private static final IClientItemExtensions GAUSS = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() { return GaussRenderer.get(); }

        /**
         * In third person the rifle is held forward with both hands, like a loaded crossbow (author, 2026-10-10: "it charges pointing up,
         * it should point forward"). The grip in the model (thirdperson [0,-90,135]) is made for a raised arm: that way the barrel goes
         * forward along the arm both while charging (bow pose from UseAnim.BOW) and at rest. With a lowered arm the barrel would point at the ground.
         */
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
    };

    @SubscribeEvent
    public static void onRegister(RegisterClientExtensionsEvent event) {
        for (var holder : ModItems.ITEMS.getEntries()) {
            Item item = holder.get();
            IClientItemExtensions ext =
                    item instanceof DrillBeaconItem ? DRILL_BEACON
                    : item instanceof EnergyDrillItem ? DRILL
                    : item instanceof NexiteArmorItem || item instanceof PlasmaArmorItem ? POWER_SUIT
                    : item instanceof EnergyBladeItem ? BLADE
                    : item instanceof GaussRifleItem ? GAUSS
                    : null;
            if (ext != null) event.registerItem(ext, item);
        }
    }
}
