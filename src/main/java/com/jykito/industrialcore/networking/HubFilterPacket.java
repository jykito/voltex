package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.block.entity.HubBlockEntity;
import com.jykito.industrialcore.menu.HubMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HubFilterPacket {
    private final BlockPos pos;
    private final int face, slot;
    private final boolean output;
    private final ItemStack stack;

    public HubFilterPacket(BlockPos pos, int face, int slot, ItemStack stack, boolean output) {
        this.pos = pos; this.face = face; this.slot = slot; this.stack = stack; this.output = output;
    }

    public HubFilterPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos(); face = buf.readVarInt(); slot = buf.readVarInt();
        output = buf.readBoolean(); stack = buf.readItem();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos); buf.writeVarInt(face); buf.writeVarInt(slot);
        buf.writeBoolean(output); buf.writeItem(stack);
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
            ItemStackHandler h = be.filter(output);
            if (stack.isEmpty()) {
                h.setStackInSlot(idx, ItemStack.EMPTY);
                if (!output) be.filterCaps[idx] = 0; // лимиты только у ввода
            } else {
                ItemStack ghost = stack.copy(); ghost.setCount(1);
                h.setStackInSlot(idx, ghost);
            }
            be.setChanged(); be.sync();
        });
        return true;
    }
}
