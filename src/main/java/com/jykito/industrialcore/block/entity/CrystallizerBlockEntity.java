package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.CrystallizerBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
import com.jykito.industrialcore.menu.CrystallizerMenu;
import com.jykito.industrialcore.recipe.CachedRecipe;
import com.jykito.industrialcore.recipe.CrystallizerRecipe;
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
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Crystallizer: liquid in, item out.
 * <p>
 * There are no capsule slots on purpose: the tank fills directly. So that this does not require
 * putting an upgrade into another machine, the crystallizer can pull fluid by itself: a directional
 * {@code FLUID_PULL} upgrade in its own slots pumps from a neighbour, and all the setup
 * stays in one GUI.
 */
public class CrystallizerBlockEntity extends BlockEntity implements MenuProvider {

    /** 0 seed, 1 output, 2..5 upgrades. */
    private static final int SLOT_SEED = 0, SLOT_OUTPUT = 1, SLOT_UPGRADE_FIRST = 2, SLOT_UPGRADE_LAST = 5;
    private static final int TANK_BASE = 8000;
    private static final int ENERGY_CAPACITY = 50000;

    private final CachedRecipe<CrystallizerRecipe> recipeCache = new CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(6) {
        @Override protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_SEED, SLOT_OUTPUT -> !(stack.getItem() instanceof MachineUpgradeItem);
                default -> UpgradeTargets.works(getBlockState().getBlock(), stack);
            };
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    /** From outside: only the seed can be inserted, only the result extracted. */
    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot != SLOT_SEED) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != SLOT_OUTPUT) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    private final MachineEnergy energyStorage = MachineEnergy.consumer(ENERGY_CAPACITY, ENERGY_CAPACITY);
    private LazyOptional<EnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    public final FluidTank inputTank = new FluidTank(TANK_BASE) {
        @Override protected void onContentsChanged() {
            recipeCache.markDirty();
            setChanged();
            if (level != null && !level.isClientSide())
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 200;

    public CrystallizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTALLIZER_BE.get(), pos, state);
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
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) {
                if (index == 0) progress = value;
                if (index == 1) maxProgress = value;
            }
            @Override
            public int getCount() { return 6; }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        MachineUpgrades upg = MachineUpgrades.scan(itemHandler, SLOT_UPGRADE_FIRST, SLOT_UPGRADE_LAST);
        // There is no fluid pusher on purpose: the tank only receives, there is nothing to drain from the machine.

        if (AdaptiveTank.resize(inputTank, TANK_BASE, upg.parallel())) {
            setChanged();
            level.sendBlockUpdated(pos, state, state, 3); // the tank gauge is computed from capacity, the client needs the new one
        }

        if (upg.fluidPull() != null) MachineIO.pullFluid(level, pos, upg.fluidPull(), inputTank);
        if (upg.itemPull()  != null) MachineIO.pullItems(level, pos, upg.itemPull(), itemHandler, SLOT_SEED);

        CrystallizerRecipe recipe = findRecipe(level);
        this.maxProgress = recipe != null ? upg.scaleTime(recipe.getProcessingTime()) : 200;

        boolean working = false;
        if (recipe != null) {
            int actualParallel = Math.max(1, Math.min(upg.parallel(), maxRuns(recipe)));
            int cost = upg.scaleEnergy(recipe.getEnergyCostPerTick());
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

        MachineLit.tick(this, level, pos, state, CrystallizerBlock.LIT, working);

        if (upg.itemPush() != null) MachineIO.pushItems(level, pos, upg.itemPush(), itemHandler, SLOT_OUTPUT);

        setChanged();
    }

    @Nullable
    private CrystallizerRecipe findRecipe(Level level) {
        return recipeCache.get(() -> {
            ItemStack seed = itemHandler.getStackInSlot(SLOT_SEED);
            for (CrystallizerRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.CRYSTALLIZING_TYPE.get())) {
                if (!r.seedMatches(seed)) continue;

                FluidStack need = r.makeFluidInput();
                if (need.isEmpty()) continue;
                if (!inputTank.getFluid().isFluidEqual(need) || inputTank.getFluidAmount() < need.getAmount()) continue;

                if (!fitsOutput(r.getResult(), 1)) continue;
                return r;
            }
            return null;
        });
    }

    /** Whether this much output fits into the slot. */
    private boolean fitsOutput(ItemStack result, int parallel) {
        ItemStack out = itemHandler.getStackInSlot(SLOT_OUTPUT);
        int adding = result.getCount() * parallel;
        if (out.isEmpty()) return adding <= result.getMaxStackSize();
        if (!ItemStack.isSameItemSameTags(out, result)) return false;
        return out.getCount() + adding <= out.getMaxStackSize();
    }

    /** How many times the recipe can run at once: by fluid, by seed and by room in the output. */
    private int maxRuns(CrystallizerRecipe recipe) {
        int runs = inputTank.getFluidAmount() / Math.max(1, recipe.getFluidInputAmount());
        if (recipe.hasSeed())
            runs = Math.min(runs, itemHandler.getStackInSlot(SLOT_SEED).getCount() / Math.max(1, recipe.getIngredientCount()));

        ItemStack result = recipe.getResult();
        ItemStack out    = itemHandler.getStackInSlot(SLOT_OUTPUT);
        int free = (out.isEmpty() ? result.getMaxStackSize() : out.getMaxStackSize() - out.getCount());
        runs = Math.min(runs, free / Math.max(1, result.getCount()));
        return Math.max(1, runs);
    }

    private void craftItem(CrystallizerRecipe recipe, int parallel) {
        inputTank.drain(recipe.getFluidInputAmount() * parallel, IFluidHandler.FluidAction.EXECUTE);
        if (recipe.hasSeed())
            itemHandler.extractItem(SLOT_SEED, recipe.getIngredientCount() * parallel, false);

        ItemStack result = recipe.getResult();
        itemHandler.insertItem(SLOT_OUTPUT, result.copyWithCount(result.getCount() * parallel), false);
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.crystallizer"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CrystallizerMenu(id, inv, this, this.data);
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
        lazyFluidHandler  = LazyOptional.of(() -> inputTank);
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
        AdaptiveTank.save(tag, "tankCapacity", inputTank);
        tag.putInt("progress", progress);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        AdaptiveTank.load(tag, "tankCapacity", inputTank, TANK_BASE);
        inputTank.readFromNBT(tag.getCompound("inputTank"));
        progress = tag.getInt("progress");
    }

    public FluidStack getFluidInTank() { return inputTank.getFluid(); }
    public int getFluidAmount()        { return inputTank.getFluidAmount(); }
    public int getTankCapacity()       { return inputTank.getCapacity(); }
}
