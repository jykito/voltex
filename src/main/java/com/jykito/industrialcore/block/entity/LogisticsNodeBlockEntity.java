package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;

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
            if (be instanceof LogisticsNodeBlockEntity n)  n.removeLink(worldPosition);
            else if (be instanceof HubBlockEntity h)       h.removeLink(worldPosition);
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
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("links", linkedPositions.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        linkedPositions.clear();
        for (long l : tag.getLongArray("links")) linkedPositions.add(BlockPos.of(l));
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { CompoundTag t = new CompoundTag(); saveAdditional(t, registries); return t; }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) { loadAdditional(tag, registries); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
