package com.jykito.industrialcore.block.entity;

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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrateBlockEntity extends BlockEntity implements MenuProvider {

    private final int rows;
    private final ItemStackHandler items;
    private LazyOptional<IItemHandler> lazyItems = LazyOptional.empty();

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
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItems.cast();
        return super.getCapability(cap, side);
    }

    @Override public void onLoad()         { super.onLoad();         lazyItems = LazyOptional.of(() -> items); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyItems.invalidate(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("items", items.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("items")) items.deserializeNBT(tag.getCompound("items"));
    }
}
