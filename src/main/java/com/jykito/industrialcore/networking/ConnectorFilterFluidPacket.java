package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.menu.FluidConnectorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConnectorFilterFluidPacket {
    private final BlockPos pos;
    private final int slot;
    private final FluidStack fluid;

    public ConnectorFilterFluidPacket(BlockPos pos, int slot, FluidStack fluid) {
        this.pos = pos; this.slot = slot; this.fluid = fluid;
    }

    public ConnectorFilterFluidPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos(); slot = buf.readVarInt(); fluid = FluidStack.readFromPacket(buf);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos); buf.writeVarInt(slot); fluid.writeToPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof FluidConnectorMenu menu)) return;
            if (!menu.blockEntity.getBlockPos().equals(pos)) return;
            if (slot < 0 || slot >= 5) return;
            menu.blockEntity.setFluidFilter(slot, fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy());
        });
        return true;
    }
}
