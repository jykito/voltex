package com.jykito.industrialcore.networking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Сервер → клиент: атлас видов нанитов игрока целиком (при входе и при каждом открытии вида). */
public class AtlasSyncPacket {
    private final CompoundTag atlas;

    public AtlasSyncPacket(CompoundTag atlas) { this.atlas = atlas; }
    public AtlasSyncPacket(FriendlyByteBuf buf) { this.atlas = buf.readNbt(); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeNbt(atlas); }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        // всплывашки «новый вид» — клиентский класс, на сервере не грузится
        ctx.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.jykito.industrialcore.client.NaniteAtlasToasts.onSync(atlas == null ? new CompoundTag() : atlas)));
        return true;
    }
}
