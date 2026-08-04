package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.block.entity.HubBlockEntity;
import com.jykito.industrialcore.menu.HubMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HubFluidFilterPacket {
    private final BlockPos pos;
    private final int face, slot;
    private final boolean output;
    private final FluidStack fluid;

    public HubFluidFilterPacket(BlockPos pos, int face, int slot, boolean output, FluidStack fluid) {
        this.pos = pos; this.face = face; this.slot = slot; this.output = output; this.fluid = fluid;
    }

    public HubFluidFilterPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos(); face = buf.readVarInt(); slot = buf.readVarInt();
        output = buf.readBoolean(); fluid = FluidStack.readFromPacket(buf);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos); buf.writeVarInt(face); buf.writeVarInt(slot);
        buf.writeBoolean(output); fluid.writeToPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.containerMenu instanceof HubMenu menu) || menu.be == null) return;
            HubBlockEntity be = menu.be;
            if (!be.fluid || !be.getBlockPos().equals(pos)) return;
            if (face < 0 || face >= 6 || slot < 0 || slot >= HubBlockEntity.FILTER_SLOTS) return;
            be.setFluidFilter(face, slot, output, fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy());
        });
        return true;
    }
}
