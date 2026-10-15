package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
import com.jykito.industrialcore.recipe.MachineRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseMachineBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

    private static volatile int recipeGeneration = 0;
    public static void markAllRecipesDirty() { recipeGeneration++; }

    @Nullable private MachineRecipe cachedRecipe = null;
    private boolean recipeDirty = true;
    private int myRecipeGeneration = -1;

    protected final ItemStackHandler itemHandler = new ItemStackHandler(6) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot == 0) recipeDirty = true;
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) return !(stack.getItem() instanceof MachineUpgradeItem);
            if (slot >= 2 && slot <= 5) return UpgradeTargets.works(getBlockState().getBlock(), stack);
            return false;
        }
    };

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot >= 2 && slot <= 5) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 0) return ItemStack.EMPTY;
            if (slot >= 2 && slot <= 5) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };

    // intake = buffer: the input cap limited nothing (recipe usage is an order of magnitude lower),
    // it only slowed refilling after idling. Same as the electric furnace.
    protected final MachineEnergy energyStorage = MachineEnergy.consumer(64000, 64000);

    protected int progress = 0;
    protected int maxProgress = 200;
    protected int energyCostPerTick = 50;

    public final ContainerData baseData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> progress;
                case 5 -> maxProgress;
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            switch (index) {
                case 4 -> progress = value;
                case 5 -> maxProgress = value;
            }
        }
        @Override
        public int getCount() { return 6; }
    };

    public BaseMachineBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    protected abstract RecipeType<MachineRecipe> getRecipeType();

    public ItemStackHandler getItemHandler() { return itemHandler; }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        MachineUpgrades upg = MachineUpgrades.scan(itemHandler, 2, 5);

        if (upg.itemPull() != null && MachineIO.pullItems(level, pos, upg.itemPull(), itemHandler, 0)) setChanged();

        boolean hasChanges = false;
        boolean isWorking  = false;

        if (recipeDirty || myRecipeGeneration != recipeGeneration) {
            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
            for (int i = 0; i < itemHandler.getSlots(); i++)
                inventory.setItem(i, itemHandler.getStackInSlot(i));
            cachedRecipe = com.jykito.industrialcore.recipe.RecipeLookup.find(level.getRecipeManager(), getRecipeType(), inventory, level)
                    .orElse(null);
            recipeDirty = false;
            myRecipeGeneration = recipeGeneration;
        }

        if (cachedRecipe != null) {
            MachineRecipe r = cachedRecipe;
            ItemStack result = r.getResultItem(level.registryAccess());

            int inputAvail   = itemHandler.getStackInSlot(0).getCount() / r.getInputCount();
            int outputSpace  = result.getMaxStackSize() - itemHandler.getStackInSlot(1).getCount();
            int actualParallel = Math.max(1, Math.min(upg.parallel(),
                    Math.min(inputAvail, outputSpace / Math.max(1, result.getCount()))));

            if (canInsertItemIntoOutputSlot(result)
                    && canInsertAmountIntoOutputSlot(result.getCount() * actualParallel)) {

                // Time comes from the recipe as is, only upgrades multiply it. The old 0.7 (a global
                // "-30% time") was moved straight into the json of the crushing/compressing/extracting/
                // rolling/extruding recipes, otherwise JEI showed the time from the file while the machine ran faster.
                this.maxProgress       = upg.scaleTime(r.getProcessingTime());
                this.energyCostPerTick = upg.scaleEnergy(r.getEnergyCost());

                if (energyStorage.getEnergyStored() >= energyCostPerTick) {
                    energyStorage.consumeEnergy(energyCostPerTick);
                    progress++;
                    hasChanges = true;
                    isWorking  = true;

                    if (progress >= maxProgress) {
                        craftItem(r, actualParallel);
                        hasChanges = true;
                    }
                }
            } else if (progress > 0) {
                progress = 0;
                hasChanges = true;
            }
        } else {
            if (progress > 0) {
                progress = 0;
                hasChanges = true;
            }
        }

        if (upg.itemPush() != null && MachineIO.pushItems(level, pos, upg.itemPush(), itemHandler, 1)) setChanged();

        MachineLit.tick(this, level, pos, state, BlockStateProperties.LIT, isWorking);

        if (hasChanges) setChanged();
    }

    private void craftItem(MachineRecipe recipe, int parallelCount) {
        ItemStack result = recipe.getResultItem(level.registryAccess());

        ItemStack input = itemHandler.getStackInSlot(0).copy();
        input.shrink(recipe.getInputCount() * parallelCount);
        itemHandler.setStackInSlot(0, input.isEmpty() ? ItemStack.EMPTY : input);

        int newOut = itemHandler.getStackInSlot(1).getCount() + result.getCount() * parallelCount;
        itemHandler.setStackInSlot(1, new ItemStack(result.getItem(), newOut));
        progress = 0;
    }

    private boolean canInsertItemIntoOutputSlot(ItemStack output) {
        ItemStack out = itemHandler.getStackInSlot(1);
        return out.isEmpty() || out.getItem() == output.getItem();
    }

    private boolean canInsertAmountIntoOutputSlot(int amount) {
        ItemStack out = itemHandler.getStackInSlot(1);
        // 1.21: an empty stack reports max size 1 (it reads the component map), in 1.20.1 it was 64 (air).
        // The amount is already capped by the result's stack size in tick(), so an empty slot always fits.
        return out.isEmpty() || out.getMaxStackSize() >= out.getCount() + amount;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK)       return (T) energyStorage;
        if (cap == Capabilities.ItemHandler.BLOCK) return (T) automationHandler;
        return null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("progress", progress);
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        progress = tag.getInt("progress");
    }
}
