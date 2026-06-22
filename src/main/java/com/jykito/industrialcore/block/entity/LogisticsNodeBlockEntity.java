package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.menu.LogisticsNodeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LogisticsNodeBlockEntity extends BlockEntity implements MenuProvider {

    public final List<BlockPos> linkedPositions = new ArrayList<>();

    public LogisticsNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOGISTICS_NODE_BE.get(), pos, state);
    }

    public void addLink(BlockPos other) {
        if (!linkedPositions.contains(other)) {
            linkedPositions.add(other);
            setChanged(); sync();
        }
    }

    public void removeLink(BlockPos other) {
        linkedPositions.remove(other);
        setChanged(); sync();
    }

    public void clearAllLinks(net.minecraft.world.level.Level level) {
        for (BlockPos linked : new HashSet<>(linkedPositions)) {
            BlockEntity be = level.getBlockEntity(linked);
            if (be instanceof ItemConnectorBlockEntity c)  c.removeLink(worldPosition);
            else if (be instanceof FluidConnectorBlockEntity c) c.removeLink(worldPosition);
            else if (be instanceof LogisticsNodeBlockEntity n)  n.removeLink(worldPosition);
        }
        linkedPositions.clear();
        setChanged();
        sync();
    }

    public void sync() {
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.logistics_node");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new LogisticsNodeMenu(id, inv, worldPosition);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putLongArray("links", linkedPositions.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedPositions.clear();
        for (long l : tag.getLongArray("links")) linkedPositions.add(BlockPos.of(l));
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = new CompoundTag(); saveAdditional(t); return t; }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
