package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.item.custom.PlasmaArmorEventHandler;
import com.jykito.industrialcore.item.custom.PlasmaArmorItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PlasmaBurstPacket {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public PlasmaBurstPacket() {}
    public PlasmaBurstPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
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
        return true;
    }
}
