package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.block.custom.LiquefierBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
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
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LiquefierBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

    private final com.jykito.industrialcore.recipe.CachedRecipe<LiquefierRecipe> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(6) {
        @Override protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0, 1 -> !(stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem);
                case 2, 3, 4, 5 -> UpgradeTargets.works(getBlockState().getBlock(), stack);
                default -> false;
            };
        }
    };

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

    private final MachineEnergy energyStorage = MachineEnergy.consumer(50000, 50000); // intake = buffer, no input cap

    private static final int TANK_BASE = 8000;

    public final FluidTank outputTank = new FluidTank(TANK_BASE) {
        @Override protected void onContentsChanged() {
            recipeCache.markDirty();
            setChanged();
            if (level != null && !level.isClientSide())
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };

    private int heat = 0;
    private int litGrace = 0; // anti-flicker for LIT (grace via MachineLit)
    private static final int MAX_HEAT = 10000;
    private boolean isReceivingHeat = false;

    private final com.jykito.industrialcore.heat.IHeatHandler heatHandler = new com.jykito.industrialcore.heat.IHeatHandler() {
        @Override public int getHeat() { return heat; }
        @Override public int getMaxHeat() { return MAX_HEAT; }
        @Override public int receiveHeat(int amount, boolean simulate) {
            int accepted = Math.min(Math.max(0, amount), MAX_HEAT - heat);
            if (!simulate && accepted > 0) addHeat(accepted);
            return accepted;
        }
    };

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

        MachineUpgrades upg = MachineUpgrades.scan(itemHandler, 2, 5);

        if (AdaptiveTank.resize(outputTank, TANK_BASE, upg.parallel()))
            level.sendBlockUpdated(pos, state, state, 3); // the tank gauge is computed from capacity, the client needs the new one

        LiquefierRecipe recipe = findRecipe(level);
        this.maxProgress = recipe != null ? upg.scaleTime(recipe.getProcessingTime()) : 200;

        boolean working = false;
        if (recipe != null
                && heat >= recipe.getRequiredHeat()
                && outputTank.getSpace() >= recipe.getFluidOutputAmount()) {

            int actualParallel = Math.max(1, Math.min(upg.parallel(), Math.min(
                    maxRunsForInputs(recipe),
                    outputTank.getSpace() / Math.max(1, recipe.getFluidOutputAmount()))));
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

        boolean wasLit = state.getValue(LiquefierBlock.LIT);
        if (working) litGrace = MachineLit.GRACE; else if (litGrace > 0) litGrace--;
        boolean wantLit = litGrace > 0;
        if (wasLit != wantLit) {
            BlockState ns = state.setValue(LiquefierBlock.LIT, wantLit);
            if (wantLit) // new work session (with grace): a new random face animation
                ns = ns.setValue(LiquefierBlock.VARIANT, level.random.nextInt(LiquefierBlock.VARIANTS));
            level.setBlock(pos, ns, 3);
        }

        if (upg.fluidPush() != null) MachineIO.pushFluid(level, pos, upg.fluidPush(), outputTank);

        if (upg.itemPull() != null) MachineIO.pullItems(level, pos, upg.itemPull(), itemHandler, 0, 1);

        setChanged();
    }

    private LiquefierRecipe findRecipe(Level level) {
        return recipeCache.get(() -> {
            ItemStack s0 = itemHandler.getStackInSlot(0);
            ItemStack s1 = itemHandler.getStackInSlot(1);
            for (LiquefierRecipe r : com.jykito.industrialcore.recipe.RecipeLookup.all(level, ModRecipes.LIQUEFYING_TYPE.get())) {
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

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

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
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK)        return (T) energyStorage;
        if (cap == Capabilities.ItemHandler.BLOCK)  return (T) automationHandler;
        if (cap == Capabilities.FluidHandler.BLOCK) return (T) outputTank;
        if (cap == com.jykito.industrialcore.heat.HeatCapability.HEAT) return (T) heatHandler;
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
        tag.put("outputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        AdaptiveTank.save(tag, "outputTankCapacity", outputTank);
        tag.putInt("progress", progress);
        tag.putInt("heat", heat);
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        AdaptiveTank.load(tag, "outputTankCapacity", outputTank, TANK_BASE);
        outputTank.readFromNBT(registries, tag.getCompound("outputTank"));
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
