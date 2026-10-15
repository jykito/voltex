package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.block.custom.ResinCollectorBlock;
import com.jykito.industrialcore.block.custom.RubberLogBlock;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.menu.ResinCollectorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResinCollectorBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

    private static final int SLOTS = 9;
    private static final int TICK_INTERVAL = 100;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };

    private int tickCounter = 0;

    public ResinCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESIN_COLLECTOR_BE.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (++tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;

        Direction facing = state.getValue(ResinCollectorBlock.FACING);

        BlockPos targetLog = null;
        BlockState targetState = null;
        BlockPos scan = pos.relative(facing);
        while (level.getBlockState(scan).getBlock() instanceof RubberLogBlock) {
            BlockState scanState = level.getBlockState(scan);
            if (scanState.getValue(RubberLogBlock.LATEX_STATE) == 2) {
                targetLog = scan;
                targetState = scanState;
                break;
            }
            scan = scan.above();
        }
        if (targetLog == null) return;

        int amount = level.random.nextIntBetweenInclusive(1, 3);
        ItemStack resin = new ItemStack(ModItems.TREE_RESIN.get(), amount);

        for (int i = 0; i < SLOTS; i++) {
            resin = itemHandler.insertItem(i, resin, false);
            if (resin.isEmpty()) break;
        }

        if (resin.getCount() < amount) {
            level.setBlock(targetLog, targetState.setValue(RubberLogBlock.LATEX_STATE, 1), 3);
            setChanged();
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.resin_collector");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ResinCollectorMenu(id, inv, this);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.ItemHandler.BLOCK) return (T) itemHandler;
        return null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
    }
}
