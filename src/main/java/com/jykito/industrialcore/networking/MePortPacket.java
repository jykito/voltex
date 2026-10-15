package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.jykito.industrialcore.menu.MePortMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;


/**
 * ME port window -> server: whatever does not fit into a slot click or a window button.
 * AMOUNT is the scroll wheel over a config cell (index: 0..26 items, 27..29 fluids, value is the step);
 * GHOST is a drag from JEI into a config cell; PRIORITY is the pattern port priority.
 */
public class MePortPacket implements CustomPacketPayload {
    public static final Type<MePortPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "me_port"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MePortPacket> STREAM_CODEC =
            StreamCodec.ofMember(MePortPacket::toBytes, MePortPacket::new);

    @Override
    public Type<MePortPacket> type() { return TYPE; }

    public static final int AMOUNT = 0, GHOST = 1, PRIORITY = 2;

    private final int containerId, op, index, value;
    private final ItemStack item;
    private final FluidStack fluid;

    public MePortPacket(int containerId, int op, int index, int value, ItemStack item, FluidStack fluid) {
        this.containerId = containerId; this.op = op; this.index = index; this.value = value; this.item = item; this.fluid = fluid;
    }

    public static MePortPacket amount(int containerId, int index, int delta) { return new MePortPacket(containerId, AMOUNT, index, delta, ItemStack.EMPTY, FluidStack.EMPTY); }
    public static MePortPacket ghost(int containerId, int index, ItemStack item, FluidStack fluid) { return new MePortPacket(containerId, GHOST, index, 0, item, fluid); }
    public static MePortPacket priority(int containerId, int value) { return new MePortPacket(containerId, PRIORITY, 0, value, ItemStack.EMPTY, FluidStack.EMPTY); }

    public MePortPacket(RegistryFriendlyByteBuf buf) {
        containerId = buf.readVarInt(); op = buf.readVarInt(); index = buf.readVarInt(); value = buf.readVarInt();
        item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf); fluid = FluidStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(containerId); buf.writeVarInt(op); buf.writeVarInt(index); buf.writeVarInt(value);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, item); FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, fluid);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null || !(player.containerMenu instanceof MePortMenu menu) || menu.containerId != containerId) return;
            if (!menu.stillValid(player)) return;
            switch (op) {
                case AMOUNT -> menu.adjustAmount(index, value);
                case GHOST -> menu.setGhost(index, item, fluid);
                case PRIORITY -> menu.setPriority(value);
                default -> {}
            }
        });
    }
}
