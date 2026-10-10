package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.networking.DrillModeSwitchPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(IndustrialCore.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(MetalFormerModePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(MetalFormerModePacket::new)
                .encoder(MetalFormerModePacket::toBytes)
                .consumerMainThread(MetalFormerModePacket::handle)
                .add();

        net.messageBuilder(DrillModeSwitchPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DrillModeSwitchPacket::new)
                .encoder(DrillModeSwitchPacket::toBytes)
                .consumerMainThread(DrillModeSwitchPacket::handle)
                .add();

        net.messageBuilder(NodeReorderPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(NodeReorderPacket::new)
                .encoder(NodeReorderPacket::toBytes)
                .consumerMainThread(NodeReorderPacket::handle)
                .add();

        net.messageBuilder(HubFilterPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(HubFilterPacket::new)
                .encoder(HubFilterPacket::toBytes)
                .consumerMainThread(HubFilterPacket::handle)
                .add();

        net.messageBuilder(HubFilterQtyPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(HubFilterQtyPacket::new)
                .encoder(HubFilterQtyPacket::toBytes)
                .consumerMainThread(HubFilterQtyPacket::handle)
                .add();

        net.messageBuilder(HubFluidFilterPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(HubFluidFilterPacket::new)
                .encoder(HubFluidFilterPacket::toBytes)
                .consumerMainThread(HubFluidFilterPacket::handle)
                .add();

        net.messageBuilder(PlasmaBurstPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PlasmaBurstPacket::new)
                .encoder(PlasmaBurstPacket::toBytes)
                .consumerMainThread(PlasmaBurstPacket::handle)
                .add();

        net.messageBuilder(ExportRecipePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ExportRecipePacket::new)
                .encoder(ExportRecipePacket::toBytes)
                .consumerMainThread(ExportRecipePacket::handle)
                .add();

        net.messageBuilder(AtlasSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(AtlasSyncPacket::new)
                .encoder(AtlasSyncPacket::toBytes)
                .consumerMainThread(AtlasSyncPacket::handle)
                .add();

        net.messageBuilder(MagnetThrottlePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(MagnetThrottlePacket::new)
                .encoder(MagnetThrottlePacket::toBytes)
                .consumerMainThread(MagnetThrottlePacket::handle)
                .add();

        net.messageBuilder(GaussShotPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(GaussShotPacket::new)
                .encoder(GaussShotPacket::toBytes)
                .consumerMainThread(GaussShotPacket::handle)
                .add();

        net.messageBuilder(MePortPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(MePortPacket::new)
                .encoder(MePortPacket::toBytes)
                .consumerMainThread(MePortPacket::handle)
                .add();

    }

    public static <MSG> void sendToPlayer(MSG message, net.minecraft.server.level.ServerPlayer player) {
        INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), message);
    }

    /** Всем, кто видит сущность, и ей самой (если это игрок). */
    public static <MSG> void sendToTrackingAndSelf(MSG message, net.minecraft.world.entity.Entity entity) {
        INSTANCE.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
