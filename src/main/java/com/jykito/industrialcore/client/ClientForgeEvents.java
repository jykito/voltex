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

@SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

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
            if (srcPos != null) {
                float[] col = LogisticsBeamRenderer.COL_PREVIEW;
                float pulse = 0.6f + 0.4f * Mth.sin((mc.level.getGameTime() + event.getPartialTick()) * 0.15f);
                PoseStack ps = event.getPoseStack();
                Vec3 cam = event.getCamera().getPosition();
                MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
                ps.pushPose();
                ps.translate(-cam.x + srcPos.getX() + 0.5, -cam.y + srcPos.getY() + 0.5, -cam.z + srcPos.getZ() + 0.5);
                LogisticsBeamRenderer.drawOrb(ps, buf, 0, 0, 0, col[0] * pulse, col[1] * pulse, col[2] * pulse);
                ps.popPose();
                buf.endBatch(ModRenderTypes.ADDITIVE_GLOW.apply(LogisticsBeamRenderer.glowTex()));
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
