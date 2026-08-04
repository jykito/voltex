package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.menu.ElectromagnetMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент → сервер: игрок двигает троттл раскрутки магнита (0..1). */
public class MagnetThrottlePacket {
    private final BlockPos pos;
    private final float throttle;

    public MagnetThrottlePacket(BlockPos pos, float throttle) {
        this.pos = pos;
        this.throttle = throttle;
    }
    public MagnetThrottlePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.throttle = buf.readFloat();
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(throttle);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            // безопасность: игрок должен держать открытым ЭТО же меню магнита
            if (!(player.containerMenu instanceof ElectromagnetMenu menu)) return;
            if (menu.blockEntity == null || !menu.blockEntity.getBlockPos().equals(pos)) return;
            menu.blockEntity.setThrottle(throttle);
        });
        return true;
    }
}
