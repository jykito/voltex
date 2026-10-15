package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.jykito.industrialcore.block.entity.LogisticsNodeBlockEntity;
import com.jykito.industrialcore.menu.LogisticsNodeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;


public class NodeReorderPacket implements CustomPacketPayload {
    public static final Type<NodeReorderPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "node_reorder"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NodeReorderPacket> STREAM_CODEC =
            StreamCodec.ofMember(NodeReorderPacket::toBytes, NodeReorderPacket::new);

    @Override
    public Type<NodeReorderPacket> type() { return TYPE; }

    private final BlockPos nodePos;
    private final int fromIndex;
    private final int toIndex;

    public NodeReorderPacket(BlockPos pos, int from, int to) {
        this.nodePos = pos; this.fromIndex = from; this.toIndex = to;
    }

    public NodeReorderPacket(RegistryFriendlyByteBuf buf) {
        nodePos    = buf.readBlockPos();
        fromIndex  = buf.readVarInt();
        toIndex    = buf.readVarInt();
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(nodePos);
        buf.writeVarInt(fromIndex);
        buf.writeVarInt(toIndex);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;

            if (!(player.containerMenu instanceof LogisticsNodeMenu menu) || !menu.nodePos.equals(nodePos)) return;
            if (!(player.level().getBlockEntity(nodePos) instanceof LogisticsNodeBlockEntity node)) return;
            int size = node.linkedPositions.size();
            if (fromIndex < 0 || fromIndex >= size || toIndex < 0 || toIndex >= size) return;
            BlockPos moved = node.linkedPositions.remove(fromIndex);
            node.linkedPositions.add(toIndex, moved);
            node.setChanged();
            node.sync();
        });
    }
}
