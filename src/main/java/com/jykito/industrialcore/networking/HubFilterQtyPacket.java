package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.jykito.industrialcore.block.entity.HubBlockEntity;
import com.jykito.industrialcore.menu.HubMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;


public class HubFilterQtyPacket implements CustomPacketPayload {
    public static final Type<HubFilterQtyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "hub_filter_qty"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HubFilterQtyPacket> STREAM_CODEC =
            StreamCodec.ofMember(HubFilterQtyPacket::toBytes, HubFilterQtyPacket::new);

    @Override
    public Type<HubFilterQtyPacket> type() { return TYPE; }

    private final BlockPos pos;
    private final int face, slot, qty;

    public HubFilterQtyPacket(BlockPos pos, int face, int slot, int qty) {
        this.pos = pos; this.face = face; this.slot = slot; this.qty = qty;
    }

    public HubFilterQtyPacket(RegistryFriendlyByteBuf buf) {
        pos = buf.readBlockPos(); face = buf.readVarInt(); slot = buf.readVarInt(); qty = buf.readVarInt();
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos); buf.writeVarInt(face); buf.writeVarInt(slot); buf.writeVarInt(qty);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            if (!(player.containerMenu instanceof HubMenu menu) || menu.be == null) return;
            HubBlockEntity be = menu.be;
            if (!be.getBlockPos().equals(pos)) return;
            if (face < 0 || face >= 6 || slot < 0 || slot >= HubBlockEntity.FILTER_SLOTS) return;
            int idx = face * HubBlockEntity.FILTER_SLOTS + slot;
            be.filterCaps[idx] = Math.max(0, Math.min(qty, 99999));
            be.setChanged(); be.sync();
        });
    }
}
