package com.jykito.industrialcore.networking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModMessages {

    /** Mod bus. Handlers run on the main thread (the default in NeoForge 21). */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar net = event.registrar("1");

        net.playToServer(MetalFormerModePacket.TYPE, MetalFormerModePacket.STREAM_CODEC, MetalFormerModePacket::handle);
        net.playToServer(DrillModeSwitchPacket.TYPE, DrillModeSwitchPacket.STREAM_CODEC, DrillModeSwitchPacket::handle);
        net.playToServer(NodeReorderPacket.TYPE, NodeReorderPacket.STREAM_CODEC, NodeReorderPacket::handle);
        net.playToServer(HubFilterPacket.TYPE, HubFilterPacket.STREAM_CODEC, HubFilterPacket::handle);
        net.playToServer(HubFilterQtyPacket.TYPE, HubFilterQtyPacket.STREAM_CODEC, HubFilterQtyPacket::handle);
        net.playToServer(HubFluidFilterPacket.TYPE, HubFluidFilterPacket.STREAM_CODEC, HubFluidFilterPacket::handle);
        net.playToServer(PlasmaBurstPacket.TYPE, PlasmaBurstPacket.STREAM_CODEC, PlasmaBurstPacket::handle);
        net.playToServer(ExportRecipePacket.TYPE, ExportRecipePacket.STREAM_CODEC, ExportRecipePacket::handle);
        net.playToServer(MagnetThrottlePacket.TYPE, MagnetThrottlePacket.STREAM_CODEC, MagnetThrottlePacket::handle);
        net.playToServer(MePortPacket.TYPE, MePortPacket.STREAM_CODEC, MePortPacket::handle);

        net.playToClient(AtlasSyncPacket.TYPE, AtlasSyncPacket.STREAM_CODEC, AtlasSyncPacket::handle);
        net.playToClient(GaussShotPacket.TYPE, GaussShotPacket.STREAM_CODEC, GaussShotPacket::handle);
    }

    public static void sendToPlayer(CustomPacketPayload message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }

    /** To everyone who sees the entity, and to the entity itself (if it is a player). */
    public static void sendToTrackingAndSelf(CustomPacketPayload message, Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, message);
    }

    public static void sendToServer(CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }
}
