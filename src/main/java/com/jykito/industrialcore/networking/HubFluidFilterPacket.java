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
import net.neoforged.neoforge.fluids.FluidStack;


public class HubFluidFilterPacket implements CustomPacketPayload {
    public static final Type<HubFluidFilterPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "hub_fluid_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HubFluidFilterPacket> STREAM_CODEC =
            StreamCodec.ofMember(HubFluidFilterPacket::toBytes, HubFluidFilterPacket::new);

    @Override
    public Type<HubFluidFilterPacket> type() { return TYPE; }

    private final BlockPos pos;
    private final int face, slot;
    private final boolean output;
    private final FluidStack fluid;

    public HubFluidFilterPacket(BlockPos pos, int face, int slot, boolean output, FluidStack fluid) {
        this.pos = pos; this.face = face; this.slot = slot; this.output = output; this.fluid = fluid;
    }

    public HubFluidFilterPacket(RegistryFriendlyByteBuf buf) {
        pos = buf.readBlockPos(); face = buf.readVarInt(); slot = buf.readVarInt();
        output = buf.readBoolean(); fluid = FluidStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos); buf.writeVarInt(face); buf.writeVarInt(slot);
        buf.writeBoolean(output); FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, fluid);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            if (!(player.containerMenu instanceof HubMenu menu) || menu.be == null) return;
            HubBlockEntity be = menu.be;
            if (!be.fluid || !be.getBlockPos().equals(pos)) return;
            if (face < 0 || face >= 6 || slot < 0 || slot >= HubBlockEntity.FILTER_SLOTS) return;
            be.setFluidFilter(face, slot, output, fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy());
        });
    }
}
