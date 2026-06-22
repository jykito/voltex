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

        net.messageBuilder(ConnectorFilterItemPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ConnectorFilterItemPacket::new)
                .encoder(ConnectorFilterItemPacket::toBytes)
                .consumerMainThread(ConnectorFilterItemPacket::handle)
                .add();

        net.messageBuilder(ConnectorFilterFluidPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ConnectorFilterFluidPacket::new)
                .encoder(ConnectorFilterFluidPacket::toBytes)
                .consumerMainThread(ConnectorFilterFluidPacket::handle)
                .add();

        net.messageBuilder(NodeReorderPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(NodeReorderPacket::new)
                .encoder(NodeReorderPacket::toBytes)
                .consumerMainThread(NodeReorderPacket::handle)
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
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
