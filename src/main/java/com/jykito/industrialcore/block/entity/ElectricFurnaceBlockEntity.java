package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.ElectricFurnaceBlock;
import com.jykito.industrialcore.menu.ElectricFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ElectricFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    private static class FurnaceEnergyStorage extends EnergyStorage {
        FurnaceEnergyStorage() { super(10000, 100, 0); }
        void setEnergy(int amount) { this.energy = amount; }

        void consumeEnergy(int amount) { this.energy = Math.max(0, this.energy - amount); }
    }

    private final FurnaceEnergyStorage energyStorage = new FurnaceEnergyStorage();
    private LazyOptional<IEnergyStorage> energyOptional = LazyOptional.empty();

    private final com.jykito.industrialcore.recipe.CachedRecipe<SmeltingRecipe> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(6) {
        @Override
        protected void onContentsChanged(int slot) {
            recipeCache.markDirty();
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) return !(stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem);
            if (slot >= 2 && slot <= 5) return stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
            return false;
        }
    };
    private LazyOptional<IItemHandler> itemOptional = LazyOptional.empty();

    private final net.minecraftforge.items.IItemHandlerModifiable automationHandler = new net.minecraftforge.items.IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot != 0) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 1) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };

    public int progress = 0;
    public int maxProgress = 100;
    private static final int BASE_MAX_PROGRESS = 100;
    private static final int BASE_ENERGY_PER_TICK = 20;

    protected final ContainerData data;

    public ElectricFurnaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.ELECTRIC_FURNACE_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> ElectricFurnaceBlockEntity.this.energyStorage.getEnergyStored() & 0xFFFF;
                    case 1 -> (ElectricFurnaceBlockEntity.this.energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 2 -> ElectricFurnaceBlockEntity.this.energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 3 -> (ElectricFurnaceBlockEntity.this.energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> ElectricFurnaceBlockEntity.this.progress;
                    case 5 -> ElectricFurnaceBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }
            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 4 -> ElectricFurnaceBlockEntity.this.progress = pValue;
                    case 5 -> ElectricFurnaceBlockEntity.this.maxProgress = pValue;
                }
            }
            @Override
            public int getCount() { return 6; }
        };
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.electric_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new ElectricFurnaceMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectricFurnaceBlockEntity entity) {
        if (level.isClientSide()) return;

        float timeMultiplier   = 1.0f;
        float energyMultiplier = 1.0f;
        int parallelCount      = 1;
        for (int s = 2; s <= 5; s++) {
            ItemStack upgrade = entity.itemHandler.getStackInSlot(s);
            if (upgrade.isEmpty()) continue;
            if (upgrade.getItem() instanceof com.jykito.industrialcore.item.upgrade.AcceleratorItem acc) {
                for (int c = 0; c < upgrade.getCount(); c++) {
                    timeMultiplier   *= acc.getSpeedMultiplier();
                    energyMultiplier *= acc.getEnergyMultiplier();
                }
            } else if (upgrade.getItem() instanceof com.jykito.industrialcore.item.upgrade.ParallelModuleItem pm) {
                parallelCount = Math.max(parallelCount, pm.getParallelCount());
            } else if (upgrade.getItem() instanceof com.jykito.industrialcore.item.upgrade.AdvancedParallelModuleItem adv) {
                parallelCount = Math.max(parallelCount, adv.getParallelCount());
            }
        }

        entity.maxProgress = Math.max(1, (int) (BASE_MAX_PROGRESS * timeMultiplier));
        int energyCost     = Math.max(1, (int) (BASE_ENERGY_PER_TICK * energyMultiplier));

        boolean hasEnergy = entity.energyStorage.getEnergyStored() >= energyCost;
        boolean hasRecipe = entity.hasValidRecipe();

        if (hasEnergy && hasRecipe) {
            entity.progress++;
            entity.energyStorage.consumeEnergy(energyCost);
            setLit(level, pos, state, true);

            if (entity.progress >= entity.maxProgress) {
                entity.craftItem(parallelCount);
            }
        } else {
            entity.progress = 0;
            setLit(level, pos, state, false);
        }
    }

    @Nullable
    private SmeltingRecipe currentRecipe() {
        return recipeCache.get(() -> {
            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
            for (int i = 0; i < itemHandler.getSlots(); i++)
                inventory.setItem(i, itemHandler.getStackInSlot(i));
            return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, inventory, level).orElse(null);
        });
    }

    private boolean hasValidRecipe() {
        if (itemHandler.getStackInSlot(0).isEmpty()) return false;
        SmeltingRecipe recipe = currentRecipe();
        if (recipe != null) {
            ItemStack outputItem = itemHandler.getStackInSlot(1);
            ItemStack recipeResult = recipe.getResultItem(level.registryAccess());
            return outputItem.isEmpty() || (outputItem.getItem() == recipeResult.getItem() && outputItem.getCount() + recipeResult.getCount() <= outputItem.getMaxStackSize());
        }
        return false;
    }

    private void craftItem(int parallelCount) {
        SmeltingRecipe recipe = currentRecipe();
        if (recipe != null) {
            ItemStack result = recipe.getResultItem(level.registryAccess());
            int perCraftOut  = Math.max(1, result.getCount());

            ItemStack in  = itemHandler.getStackInSlot(0);
            ItemStack out = itemHandler.getStackInSlot(1);
            int inputAvail  = in.getCount();
            int outputSpace = (out.isEmpty() ? result.getMaxStackSize() : out.getMaxStackSize() - out.getCount()) / perCraftOut;
            int actual = Math.max(1, Math.min(parallelCount, Math.min(inputAvail, Math.max(0, outputSpace))));

            if (actual <= 0) { this.progress = 0; return; }

            ItemStack newIn = in.copy();
            newIn.shrink(actual);
            itemHandler.setStackInSlot(0, newIn.isEmpty() ? ItemStack.EMPTY : newIn);

            int newOutCount = (out.isEmpty() ? 0 : out.getCount()) + perCraftOut * actual;
            itemHandler.setStackInSlot(1, new ItemStack(result.getItem(), newOutCount));

            this.progress = 0;
            setChanged();
        }
    }

    private static void setLit(Level level, BlockPos pos, BlockState state, boolean isLit) {
        if (state.getValue(ElectricFurnaceBlock.LIT) != isLit) {
            level.setBlock(pos, state.setValue(ElectricFurnaceBlock.LIT, isLit), 3);
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyOptional.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemOptional.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        energyOptional = LazyOptional.of(() -> energyStorage);

        itemOptional   = LazyOptional.of(() -> automationHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOptional.invalidate();
        itemOptional.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("progress", progress);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        progress = tag.getInt("progress");
    }
}
