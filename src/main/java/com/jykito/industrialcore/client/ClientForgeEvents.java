package com.jykito.industrialcore.client;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.item.custom.IndustrialDrillItem;
import com.jykito.industrialcore.item.custom.LogisticsLinkerItem;
import com.jykito.industrialcore.item.custom.PlasmaArmorItem;
import com.jykito.industrialcore.networking.DrillModeSwitchPacket;
import com.jykito.industrialcore.networking.ModMessages;
import com.jykito.industrialcore.networking.PlasmaBurstPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IndustrialCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {

    /**
     * Under a Nexite or Plasma helmet the head and the second skin layer are not drawn: the helmet is rounded and smaller than the head cube,
     * otherwise the skin would poke through the cut corners. Visibility comes back by itself on the next frame
     * (PlayerRenderer re-enables the model parts before this event).
     */
    @SubscribeEvent
    public static void onRenderPlayerPre(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
        var helmet = event.getEntity().getItemBySlot(EquipmentSlot.HEAD).getItem();
        if (helmet instanceof com.jykito.industrialcore.item.custom.NexiteArmorItem || helmet instanceof PlasmaArmorItem) {
            event.getRenderer().getModel().head.visible = false;
            event.getRenderer().getModel().hat.visible = false;
        }
    }

    /**
     * First-person arm in a Nexite or Plasma chestplate. Vanilla does not draw armor on the arm at all, so we draw it ourselves:
     * first the normal arm (it also sets the pose for the wide or slim skin model), then on top of it the arm bone
     * of the power suit model in the same pose. The vanilla call is cancelled so the arm is not drawn twice.
     * ⚠ The event is fired by PlayerRenderer.renderRightHand/renderLeftHand itself, so our arm call lands here again:
     * without the flag that is infinite recursion and a crash (stack overflow, which the report disguises as a log4j LinkageError).
     */
    private static boolean renderingArm;

    @SubscribeEvent
    public static void onRenderArm(net.minecraftforge.client.event.RenderArmEvent event) {
        if (renderingArm) return;   // nested call from renderRightHand: the normal arm
        var player = event.getPlayer();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof com.jykito.industrialcore.item.custom.NexiteArmorItem) && !(chest.getItem() instanceof PlasmaArmorItem)) return;
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer)) return;
        PoseStack ps = event.getPoseStack();
        MultiBufferSource buf = event.getMultiBufferSource();
        int light = event.getPackedLight();
        boolean right = event.getArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        renderingArm = true;
        try {
            if (right) renderer.renderRightHand(ps, buf, light, player);
            else renderer.renderLeftHand(ps, buf, light, player);
        } finally {
            renderingArm = false;
        }
        var tex = new net.minecraft.resources.ResourceLocation(chest.getItem().getArmorTexture(chest, player, EquipmentSlot.CHEST, null));
        var vc = net.minecraft.client.renderer.entity.ItemRenderer.getArmorFoilBuffer(buf, net.minecraft.client.renderer.RenderType.armorCutoutNoCull(tex), false, chest.hasFoil());
        com.jykito.industrialcore.client.armor.PowerSuitModel.forSlot(EquipmentSlot.CHEST)
                .renderArm(ps, vc, light, right ? renderer.getModel().rightArm : renderer.getModel().leftArm, right);
        event.setCanceled(true);
    }

@SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        com.jykito.industrialcore.client.drill.DrillSound.clientTick(mc);   // drill sound while digging

        if (mc.screen == null) {
            while (ModKeyBindings.DRILL_MODE.consumeClick()) {
                if (mc.player.getMainHandItem().getItem() instanceof IndustrialDrillItem) {
                    ModMessages.sendToServer(new DrillModeSwitchPacket());
                }
            }
            while (ModKeyBindings.PLASMA_BURST.consumeClick()) {
                if (isPlasmaReady(mc.player)) {
                    ModMessages.sendToServer(new PlasmaBurstPacket());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                LogisticsBeamRenderer.renderPending(
                        mc.level,
                        event.getPoseStack(),
                        mc.renderBuffers().bufferSource(),
                        event.getCamera().getPosition(),
                        event.getPartialTick());
            }
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof LogisticsLinkerItem && LogisticsLinkerItem.hasSource(held)) {
            BlockPos srcPos = LogisticsLinkerItem.getSource(held);
            if (srcPos != null && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr
                    && bhr.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                LogisticsBeamRenderer.renderPreviewLine(
                        event.getPoseStack(),
                        mc.renderBuffers().bufferSource(),
                        event.getCamera().getPosition(),
                        srcPos, bhr.getBlockPos(),
                        LogisticsBeamRenderer.COL_PREVIEW);
            }
        }

    }

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static boolean isPlasmaReady(Player player) {
        int ready = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack s = player.getItemBySlot(slot);
            if (s.getItem() instanceof PlasmaArmorItem &&
                PlasmaArmorItem.getBurstCharge(s) >= PlasmaArmorItem.BURST_MAX) {
                ready++;
            }
        }
        return ready == 4;
    }

    private ClientForgeEvents() {}
}
