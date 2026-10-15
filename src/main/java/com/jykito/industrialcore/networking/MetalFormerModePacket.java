package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.jykito.industrialcore.menu.MetalFormerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;


public class MetalFormerModePacket implements CustomPacketPayload {
    public static final Type<MetalFormerModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "metal_former_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MetalFormerModePacket> STREAM_CODEC =
            StreamCodec.ofMember(MetalFormerModePacket::toBytes, MetalFormerModePacket::new);

    @Override
    public Type<MetalFormerModePacket> type() { return TYPE; }

    private final BlockPos pos;

    public MetalFormerModePacket(BlockPos pos) {
        this.pos = pos;
    }

    public MetalFormerModePacket(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            if (!(player.containerMenu instanceof MetalFormerMenu menu)) return;
            if (!menu.getBlockEntity().getBlockPos().equals(pos)) return;
            menu.getBlockEntity().cycleMode();
        });
    }
}
