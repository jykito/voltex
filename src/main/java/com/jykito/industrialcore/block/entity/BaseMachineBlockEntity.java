package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.item.upgrade.AcceleratorItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import com.jykito.industrialcore.item.upgrade.ParallelModuleItem;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseMachineBlockEntity extends BlockEntity implements MenuProvider {

    protected static class MachineEnergyStorage extends EnergyStorage {
        public MachineEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0);
        }
        @Override public boolean canExtract() { return false; }
        public void consumeEnergy(int amount) { this.energy = Math.max(0, this.energy - amount); }
        public void setEnergy(int amount) { this.energy = amount; }
    }

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
            if (slot >= 2 && slot <= 5) return stack.getItem() instanceof MachineUpgradeItem;
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

    protected final MachineEnergyStorage energyStorage = new MachineEnergyStorage(64000, 1024);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

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

        Direction itemPusherDir = null;
        Direction itemPullerDir = null;
        int parallelCount = 1;

        float timeMultiplier   = 0.7f;
        float energyMultiplier = 1.0f;

        for (int s = 2; s <= 5; s++) {
            ItemStack upgrade = itemHandler.getStackInSlot(s);
            if (upgrade.isEmpty()) continue;
            if (upgrade.getItem() instanceof AcceleratorItem acc) {

                for (int c = 0; c < upgrade.getCount(); c++) {
                    timeMultiplier   *= acc.getSpeedMultiplier();
                    energyMultiplier *= acc.getEnergyMultiplier();
                }
            } else if (upgrade.getItem() instanceof DirectionalUpgradeItem dir) {
                Direction d = DirectionalUpgradeItem.getStoredDirection(upgrade);
                if (d != null) {
                    switch (dir.getMode()) {
                        case ITEM_PUSH -> itemPusherDir = d;
                        case ITEM_PULL -> itemPullerDir = d;
                        default -> {  }
                    }
                }
            } else if (upgrade.getItem() instanceof ParallelModuleItem pm) {
                parallelCount = Math.max(parallelCount, pm.getParallelCount());
            } else if (upgrade.getItem() instanceof com.jykito.industrialcore.item.upgrade.AdvancedParallelModuleItem adv) {
                parallelCount = Math.max(parallelCount, adv.getParallelCount());
            }
        }

        if (itemPullerDir != null) {
            ItemStack cur = itemHandler.getStackInSlot(0);
            boolean hasRoom = cur.isEmpty()
                    || cur.getCount() < Math.min(cur.getMaxStackSize(), itemHandler.getSlotLimit(0));
            if (hasRoom) {
                final Direction pullerDir = itemPullerDir;
                BlockEntity neighbor = level.getBlockEntity(pos.relative(pullerDir));
                if (neighbor != null) {
                    neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, pullerDir.getOpposite())
                            .ifPresent(nh -> {
                                for (int i = 0; i < nh.getSlots(); i++) {
                                    ItemStack sim = nh.extractItem(i, 64, true);
                                    if (sim.isEmpty()) continue;

                                    int accepted = sim.getCount() - itemHandler.insertItem(0, sim, true).getCount();
                                    if (accepted <= 0) continue;
                                    itemHandler.insertItem(0, nh.extractItem(i, accepted, false), false);
                                    setChanged();
                                    break;
                                }
                            });
                }
            }
        }

        boolean hasChanges = false;
        boolean isWorking  = false;

        if (recipeDirty || myRecipeGeneration != recipeGeneration) {
            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
            for (int i = 0; i < itemHandler.getSlots(); i++)
                inventory.setItem(i, itemHandler.getStackInSlot(i));
            cachedRecipe = level.getRecipeManager()
                    .getRecipeFor(getRecipeType(), inventory, level)
                    .orElse(null);
            recipeDirty = false;
            myRecipeGeneration = recipeGeneration;
        }

        if (cachedRecipe != null) {
            MachineRecipe r = cachedRecipe;
            ItemStack result = r.getResultItem(level.registryAccess());

            int inputAvail   = itemHandler.getStackInSlot(0).getCount() / r.getInputCount();
            int outputSpace  = result.getMaxStackSize() - itemHandler.getStackInSlot(1).getCount();
            int actualParallel = Math.max(1, Math.min(parallelCount,
                    Math.min(inputAvail, outputSpace / Math.max(1, result.getCount()))));

            if (canInsertItemIntoOutputSlot(result)
                    && canInsertAmountIntoOutputSlot(result.getCount() * actualParallel)) {

                this.maxProgress       = Math.max(1, (int)(r.getProcessingTime() * timeMultiplier));
                this.energyCostPerTick = Math.max(1, (int)(r.getEnergyCost()     * energyMultiplier));

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

        if (itemPusherDir != null) {
            ItemStack output = itemHandler.getStackInSlot(1);
            if (!output.isEmpty()) {
                final Direction pusherDir = itemPusherDir;
                BlockEntity neighbor = level.getBlockEntity(pos.relative(pusherDir));
                if (neighbor != null) {
                    neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, pusherDir.getOpposite())
                            .ifPresent(nh -> {
                                ItemStack remaining = ItemHandlerHelper.insertItem(nh, output.copy(), false);
                                if (remaining.getCount() < output.getCount()) {
                                    itemHandler.setStackInSlot(1,
                                            remaining.isEmpty() ? ItemStack.EMPTY : remaining);
                                    setChanged();
                                }
                            });
                }
            }
        }

        if (state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, isWorking), 3);
            hasChanges = true;
        }

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
        return out.getMaxStackSize() >= out.getCount() + amount;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY)       return lazyEnergyHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler   = LazyOptional.of(() -> automationHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
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
