package com.jykito.industrialcore.networking;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.jykito.industrialcore.block.entity.RecipeExporterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;


public class ExportRecipePacket implements CustomPacketPayload {
    public static final Type<ExportRecipePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "export_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExportRecipePacket> STREAM_CODEC =
            StreamCodec.ofMember(ExportRecipePacket::toBytes, ExportRecipePacket::new);

    @Override
    public Type<ExportRecipePacket> type() { return TYPE; }

    private final BlockPos pos;
    private final boolean shapeless;

    public ExportRecipePacket(BlockPos pos, boolean shapeless) {
        this.pos = pos;
        this.shapeless = shapeless;
    }

    public ExportRecipePacket(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.shapeless = buf.readBoolean();
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(shapeless);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            if (!player.level().isLoaded(pos)) return;
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;
            BlockEntity be = player.level().getBlockEntity(pos);
            if (be instanceof RecipeExporterBlockEntity exporter) {
                exporter.exportRecipe(player, shapeless);
            }
        });
    }
}
