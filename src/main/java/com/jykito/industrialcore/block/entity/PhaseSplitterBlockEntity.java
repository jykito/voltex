package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.item.upgrade.*;
import com.jykito.industrialcore.menu.PhaseSplitterMenu;
import com.jykito.industrialcore.recipe.ModRecipes;
import com.jykito.industrialcore.recipe.PhaseSplitterRecipe;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PhaseSplitterBlockEntity extends BlockEntity implements MenuProvider {

    private final com.jykito.industrialcore.recipe.CachedRecipe<PhaseSplitterRecipe> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();

    protected final ItemStackHandler itemHandler = new ItemStackHandler(8) {
        @Override
        protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) return !(stack.getItem() instanceof MachineUpgradeItem);
            if (slot >= 1 && slot <= 3) return false;
            if (slot >= 4 && slot <= 7) {

                return stack.getItem() instanceof MachineUpgradeItem;
            }
            return false;
        }
    };

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack s) { itemHandler.setStackInSlot(slot, s); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot >= 1) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 0 || slot >= 4) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack s) { return itemHandler.isItemValid(slot, s); }
    };

    private Optional<PhaseSplitterRecipe> currentRecipe(Level level) {
        return Optional.ofNullable(recipeCache.get(() -> {
            SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
            for (int i = 0; i < itemHandler.getSlots(); i++)
                inventory.setItem(i, itemHandler.getStackInSlot(i));
            return level.getRecipeManager()
                    .getRecipeFor(ModRecipes.PHASE_SPLITTING_TYPE.get(), inventory, level).orElse(null);
        }));
    }

    protected final BaseMachineBlockEntity.MachineEnergyStorage energyStorage =
            new BaseMachineBlockEntity.MachineEnergyStorage(200_000, 4_000);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    protected int progress = 0;
    protected int maxProgress = 400;
    protected int energyCostPerTick = 100;

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
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 6; }
    };

    public PhaseSplitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHASE_SPLITTER_BE.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.phase_splitter"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PhaseSplitterMenu(id, inventory, this, this.data);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        float timeMultiplier   = 1.0f;
        float energyMultiplier = 1.0f;
        Direction itemPusherDir = null;
        Direction itemPullerDir = null;
        int parallelCount = 1;

        for (int s = 4; s <= 7; s++) {
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
                        default -> {}
                    }
                }
            } else if (upgrade.getItem() instanceof ParallelModuleItem pm) {
                parallelCount = Math.max(parallelCount, pm.getParallelCount());
            } else if (upgrade.getItem() instanceof AdvancedParallelModuleItem adv) {
                parallelCount = Math.max(parallelCount, adv.getParallelCount());
            }
        }

        if (itemPullerDir != null && itemHandler.getStackInSlot(0).isEmpty()) {
            final Direction dir = itemPullerDir;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).ifPresent(nh -> {
                    for (int i = 0; i < nh.getSlots(); i++) {
                        ItemStack sim = nh.extractItem(i, 64, true);
                        if (sim.isEmpty() || !itemHandler.isItemValid(0, sim)) continue;
                        int accepted = sim.getCount() - itemHandler.insertItem(0, sim, true).getCount();
                        if (accepted > 0) {
                            itemHandler.insertItem(0, nh.extractItem(i, accepted, false), false);
                            setChanged();
                            break;
                        }
                    }
                });
            }
        }

        boolean hasChanges = false;
        boolean isWorking  = false;

        Optional<PhaseSplitterRecipe> recipe = currentRecipe(level);

        if (recipe.isPresent()) {
            PhaseSplitterRecipe r = recipe.get();
            ItemStack o1 = r.getOutput1(), o2 = r.getOutput2(), o3 = r.getOutput3();
            int inputAvail = itemHandler.getStackInSlot(0).getCount() / r.getInputCount();
            int actualParallel = Math.max(1, Math.min(parallelCount, inputAvail));

            while (actualParallel > 1 && !canFitOutputs(o1, o2, o3, actualParallel)) actualParallel--;

            if (canFitOutputs(o1, o2, o3, actualParallel)) {
                this.maxProgress      = Math.max(1, (int)(r.getProcessingTime() * timeMultiplier));
                this.energyCostPerTick = Math.max(1, (int)(r.getEnergyCost()    * energyMultiplier));

                if (energyStorage.getEnergyStored() >= energyCostPerTick) {
                    energyStorage.consumeEnergy(energyCostPerTick);
                    progress++;
                    hasChanges = true;
                    isWorking  = true;

                    if (progress >= maxProgress) { craftItem(r, actualParallel); hasChanges = true; }
                }
            } else if (progress > 0) { progress = 0; hasChanges = true; }
        } else {
            if (progress > 0) { progress = 0; hasChanges = true; }
        }

        if (itemPusherDir != null) {
            for (int outSlot = 1; outSlot <= 3; outSlot++) {
                ItemStack output = itemHandler.getStackInSlot(outSlot);
                if (output.isEmpty()) continue;
                final Direction dir = itemPusherDir;
                final int slot = outSlot;
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor != null) {
                    neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).ifPresent(nh -> {
                        ItemStack remaining = ItemHandlerHelper.insertItem(nh, output.copy(), false);
                        if (remaining.getCount() < output.getCount()) {
                            itemHandler.setStackInSlot(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
                            setChanged();
                        }
                    });
                }
            }
        }

        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, isWorking), 3);
            hasChanges = true;
        }

        if (hasChanges) setChanged();
    }

    private boolean canFitOutputs(ItemStack o1, ItemStack o2, ItemStack o3, int parallel) {

        net.minecraft.world.item.Item[] slotItem  = new net.minecraft.world.item.Item[3];
        int[] slotSpace = new int[3];
        for (int i = 0; i < 3; i++) {
            ItemStack cur = itemHandler.getStackInSlot(i + 1);
            slotItem[i]  = cur.isEmpty() ? null : cur.getItem();
            slotSpace[i] = cur.isEmpty() ? Integer.MAX_VALUE : (cur.getMaxStackSize() - cur.getCount());
        }

        if (!o1.isEmpty()) {
            int needed = o1.getCount() * parallel;
            for (int i = 0; i < 3 && needed > 0; i++) {
                if (i == 1 && !o2.isEmpty()) break;
                if (i == 2 && !o3.isEmpty()) break;
                if (slotItem[i] != null && slotItem[i] != o1.getItem()) break;
                int space = slotItem[i] == null ? o1.getMaxStackSize() : slotSpace[i];
                int take = Math.min(needed, space);
                needed -= take;
                slotSpace[i] = space - take;
                if (slotItem[i] == null) slotItem[i] = o1.getItem();
            }
            if (needed > 0) return false;
        }

        if (!o2.isEmpty()) {
            int needed = o2.getCount() * parallel;
            if (slotItem[1] != null && slotItem[1] != o2.getItem()) return false;
            int space = slotItem[1] == null ? o2.getMaxStackSize() : slotSpace[1];
            if (space < needed) return false;
        }

        if (!o3.isEmpty()) {
            int needed = o3.getCount() * parallel;
            if (slotItem[2] != null && slotItem[2] != o3.getItem()) return false;
            int space = slotItem[2] == null ? o3.getMaxStackSize() : slotSpace[2];
            if (space < needed) return false;
        }

        return true;
    }

    private void craftItem(PhaseSplitterRecipe recipe, int parallel) {
        ItemStack input = itemHandler.getStackInSlot(0).copy();
        input.shrink(recipe.getInputCount() * parallel);
        itemHandler.setStackInSlot(0, input.isEmpty() ? ItemStack.EMPTY : input);

        ItemStack o1 = recipe.getOutput1();
        ItemStack o2 = recipe.getOutput2();
        ItemStack o3 = recipe.getOutput3();

        if (!o1.isEmpty()) {
            int remaining = o1.getCount() * parallel;
            for (int i = 0; i < 3 && remaining > 0; i++) {
                int slot = i + 1;
                if (i == 1 && !o2.isEmpty()) break;
                if (i == 2 && !o3.isEmpty()) break;
                ItemStack cur = itemHandler.getStackInSlot(slot);
                if (!cur.isEmpty() && cur.getItem() != o1.getItem()) break;
                int maxStack = o1.getMaxStackSize();
                int curCount = cur.isEmpty() ? 0 : cur.getCount();
                int canFit   = maxStack - curCount;
                int toPlace  = Math.min(remaining, canFit);
                itemHandler.setStackInSlot(slot, new ItemStack(o1.getItem(), curCount + toPlace));
                remaining -= toPlace;
            }
        }

        addToOutput(2, o2, parallel);
        addToOutput(3, o3, parallel);

        progress = 0;
    }

    private void addToOutput(int slot, ItemStack result, int parallel) {
        if (result.isEmpty()) return;
        ItemStack cur = itemHandler.getStackInSlot(slot);
        int newCount = (cur.isEmpty() ? 0 : cur.getCount()) + result.getCount() * parallel;
        itemHandler.setStackInSlot(slot, new ItemStack(result.getItem(), newCount));
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
