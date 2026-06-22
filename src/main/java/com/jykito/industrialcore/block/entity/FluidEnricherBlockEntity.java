package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.FluidEnricherBlock;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.recipe.FluidEnricherRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
import com.jykito.industrialcore.item.upgrade.AcceleratorItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.menu.FluidEnricherMenu;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidEnricherBlockEntity extends BlockEntity implements MenuProvider {

    private final com.jykito.industrialcore.recipe.CachedRecipe<FluidEnricherRecipe> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
                case 4 -> stack.getItem() == ModItems.UNIVERSAL_CAPSULE.get();
                case 6, 7, 8, 9 -> stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
                default -> super.isItemValid(slot, stack);
            };
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot >= 6) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 1 && slot != 5) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    private static class EnricherEnergy extends EnergyStorage {
        EnricherEnergy(int capacity, int maxReceive) { super(capacity, maxReceive, 0); }
        void consumeEnergy(int amount) { this.energy = Math.max(0, this.energy - amount); }
        void setEnergy(int amount) { this.energy = Math.min(getMaxEnergyStored(), Math.max(0, amount)); }
    }
    private final EnricherEnergy energyStorage = new EnricherEnergy(50000, 1000);
    private LazyOptional<EnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    private final IFluidHandler combinedFluidHandler = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? inputTank.getFluid() : outputTank.getFluid();
        }
        @Override public int getTankCapacity(int tank) {
            return tank == 0 ? inputTank.getCapacity() : outputTank.getCapacity();
        }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 ? inputTank.isFluidValid(stack) : outputTank.isFluidValid(stack);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            return inputTank.fill(resource, action);
        }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return outputTank.drain(resource, action);
        }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return outputTank.drain(maxDrain, action);
        }
    };
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    public final FluidTank inputTank = new FluidTank(8000) {
        @Override
        protected void onContentsChanged() { recipeCache.markDirty(); setChanged(); }
    };
    public final FluidTank outputTank = new FluidTank(8000) {
        @Override
        protected void onContentsChanged() { recipeCache.markDirty(); setChanged(); }
    };

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 100;

    public FluidEnricherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_ENRICHER_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> FluidEnricherBlockEntity.this.progress;
                    case 1 -> FluidEnricherBlockEntity.this.maxProgress;
                    case 2 -> FluidEnricherBlockEntity.this.energyStorage.getEnergyStored() & 0xFFFF;
                    case 3 -> (FluidEnricherBlockEntity.this.energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> FluidEnricherBlockEntity.this.energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 5 -> (FluidEnricherBlockEntity.this.energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) {
                if (index == 0) FluidEnricherBlockEntity.this.progress = value;
                if (index == 1) FluidEnricherBlockEntity.this.maxProgress = value;
            }
            @Override
            public int getCount() { return 6; }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        boolean isWorking = false;
        int inFluidBefore  = inputTank.getFluidAmount();
        int outFluidBefore = outputTank.getFluidAmount();

        Direction fluidPusherDir = null;
        Direction fluidPullerDir = null;
        Direction itemPusherDir  = null;
        Direction itemPullerDir  = null;
        float timeMultiplier   = 1.0f;
        float energyMultiplier = 1.0f;
        int parallel = 1;

        for (int s = 6; s <= 9; s++) {
            ItemStack upgrade = itemHandler.getStackInSlot(s);
            if (upgrade.isEmpty()) continue;
            if (upgrade.getItem() instanceof DirectionalUpgradeItem dir) {
                Direction d = DirectionalUpgradeItem.getStoredDirection(upgrade);
                if (d != null) switch (dir.getMode()) {
                    case FLUID_PUSH -> fluidPusherDir = d;
                    case FLUID_PULL -> fluidPullerDir = d;
                    case ITEM_PUSH  -> itemPusherDir  = d;
                    case ITEM_PULL  -> itemPullerDir  = d;
                }
            } else if (upgrade.getItem() instanceof AcceleratorItem acc) {
                for (int c = 0; c < upgrade.getCount(); c++) {
                    timeMultiplier   *= acc.getSpeedMultiplier();
                    energyMultiplier *= acc.getEnergyMultiplier();
                }
            } else if (upgrade.getItem() instanceof com.jykito.industrialcore.item.upgrade.ParallelModuleItem pm) {
                parallel = Math.max(parallel, pm.getParallelCount());
            } else if (upgrade.getItem() instanceof com.jykito.industrialcore.item.upgrade.AdvancedParallelModuleItem adv) {
                parallel = Math.max(parallel, adv.getParallelCount());
            }
        }

        FluidEnricherRecipe currentRecipe = findRecipe(level);
        int baseTime   = currentRecipe != null ? currentRecipe.getProcessingTime()    : 100;
        int baseEnergy = currentRecipe != null ? currentRecipe.getEnergyCostPerTick() : 50;
        this.maxProgress = Math.max(1, (int)(baseTime   * timeMultiplier));
        int effectiveEnergy = Math.max(1, (int)(baseEnergy * energyMultiplier));

        if (itemPullerDir != null) {
            final Direction pullDir = itemPullerDir;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(pullDir));
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, pullDir.getOpposite())
                        .ifPresent(nh -> {
                            outer:
                            for (int i = 0; i < nh.getSlots(); i++) {
                                ItemStack extracted = nh.extractItem(i, 64, true);
                                if (extracted.isEmpty()) continue;
                                for (int target : new int[]{2, 3, 4, 0}) {
                                    ItemStack remaining = itemHandler.insertItem(target, extracted.copy(), true);
                                    int inserted = extracted.getCount() - remaining.getCount();
                                    if (inserted > 0) {
                                        itemHandler.insertItem(target, nh.extractItem(i, inserted, false), false);
                                        setChanged();
                                        break outer;
                                    }
                                }
                            }
                        });
            }
        }

        if (fluidPullerDir != null && inputTank.getSpace() > 0) {
            final Direction pullDir = fluidPullerDir;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(pullDir));
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, pullDir.getOpposite())
                        .ifPresent(nh -> {

                            FluidStack sim = nh.drain(Math.min(1000, inputTank.getSpace()), IFluidHandler.FluidAction.SIMULATE);
                            if (sim.isEmpty()) return;
                            int canFill = inputTank.fill(sim, IFluidHandler.FluidAction.SIMULATE);
                            if (canFill <= 0) return;
                            inputTank.fill(nh.drain(canFill, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                        });
            }
        }

        transferFluidFromItem(0, 1, inputTank);

        transferFluidToItem(4, 5, outputTank);

        if (currentRecipe != null) {
            if (energyStorage.getEnergyStored() >= effectiveEnergy) {
                energyStorage.consumeEnergy(effectiveEnergy);
                progress++;
                isWorking = true;
                if (progress >= maxProgress) {
                    int actualParallel = Math.max(1, Math.min(parallel, maxRunsForInputs(currentRecipe)));
                    craftItem(currentRecipe, actualParallel);
                    progress = 0;
                }
            }
        } else {
            progress = 0;
        }

        if (itemPusherDir != null) {
            final Direction pushDir = itemPusherDir;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(pushDir));
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, pushDir.getOpposite())
                        .ifPresent(nh -> {
                            for (int outputSlot : new int[]{1, 5}) {
                                ItemStack output = itemHandler.getStackInSlot(outputSlot);
                                if (output.isEmpty()) continue;
                                ItemStack remaining = ItemHandlerHelper.insertItem(nh, output.copy(), false);
                                if (remaining.getCount() < output.getCount()) {
                                    itemHandler.setStackInSlot(outputSlot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
                                    setChanged();
                                }
                            }
                        });
            }
        }

        if (fluidPusherDir != null && !outputTank.isEmpty()) {
            final Direction pushDir = fluidPusherDir;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(pushDir));
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, pushDir.getOpposite())
                        .ifPresent(nh -> {
                            int toTransfer = Math.min(outputTank.getFluidAmount(), 1000);
                            FluidStack toSend = new FluidStack(outputTank.getFluid().getFluid(), toTransfer);
                            int accepted = nh.fill(toSend, IFluidHandler.FluidAction.EXECUTE);
                            if (accepted > 0) outputTank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                        });
            }
        }

        if (state.getValue(FluidEnricherBlock.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(FluidEnricherBlock.LIT, isWorking), 3);
        }

        setChanged();
        if (inputTank.getFluidAmount() != inFluidBefore || outputTank.getFluidAmount() != outFluidBefore)
            level.sendBlockUpdated(pos, state, state, 3);
    }

    private FluidEnricherRecipe findRecipe(Level level) {
        return recipeCache.get(() -> {
        for (FluidEnricherRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.FLUID_ENRICHER_TYPE.get())) {

            if (!r.reagentsMatch(itemHandler.getStackInSlot(2), itemHandler.getStackInSlot(3))) continue;

            FluidStack needed = r.makeFluidInput();
            if (needed.isEmpty()) continue;
            if (!inputTank.getFluid().getFluid().isSame(needed.getFluid())
                    || inputTank.getFluidAmount() < needed.getAmount()) continue;

            FluidStack out = r.makeFluidOutput();
            if (!out.isEmpty()) {
                if (!outputTank.isEmpty()
                        && (!outputTank.getFluid().getFluid().isSame(out.getFluid())
                            || outputTank.getSpace() < out.getAmount())) continue;
            }
            return r;
        }
        return null;
        });
    }

    private int maxRunsForInputs(FluidEnricherRecipe recipe) {
        int runs = Integer.MAX_VALUE;
        if (recipe.getItemCount() > 0) {
            ItemStack s2 = itemHandler.getStackInSlot(2);
            ItemStack s3 = itemHandler.getStackInSlot(3);
            if (recipe.hasSecondReagent()) {
                int c1 = Math.max(1, recipe.getItemCount());
                int c2 = Math.max(1, recipe.getItemCount2());
                runs = recipe.reagentPair(s2, s3)
                        ? Math.min(s2.getCount() / c1, s3.getCount() / c2)
                        : Math.min(s3.getCount() / c1, s2.getCount() / c2);
            } else {
                int c = Math.max(1, recipe.getItemCount());
                runs = (recipe.getItemIngredient().test(s2) && s2.getCount() >= recipe.getItemCount())
                        ? s2.getCount() / c : s3.getCount() / c;
            }
        }
        FluidStack fin = recipe.makeFluidInput();
        if (!fin.isEmpty()) runs = Math.min(runs, inputTank.getFluidAmount() / Math.max(1, fin.getAmount()));
        FluidStack fout = recipe.makeFluidOutput();
        if (!fout.isEmpty()) runs = Math.min(runs, outputTank.getSpace() / Math.max(1, fout.getAmount()));
        return runs;
    }

    private void craftItem(FluidEnricherRecipe recipe, int parallel) {
        ItemStack s2 = itemHandler.getStackInSlot(2);
        ItemStack s3 = itemHandler.getStackInSlot(3);

        if (recipe.hasSecondReagent()) {

            if (recipe.reagentPair(s2, s3)) {
                itemHandler.extractItem(2, recipe.getItemCount()  * parallel, false);
                itemHandler.extractItem(3, recipe.getItemCount2() * parallel, false);
            } else {
                itemHandler.extractItem(3, recipe.getItemCount()  * parallel, false);
                itemHandler.extractItem(2, recipe.getItemCount2() * parallel, false);
            }
        } else {

            int count = recipe.getItemCount();
            if (recipe.getItemIngredient().test(s2) && s2.getCount() >= count) {
                itemHandler.extractItem(2, count * parallel, false);
            } else {
                itemHandler.extractItem(3, count * parallel, false);
            }
        }

        FluidStack fluidIn = recipe.makeFluidInput();
        if (!fluidIn.isEmpty()) inputTank.drain(fluidIn.getAmount() * parallel, IFluidHandler.FluidAction.EXECUTE);

        FluidStack fluidOut = recipe.makeFluidOutput();
        if (!fluidOut.isEmpty()) {
            fluidOut.setAmount(fluidOut.getAmount() * parallel);
            outputTank.fill(fluidOut, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void transferFluidFromItem(int inputSlot, int outputSlot, FluidTank tank) {
        ItemStack inputStack = itemHandler.getStackInSlot(inputSlot);
        if (inputStack.isEmpty()) return;

        ItemStack singleItem = inputStack.copyWithCount(1);
        singleItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(itemFluidHandler -> {
            FluidStack fluidInItem = itemFluidHandler.getFluidInTank(0);
            if (!fluidInItem.isEmpty()) {
                int filled = tank.fill(fluidInItem, IFluidHandler.FluidAction.SIMULATE);
                if (filled > 0) {
                    tank.fill(itemFluidHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    ItemStack containerItem = itemFluidHandler.getContainer();

                    if (itemHandler.insertItem(outputSlot, containerItem, true).isEmpty()) {
                        itemHandler.insertItem(outputSlot, containerItem, false);
                        itemHandler.extractItem(inputSlot, 1, false);
                    }
                }
            }
        });
    }

    private void transferFluidToItem(int inputSlot, int outputSlot, FluidTank tank) {
        ItemStack emptyStack = itemHandler.getStackInSlot(inputSlot);
        if (emptyStack.isEmpty() || tank.isEmpty()) return;

        ItemStack singleItem = emptyStack.copyWithCount(1);
        singleItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(itemFluidHandler -> {
            int filled = itemFluidHandler.fill(tank.getFluid(), IFluidHandler.FluidAction.SIMULATE);
            if (filled > 0) {
                itemFluidHandler.fill(tank.getFluid(), IFluidHandler.FluidAction.EXECUTE);
                ItemStack filledContainer = itemFluidHandler.getContainer();

                if (itemHandler.insertItem(outputSlot, filledContainer, true).isEmpty()) {
                    tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    itemHandler.extractItem(inputSlot, 1, false);
                    itemHandler.insertItem(outputSlot, filledContainer, false);
                }
            }
        });
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.fluid_enricher"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FluidEnricherMenu(id, inventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY)        return lazyEnergyHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER)  return lazyItemHandler.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        lazyItemHandler   = LazyOptional.of(() -> automationHandler);
        lazyFluidHandler  = LazyOptional.of(() -> combinedFluidHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergyHandler.invalidate();
        lazyItemHandler.invalidate();
        lazyFluidHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("inputTank", inputTank.writeToNBT(new CompoundTag()));
        tag.put("outputTank", outputTank.writeToNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        inputTank.readFromNBT(tag.getCompound("inputTank"));
        outputTank.readFromNBT(tag.getCompound("outputTank"));
        progress = tag.getInt("progress");
    }
}
