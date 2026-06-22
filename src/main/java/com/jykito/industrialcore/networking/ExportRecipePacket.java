package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.block.entity.RecipeExporterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExportRecipePacket {
    private final BlockPos pos;
    private final boolean shapeless;

    public ExportRecipePacket(BlockPos pos, boolean shapeless) {
        this.pos = pos;
        this.shapeless = shapeless;
    }

    public ExportRecipePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.shapeless = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(shapeless);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!player.level().isLoaded(pos)) return;
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;
            BlockEntity be = player.level().getBlockEntity(pos);
            if (be instanceof RecipeExporterBlockEntity exporter) {
                exporter.exportRecipe(player, shapeless);
            }
        });
        return true;
    }
}
