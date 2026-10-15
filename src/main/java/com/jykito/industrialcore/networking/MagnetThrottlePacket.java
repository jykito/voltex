package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.jykito.industrialcore.menu.ElectromagnetMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;


/** Client -> server: the player moves the magnet spin-up throttle (0..1). */
public class MagnetThrottlePacket implements CustomPacketPayload {
    public static final Type<MagnetThrottlePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "magnet_throttle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MagnetThrottlePacket> STREAM_CODEC =
            StreamCodec.ofMember(MagnetThrottlePacket::toBytes, MagnetThrottlePacket::new);

    @Override
    public Type<MagnetThrottlePacket> type() { return TYPE; }

    private final BlockPos pos;
    private final float throttle;

    public MagnetThrottlePacket(BlockPos pos, float throttle) {
        this.pos = pos;
        this.throttle = throttle;
    }
    public MagnetThrottlePacket(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.throttle = buf.readFloat();
    }
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(throttle);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            // safety: the player must have THIS same magnet menu open
            if (!(player.containerMenu instanceof ElectromagnetMenu menu)) return;
            if (menu.blockEntity == null || !menu.blockEntity.getBlockPos().equals(pos)) return;
            menu.blockEntity.setThrottle(throttle);
        });
    }
}
