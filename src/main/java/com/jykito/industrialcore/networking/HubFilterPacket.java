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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;


public class HubFilterPacket implements CustomPacketPayload {
    public static final Type<HubFilterPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "hub_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HubFilterPacket> STREAM_CODEC =
            StreamCodec.ofMember(HubFilterPacket::toBytes, HubFilterPacket::new);

    @Override
    public Type<HubFilterPacket> type() { return TYPE; }

    private final BlockPos pos;
    private final int face, slot;
    private final boolean output;
    private final ItemStack stack;

    public HubFilterPacket(BlockPos pos, int face, int slot, ItemStack stack, boolean output) {
        this.pos = pos; this.face = face; this.slot = slot; this.stack = stack; this.output = output;
    }

    public HubFilterPacket(RegistryFriendlyByteBuf buf) {
        pos = buf.readBlockPos(); face = buf.readVarInt(); slot = buf.readVarInt();
        output = buf.readBoolean(); stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos); buf.writeVarInt(face); buf.writeVarInt(slot);
        buf.writeBoolean(output); ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
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
            ItemStackHandler h = be.filter(output);
            if (stack.isEmpty()) {
                h.setStackInSlot(idx, ItemStack.EMPTY);
                if (!output) be.filterCaps[idx] = 0; // limits only on input
            } else {
                ItemStack ghost = stack.copy(); ghost.setCount(1);
                h.setStackInSlot(idx, ghost);
            }
            be.setChanged(); be.sync();
        });
    }
}
