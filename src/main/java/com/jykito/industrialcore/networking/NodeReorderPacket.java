package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.block.entity.LogisticsNodeBlockEntity;
import com.jykito.industrialcore.menu.LogisticsNodeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NodeReorderPacket {
    private final BlockPos nodePos;
    private final int fromIndex;
    private final int toIndex;

    public NodeReorderPacket(BlockPos pos, int from, int to) {
        this.nodePos = pos; this.fromIndex = from; this.toIndex = to;
    }

    public NodeReorderPacket(FriendlyByteBuf buf) {
        nodePos    = buf.readBlockPos();
        fromIndex  = buf.readVarInt();
        toIndex    = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(nodePos);
        buf.writeVarInt(fromIndex);
        buf.writeVarInt(toIndex);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
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
        return true;
    }
}
