package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.menu.ItemConnectorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConnectorFilterItemPacket {
    private final BlockPos pos;
    private final int slot;
    private final ItemStack stack;

    public ConnectorFilterItemPacket(BlockPos pos, int slot, ItemStack stack) {
        this.pos = pos; this.slot = slot; this.stack = stack;
    }

    public ConnectorFilterItemPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos(); slot = buf.readVarInt(); stack = buf.readItem();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos); buf.writeVarInt(slot); buf.writeItem(stack);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof ItemConnectorMenu menu)) return;
            if (!menu.blockEntity.getBlockPos().equals(pos)) return;
            if (slot < 0 || slot >= menu.blockEntity.filterHandler.getSlots()) return;
            if (stack.isEmpty()) {
                menu.blockEntity.filterHandler.setStackInSlot(slot, ItemStack.EMPTY);
            } else {
                ItemStack ghost = stack.copy();
                ghost.setCount(1);
                menu.blockEntity.filterHandler.setStackInSlot(slot, ghost);
            }
        });
        return true;
    }
}
