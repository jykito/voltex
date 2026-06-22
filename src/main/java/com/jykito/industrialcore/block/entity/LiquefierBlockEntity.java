package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.LiquefierBlock;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.menu.LiquefierMenu;
import com.jykito.industrialcore.recipe.LiquefierRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
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
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LiquefierBlockEntity extends BlockEntity implements MenuProvider {

    private final com.jykito.industrialcore.recipe.CachedRecipe<LiquefierRecipe> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(6) {
        @Override protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0, 1 -> !(stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem);
                case 2, 3, 4, 5 -> stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
                default -> false;
            };
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot >= 2) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    private static class LiquefierEnergy extends EnergyStorage {
        LiquefierEnergy(int capacity, int maxReceive) { super(capacity, maxReceive, 0); }

        void consumeEnergy(int amount) { this.energy = Math.max(0, this.energy - amount); }
        void setEnergy(int amount) { this.energy = Math.min(getMaxEnergyStored(), Math.max(0, amount)); }
    }
    private final LiquefierEnergy energyStorage = new LiquefierEnergy(50000, 2000);
    private LazyOptional<EnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    public final FluidTank outputTank = new FluidTank(8000) {
        @Override protected void onContentsChanged() { recipeCache.markDirty(); setChanged(); }
    };
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    private int heat = 0;
    private static final int MAX_HEAT = 10000;
    private boolean isReceivingHeat = false;

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 200;

    public void addHeat(int amount) {
        this.isReceivingHeat = true;
        if (this.heat < MAX_HEAT) {
            this.heat = Math.min(MAX_HEAT, this.heat + amount);
            setChanged();
        }
    }
    public int getHeat()    { return heat; }
    public int getMaxHeat() { return MAX_HEAT; }

    public LiquefierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIQUEFIER_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                    case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    case 6 -> heat;
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) {
                if (index == 0) progress = value;
                if (index == 1) maxProgress = value;
                if (index == 6) heat = value;
            }
            @Override
            public int getCount() { return 7; }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        int fluidBefore = outputTank.getFluidAmount();

        if (!isReceivingHeat && heat > 0) heat = Math.max(0, heat - 1);
        isReceivingHeat = false;

        float timeMultiplier   = 1.0f;
        float energyMultiplier = 1.0f;
        int parallel = 1;
        Direction fluidPushDir = null, itemPullDir = null;
        for (int s = 2; s <= 5; s++) {
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
                    if (dir.getMode() == com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem.Mode.FLUID_PUSH) fluidPushDir = d;
                    else if (dir.getMode() == com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem.Mode.ITEM_PULL) itemPullDir = d;
                }
            }
        }

        LiquefierRecipe recipe = findRecipe(level);
        this.maxProgress = recipe != null ? Math.max(1, (int)(recipe.getProcessingTime() * timeMultiplier)) : 200;

        boolean working = false;
        if (recipe != null
                && heat >= recipe.getRequiredHeat()
                && outputTank.getSpace() >= recipe.getFluidOutputAmount()) {

            int actualParallel = Math.max(1, Math.min(parallel, Math.min(
                    maxRunsForInputs(recipe),
                    outputTank.getSpace() / Math.max(1, recipe.getFluidOutputAmount()))));
            int cost = Math.max(1, (int)(recipe.getEnergyCostPerTick() * energyMultiplier));
            if (energyStorage.getEnergyStored() >= cost) {
                energyStorage.consumeEnergy(cost);
                progress++;
                working = true;
                if (progress >= maxProgress) {
                    craftItem(recipe, actualParallel);
                    progress = 0;
                }
            }
        } else {
            progress = 0;
        }

        if (state.getValue(LiquefierBlock.LIT) != working)
            level.setBlock(pos, state.setValue(LiquefierBlock.LIT, working), 3);

        if (fluidPushDir != null && !outputTank.isEmpty()) {
            final Direction d = fluidPushDir;
            BlockEntity n = level.getBlockEntity(pos.relative(d));
            if (n != null) n.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite()).ifPresent(dst ->
                    FluidUtil.tryFluidTransfer(dst, outputTank, outputTank.getFluidAmount(), true));
        }

        if (itemPullDir != null) {
            final Direction d = itemPullDir;
            BlockEntity n = level.getBlockEntity(pos.relative(d));
            if (n != null) n.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).ifPresent(src -> {
                for (int i = 0; i < src.getSlots(); i++) {
                    ItemStack sim = src.extractItem(i, 64, true);
                    if (sim.isEmpty()) continue;
                    for (int target : new int[]{0, 1}) {
                        int accepted = sim.getCount() - itemHandler.insertItem(target, sim, true).getCount();
                        if (accepted > 0) {
                            itemHandler.insertItem(target, src.extractItem(i, accepted, false), false);
                            return;
                        }
                    }
                }
            });
        }

        setChanged();
        if (outputTank.getFluidAmount() != fluidBefore)
            level.sendBlockUpdated(pos, state, state, 3);
    }

    private LiquefierRecipe findRecipe(Level level) {
        return recipeCache.get(() -> {
            ItemStack s0 = itemHandler.getStackInSlot(0);
            ItemStack s1 = itemHandler.getStackInSlot(1);
            for (LiquefierRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.LIQUEFYING_TYPE.get())) {
                if (!r.inputsMatch(s0, s1)) continue;
                FluidStack out = r.makeFluidOutput();
                if (out.isEmpty()) continue;
                if (!outputTank.isEmpty()
                        && !outputTank.getFluid().getFluid().isSame(out.getFluid())) continue;
                if (outputTank.getSpace() < out.getAmount()) continue;
                return r;
            }
            return null;
        });
    }

    private int maxRunsForInputs(LiquefierRecipe recipe) {
        ItemStack s0 = itemHandler.getStackInSlot(0);
        ItemStack s1 = itemHandler.getStackInSlot(1);
        if (recipe.hasSecondIngredient()) {
            int c1 = Math.max(1, recipe.getIngredientCount());
            int c2 = Math.max(1, recipe.getIngredientCount2());
            return recipe.inputPair(s0, s1)
                    ? Math.min(s0.getCount() / c1, s1.getCount() / c2)
                    : Math.min(s1.getCount() / c1, s0.getCount() / c2);
        }
        int c = Math.max(1, recipe.getIngredientCount());
        return (recipe.getIngredient().test(s0) && s0.getCount() >= recipe.getIngredientCount())
                ? s0.getCount() / c
                : s1.getCount() / c;
    }

    private void craftItem(LiquefierRecipe recipe, int parallel) {
        ItemStack s0 = itemHandler.getStackInSlot(0);
        ItemStack s1 = itemHandler.getStackInSlot(1);
        if (recipe.hasSecondIngredient()) {

            if (recipe.inputPair(s0, s1)) {
                itemHandler.extractItem(0, recipe.getIngredientCount()  * parallel, false);
                itemHandler.extractItem(1, recipe.getIngredientCount2() * parallel, false);
            } else {
                itemHandler.extractItem(1, recipe.getIngredientCount()  * parallel, false);
                itemHandler.extractItem(0, recipe.getIngredientCount2() * parallel, false);
            }
        } else {

            if (recipe.getIngredient().test(s0) && s0.getCount() >= recipe.getIngredientCount()) {
                itemHandler.extractItem(0, recipe.getIngredientCount() * parallel, false);
            } else {
                itemHandler.extractItem(1, recipe.getIngredientCount() * parallel, false);
            }
        }
        FluidStack out = recipe.makeFluidOutput();
        out.setAmount(out.getAmount() * parallel);
        outputTank.fill(out, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.liquefier");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new LiquefierMenu(id, inv, this, this.data);
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
        lazyFluidHandler  = LazyOptional.of(() -> outputTank);
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
        tag.put("outputTank", outputTank.writeToNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putInt("heat", heat);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        outputTank.readFromNBT(tag.getCompound("outputTank"));
        progress = tag.getInt("progress");
        heat = tag.getInt("heat");
    }

    public int getScaledFluid(int barHeight) {
        if (outputTank.getCapacity() == 0) return 0;
        return outputTank.getFluidAmount() * barHeight / outputTank.getCapacity();
    }

    public FluidStack getFluidInTank() { return outputTank.getFluid(); }
    public int getFluidAmount()        { return outputTank.getFluidAmount(); }
    public int getTankCapacity()       { return outputTank.getCapacity(); }
}
