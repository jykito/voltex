package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.jykito.industrialcore.item.custom.IndustrialDrillItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;


public class DrillModeSwitchPacket implements CustomPacketPayload {
    public static final Type<DrillModeSwitchPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "drill_mode_switch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DrillModeSwitchPacket> STREAM_CODEC =
            StreamCodec.ofMember(DrillModeSwitchPacket::toBytes, DrillModeSwitchPacket::new);

    @Override
    public Type<DrillModeSwitchPacket> type() { return TYPE; }


    public DrillModeSwitchPacket() {}
    public DrillModeSwitchPacket(RegistryFriendlyByteBuf buf) {}
    public void toBytes(RegistryFriendlyByteBuf buf) {}

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof IndustrialDrillItem) {
                IndustrialDrillItem.cycleMode(held);
                player.inventoryMenu.broadcastChanges();
            }
        });
    }
}
