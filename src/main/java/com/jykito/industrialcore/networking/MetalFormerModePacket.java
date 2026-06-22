package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.menu.MetalFormerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MetalFormerModePacket {
    private final BlockPos pos;

    public MetalFormerModePacket(BlockPos pos) {
        this.pos = pos;
    }

    public MetalFormerModePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!(player.containerMenu instanceof MetalFormerMenu menu)) return;
            if (!menu.getBlockEntity().getBlockPos().equals(pos)) return;
            menu.getBlockEntity().cycleMode();
        });
        return true;
    }
}
