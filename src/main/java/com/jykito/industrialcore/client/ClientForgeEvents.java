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
     * Под шлемом Nexite или Plasma голова и второй слой скина не рисуются: шлем скруглённый и меньше куба головы,
     * иначе сквозь срезанные углы торчал бы скин. Видимость сама вернётся на следующем кадре
     * (PlayerRenderer заново включает части модели перед этим событием).
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
     * Рука от первого лица в нагруднике Nexite или Plasma. Ваниль броню на руке не рисует вообще, поэтому рисуем сами:
     * сначала обычную руку (она же выставляет позу под широкую или тонкую модель скина), потом поверх неё кость руки
     * силовой модели в той же позе. Ванильный вызов отменяется, чтобы рука не рисовалась дважды.
     * ⚠ Событие шлёт сам PlayerRenderer.renderRightHand/renderLeftHand, поэтому наш вызов руки снова попадает сюда:
     * без флага это бесконечная рекурсия и краш (переполнение стека, в отчёте маскируется под LinkageError log4j).
     */
    private static boolean renderingArm;

    @SubscribeEvent
    public static void onRenderArm(net.minecraftforge.client.event.RenderArmEvent event) {
        if (renderingArm) return;   // вложенный вызов из renderRightHand: обычная рука
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
        com.jykito.industrialcore.client.drill.DrillSound.clientTick(mc);   // звук бура, пока копаешь

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
