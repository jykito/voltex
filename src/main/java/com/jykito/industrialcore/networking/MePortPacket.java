package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.menu.MePortMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * ME port window -> server: whatever does not fit into a slot click or a window button.
 * AMOUNT is the scroll wheel over a config cell (index: 0..26 items, 27..29 fluids, value is the step);
 * GHOST is a drag from JEI into a config cell; PRIORITY is the pattern port priority.
 */
public class MePortPacket {
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

    public MePortPacket(FriendlyByteBuf buf) {
        containerId = buf.readVarInt(); op = buf.readVarInt(); index = buf.readVarInt(); value = buf.readVarInt();
        item = buf.readItem(); fluid = FluidStack.readFromPacket(buf);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId); buf.writeVarInt(op); buf.writeVarInt(index); buf.writeVarInt(value);
        buf.writeItem(item); fluid.writeToPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !(player.containerMenu instanceof MePortMenu menu) || menu.containerId != containerId) return;
            if (!menu.stillValid(player)) return;
            switch (op) {
                case AMOUNT -> menu.adjustAmount(index, value);
                case GHOST -> menu.setGhost(index, item, fluid);
                case PRIORITY -> menu.setPriority(value);
                default -> {}
            }
        });
        return true;
    }
}
