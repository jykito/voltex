package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.jykito.industrialcore.item.custom.PlasmaArmorEventHandler;
import com.jykito.industrialcore.item.custom.PlasmaArmorItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PlasmaBurstPacket implements CustomPacketPayload {
    public static final Type<PlasmaBurstPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "plasma_burst"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlasmaBurstPacket> STREAM_CODEC =
            StreamCodec.ofMember(PlasmaBurstPacket::toBytes, PlasmaBurstPacket::new);

    @Override
    public Type<PlasmaBurstPacket> type() { return TYPE; }


    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public PlasmaBurstPacket() {}
    public PlasmaBurstPacket(RegistryFriendlyByteBuf buf) {}
    public void toBytes(RegistryFriendlyByteBuf buf) {}

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;

            List<ItemStack> pieces = new ArrayList<>();
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.getItem() instanceof PlasmaArmorItem) pieces.add(stack);
            }

            if (pieces.size() == 4 && PlasmaArmorEventHandler.isBurstReady(pieces)) {
                PlasmaArmorEventHandler.triggerPlasmaBurst(player, pieces);
            }
        });
    }
}
