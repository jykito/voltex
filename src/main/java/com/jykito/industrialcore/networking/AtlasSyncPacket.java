package com.jykito.industrialcore.networking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: the player's whole nanite species atlas (on join and on every species unlock). */
public class AtlasSyncPacket {
    private final CompoundTag atlas;

    public AtlasSyncPacket(CompoundTag atlas) { this.atlas = atlas; }
    public AtlasSyncPacket(FriendlyByteBuf buf) { this.atlas = buf.readNbt(); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeNbt(atlas); }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        // "new species" toasts are a client class, not loaded on the server
        ctx.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.jykito.industrialcore.client.NaniteAtlasToasts.onSync(atlas == null ? new CompoundTag() : atlas)));
        return true;
    }
}
