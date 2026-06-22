package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.item.custom.IndustrialDrillItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DrillModeSwitchPacket {

    public DrillModeSwitchPacket() {}
    public DrillModeSwitchPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof IndustrialDrillItem) {
                IndustrialDrillItem.cycleMode(held);
                player.inventoryMenu.broadcastChanges();
            }
        });
        return true;
    }
}
