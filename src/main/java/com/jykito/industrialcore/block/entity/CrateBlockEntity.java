package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import java.util.Optional;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.block.custom.CrateBlock;
import com.jykito.industrialcore.menu.CrateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrateBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

    private final int rows;
    private final ItemStackHandler items;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRATE_BE.get(), pos, state);
        this.rows = (state.getBlock() instanceof CrateBlock c) ? c.rows : 6;
        this.items = new ItemStackHandler(rows * 9) {
            @Override protected void onContentsChanged(int slot) { setChanged(); }
        };
    }

    public int getRows() { return rows; }
    public ItemStackHandler getItems() { return items; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CrateMenu(id, inv, this);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.ItemHandler.BLOCK) return (T) items;
        return null;
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("items", items.serializeNBT(registries));
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("items")) items.deserializeNBT(registries, tag.getCompound("items"));
    }
}
