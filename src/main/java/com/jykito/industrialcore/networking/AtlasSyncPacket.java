package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.nbt.CompoundTag;


/** Server -> client: the player's whole nanite species atlas (on join and on every species unlock). */
public class AtlasSyncPacket implements CustomPacketPayload {
    public static final Type<AtlasSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "atlas_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AtlasSyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(AtlasSyncPacket::toBytes, AtlasSyncPacket::new);

    @Override
    public Type<AtlasSyncPacket> type() { return TYPE; }

    private final CompoundTag atlas;

    public AtlasSyncPacket(CompoundTag atlas) { this.atlas = atlas; }
    public AtlasSyncPacket(RegistryFriendlyByteBuf buf) { this.atlas = buf.readNbt(); }
    public void toBytes(RegistryFriendlyByteBuf buf) { buf.writeNbt(atlas); }

    public void handle(IPayloadContext ctx) {
        // "new species" toasts are a client class, not loaded on the server
        ctx.enqueueWork(() -> com.jykito.industrialcore.client.NaniteAtlasToasts.onSync(atlas == null ? new CompoundTag() : atlas));
    }
}
