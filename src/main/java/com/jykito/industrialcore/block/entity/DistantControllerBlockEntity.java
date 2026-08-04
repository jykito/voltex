package com.jykito.industrialcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** BlockEntity пульта: хранит позицию привязанного ядра магнита; открывает его GUI на расстоянии. */
public class DistantControllerBlockEntity extends BlockEntity implements MenuProvider {

    @Nullable private BlockPos linkedCore;

    // клиент-кэш для BER экрана (троттл поиска рецепта): что варится + тик последнего обновления
    public ItemStack clientDisplayItem = ItemStack.EMPTY;
    public long clientDisplayTick = -100L;

    public DistantControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISTANT_CONTROLLER_BE.get(), pos, state);
    }

    public void setLinkedCore(@Nullable BlockPos core) {
        this.linkedCore = core;
        setChanged();
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);  // синк привязки на клиент (для BER)
    }
    @Nullable public BlockPos getLinkedCore() { return linkedCore; }

    /** Привязанное ядро (BE), либо null если не привязано / ядра больше нет. */
    @Nullable public ElectromagnetCoreBlockEntity resolveCore() {
        if (linkedCore == null || level == null) return null;
        return level.getBlockEntity(linkedCore) instanceof ElectromagnetCoreBlockEntity c ? c : null;
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.industrial_core.distant_controller");
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        ElectromagnetCoreBlockEntity core = resolveCore();
        return core == null ? null : core.remoteMenu(id, inv, this.worldPosition);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (linkedCore != null) tag.putLong("linkedCore", linkedCore.asLong());
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        linkedCore = tag.contains("linkedCore") ? BlockPos.of(tag.getLong("linkedCore")) : null;
    }

    // синк на клиент: BER экрана резолвит ядро по linkedCore на клиенте
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
