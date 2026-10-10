package com.jykito.industrialcore.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Сервер → клиенты рядом со стрелком: выстрел гаусса (откуда, куда, ступень, плазма, плазменный взрыв, кто стрелял).
 * Клиент рисует луч, спираль и вспышки (GaussBeams), а стрелку даёт отдачу камерой.
 */
public class GaussShotPacket {
    private final Vec3 from, to;
    private final int stage, shooter;
    private final boolean plasma, blast;

    public GaussShotPacket(Vec3 from, Vec3 to, int stage, boolean plasma, boolean blast, int shooter) {
        this.from = from; this.to = to; this.stage = stage; this.plasma = plasma; this.blast = blast; this.shooter = shooter;
    }

    public GaussShotPacket(FriendlyByteBuf buf) {
        from = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        to = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        stage = buf.readByte(); plasma = buf.readBoolean(); blast = buf.readBoolean(); shooter = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeDouble(from.x); buf.writeDouble(from.y); buf.writeDouble(from.z);
        buf.writeDouble(to.x); buf.writeDouble(to.y); buf.writeDouble(to.z);
        buf.writeByte(stage); buf.writeBoolean(plasma); buf.writeBoolean(blast); buf.writeVarInt(shooter);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        // рисование — клиентский класс, на сервере не грузится
        ctx.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.jykito.industrialcore.client.weapon.GaussBeams.add(from, to, stage, plasma, blast, shooter)));
        return true;
    }
}
