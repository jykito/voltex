package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.world.phys.Vec3;


/**
 * Server -> clients near the shooter: a gauss shot (from, to, stage, plasma, plasma blast, who fired).
 * The client draws the beam, the spiral and the flashes (GaussBeams) and gives the shooter camera recoil.
 */
public class GaussShotPacket implements CustomPacketPayload {
    public static final Type<GaussShotPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "gauss_shot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GaussShotPacket> STREAM_CODEC =
            StreamCodec.ofMember(GaussShotPacket::toBytes, GaussShotPacket::new);

    @Override
    public Type<GaussShotPacket> type() { return TYPE; }

    private final Vec3 from, to;
    private final int stage, shooter;
    private final boolean plasma, blast;

    public GaussShotPacket(Vec3 from, Vec3 to, int stage, boolean plasma, boolean blast, int shooter) {
        this.from = from; this.to = to; this.stage = stage; this.plasma = plasma; this.blast = blast; this.shooter = shooter;
    }

    public GaussShotPacket(RegistryFriendlyByteBuf buf) {
        from = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        to = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        stage = buf.readByte(); plasma = buf.readBoolean(); blast = buf.readBoolean(); shooter = buf.readVarInt();
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeDouble(from.x); buf.writeDouble(from.y); buf.writeDouble(from.z);
        buf.writeDouble(to.x); buf.writeDouble(to.y); buf.writeDouble(to.z);
        buf.writeByte(stage); buf.writeBoolean(plasma); buf.writeBoolean(blast); buf.writeVarInt(shooter);
    }

    public void handle(IPayloadContext ctx) {
        // drawing is a client class, not loaded on the server
        ctx.enqueueWork(() -> com.jykito.industrialcore.client.weapon.GaussBeams.add(from, to, stage, plasma, blast, shooter));
    }
}
