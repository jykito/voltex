package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.block.entity.HubBlockEntity;
import com.jykito.industrialcore.menu.HubMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HubFilterQtyPacket {
    private final BlockPos pos;
    private final int face, slot, qty;

    public HubFilterQtyPacket(BlockPos pos, int face, int slot, int qty) {
        this.pos = pos; this.face = face; this.slot = slot; this.qty = qty;
    }

    public HubFilterQtyPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos(); face = buf.readVarInt(); slot = buf.readVarInt(); qty = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos); buf.writeVarInt(face); buf.writeVarInt(slot); buf.writeVarInt(qty);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof HubMenu menu) || menu.be == null) return;
            HubBlockEntity be = menu.be;
            if (!be.getBlockPos().equals(pos)) return;
            if (face < 0 || face >= 6 || slot < 0 || slot >= HubBlockEntity.FILTER_SLOTS) return;
            int idx = face * HubBlockEntity.FILTER_SLOTS + slot;
            be.filterCaps[idx] = Math.max(0, Math.min(qty, 99999));
            be.setChanged(); be.sync();
        });
        return true;
    }
}
