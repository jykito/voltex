package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.RodFabricatorBlock;
import com.jykito.industrialcore.menu.RodFabricatorMenu;
import com.jykito.industrialcore.recipe.RodFabricatorRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RodFabricatorBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements MenuProvider {

    private static class RodFabricatorEnergyStorage extends EnergyStorage {
        public RodFabricatorEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0);
        }
        public void consumeEnergy(int amount) { this.energy = Math.max(0, this.energy - amount); }
        public void setEnergy(int amount) { this.energy = amount; }
    }

    private static final int MAX_ENERGY  = 100_000;
    private static final int MAX_RECEIVE = 2_000;

    private int maxProgress    = 200;
    private int energyPerTick  = 500;

    private final com.jykito.industrialcore.recipe.CachedRecipe<RodFabricatorRecipe> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(7) {
        @Override
        protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0, 1 -> !(stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem);
                case 2    -> false;
                case 3, 4, 5, 6 -> stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
                default   -> false;
            };
        }
    };

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot >= 3) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 2) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    private final RodFabricatorEnergyStorage energyStorage = new RodFabricatorEnergyStorage(MAX_ENERGY, MAX_RECEIVE);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    private int progress = 0;

    protected final ContainerData data = new ContainerData() {
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
        public void set(int index, int value) {}
        @Override
        public int getCount() { return 6; }
    };

    public RodFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROD_FABRICATOR_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.rod_fabricator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RodFabricatorMenu(id, inventory, this, this.data);
    }

    private java.util.Optional<RodFabricatorRecipe> getRecipe(Level level) {
        return java.util.Optional.ofNullable(recipeCache.get(() -> {
            SimpleContainer c = new SimpleContainer(2);
            c.setItem(0, itemHandler.getStackInSlot(0));
            c.setItem(1, itemHandler.getStackInSlot(1));
            return level.getRecipeManager().getRecipeFor(ModRecipes.ROD_FABRICATION_TYPE.get(), c, level).orElse(null);
        }));
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        boolean hasChanges = false;
        boolean isWorking  = false;

        float timeMultiplier   = 1.0f;
        float energyMultiplier = 1.0f;
        int parallel = 1;
        Direction itemPullDir = null, itemPushDir = null;
        for (int s = 3; s <= 6; s++) {
            ItemStack up = itemHandler.getStackInSlot(s);
            if (up.getItem() instanceof com.jykito.industrialcore.item.upgrade.AcceleratorItem acc) {
                for (int c = 0; c < up.getCount(); c++) {
                    timeMultiplier   *= acc.getSpeedMultiplier();
                    energyMultiplier *= acc.getEnergyMultiplier();
                }
            } else if (up.getItem() instanceof com.jykito.industrialcore.item.upgrade.ParallelModuleItem pm) {
                parallel = Math.max(parallel, pm.getParallelCount());
            } else if (up.getItem() instanceof com.jykito.industrialcore.item.upgrade.AdvancedParallelModuleItem adv) {
                parallel = Math.max(parallel, adv.getParallelCount());
            } else if (up.getItem() instanceof com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem dir) {
                Direction d = com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem.getStoredDirection(up);
                if (d != null) {
                    if (dir.getMode() == com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem.Mode.ITEM_PULL) itemPullDir = d;
                    else if (dir.getMode() == com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem.Mode.ITEM_PUSH) itemPushDir = d;
                }
            }
        }

        java.util.Optional<RodFabricatorRecipe> recipe = getRecipe(level);
        if (recipe.isPresent()) {
            RodFabricatorRecipe r  = recipe.get();
            maxProgress      = Math.max(1, (int)(r.getProcessingTime()    * timeMultiplier));
            energyPerTick    = Math.max(1, (int)(r.getEnergyCostPerTick() * energyMultiplier));
            ItemStack result = r.getResultItem(level.registryAccess());
            ItemStack output = itemHandler.getStackInSlot(2);
            boolean canFit   = output.isEmpty()
                    || (output.is(result.getItem()) && output.getCount() < output.getMaxStackSize());

            if (canFit && energyStorage.getEnergyStored() >= energyPerTick) {
                energyStorage.consumeEnergy(energyPerTick);
                progress++;
                isWorking  = true;
                hasChanges = true;

                if (progress >= maxProgress) {
                    progress = 0;

                    int inputRuns = Integer.MAX_VALUE;
                    if (r.getCount1() > 0) inputRuns = Math.min(inputRuns, itemHandler.getStackInSlot(0).getCount() / r.getCount1());
                    if (r.getCount2() > 0) inputRuns = Math.min(inputRuns, itemHandler.getStackInSlot(1).getCount() / r.getCount2());
                    int resCount = Math.max(1, result.getCount());
                    int outRuns  = (output.isEmpty()
                            ? result.getMaxStackSize()
                            : output.getMaxStackSize() - output.getCount()) / resCount;
                    int actualParallel = Math.max(1, Math.min(parallel, Math.min(inputRuns, outRuns)));

                    itemHandler.extractItem(0, r.getCount1() * actualParallel, false);
                    itemHandler.extractItem(1, r.getCount2() * actualParallel, false);
                    if (output.isEmpty()) {
                        ItemStack out = result.copy();
                        out.setCount(result.getCount() * actualParallel);
                        itemHandler.setStackInSlot(2, out);
                    } else {
                        output.grow(result.getCount() * actualParallel);
                    }
                }
            }
        } else {
            if (progress > 0) { progress = 0; hasChanges = true; }
        }

        if (itemPullDir != null) {
            final Direction d = itemPullDir;
            var n = level.getBlockEntity(pos.relative(d));
            if (n != null) n.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).ifPresent(src -> {
                for (int i = 0; i < src.getSlots(); i++) {
                    ItemStack sim = src.extractItem(i, 64, true);
                    if (sim.isEmpty()) continue;
                    int target = isValidInput(0, sim) ? 0 : (isValidInput(1, sim) ? 1 : -1);
                    if (target < 0) continue;
                    int accepted = sim.getCount() - itemHandler.insertItem(target, sim, true).getCount();
                    if (accepted > 0) {
                        itemHandler.insertItem(target, src.extractItem(i, accepted, false), false);
                        return;
                    }
                }
            });
        }

        if (itemPushDir != null && !itemHandler.getStackInSlot(2).isEmpty()) {
            final Direction d = itemPushDir;
            var n = level.getBlockEntity(pos.relative(d));
            if (n != null) n.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).ifPresent(dst ->
                    itemHandler.setStackInSlot(2, net.minecraftforge.items.ItemHandlerHelper.insertItem(dst, itemHandler.getStackInSlot(2), false)));
        }

        if (state.getValue(RodFabricatorBlock.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(RodFabricatorBlock.LIT, isWorking), 3);
        }

        if (hasChanges) setChanged();
    }

    public boolean isValidInput(int slotIndex, ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        for (RodFabricatorRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.ROD_FABRICATION_TYPE.get())) {
            var ing = slotIndex == 0 ? r.getIngredient1() : r.getIngredient2();
            if (ing.test(stack)) return true;
        }
        return false;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergyHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> automationHandler);
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
