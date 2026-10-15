package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.item.custom.NaniteStrainItem;
import com.jykito.industrialcore.nanite.NaniteProcessing;
import com.jykito.industrialcore.block.custom.OreWasherBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
import com.jykito.industrialcore.menu.OreWasherMenu;
import com.jykito.industrialcore.recipe.ModRecipes;
import com.jykito.industrialcore.recipe.OreWashingRecipe;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OreWasherBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

    private final com.jykito.industrialcore.recipe.CachedRecipe<OreWashingRecipe> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(8) {
        @Override protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {

            if (slot >= 4) return UpgradeTargets.works(getBlockState().getBlock(), stack);
            if (slot == 0) return !(stack.getItem() instanceof MachineUpgradeItem) && !(stack.getItem() instanceof NaniteStrainItem);
            return true;
        }
    };

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot != 0) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 1 || slot > 3) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    private final MachineEnergy energyStorage = MachineEnergy.consumer(64000, 64000); // intake = buffer, no input cap

    // Base for running without a module: 8 washes. With parallel the tank grows by itself, see AdaptiveTank.
    private static final int TANK_BASE = 8000;

    public final FluidTank waterTank = new FluidTank(TANK_BASE, fs -> fs.getFluid() == Fluids.WATER) {
        @Override protected void onContentsChanged() {
            recipeCache.markDirty();
            setChanged();

            if (level != null && !level.isClientSide)
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };

    private final IFluidHandler fillOnlyHandler = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return waterTank.getFluid(); }
        @Override public int getTankCapacity(int tank) { return waterTank.getCapacity(); }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return waterTank.isFluidValid(stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return waterTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 200;

    public OreWasherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_WASHER_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> OreWasherBlockEntity.this.progress;
                    case 1 -> OreWasherBlockEntity.this.maxProgress;
                    case 2 -> OreWasherBlockEntity.this.energyStorage.getEnergyStored() & 0xFFFF;
                    case 3 -> (OreWasherBlockEntity.this.energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> OreWasherBlockEntity.this.energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 5 -> (OreWasherBlockEntity.this.energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) {
                if (index == 0) OreWasherBlockEntity.this.progress = value;
                if (index == 1) OreWasherBlockEntity.this.maxProgress = value;
            }
            @Override public int getCount() { return 6; }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        boolean isWorking = false;

        MachineUpgrades upg = MachineUpgrades.scan(itemHandler, 4, 7);

        if (AdaptiveTank.resize(waterTank, TANK_BASE, upg.parallel()))
            level.sendBlockUpdated(pos, state, state, 3); // the tank gauge is computed from capacity, the client needs the new one

        if (upg.fluidPull() != null && waterTank.getSpace() > 0) {
            MachineIO.neighbor(level, pos, upg.fluidPull(), Caps.FLUID_HANDLER).ifPresent(nh -> {
                FluidStack drained = nh.drain(new FluidStack(Fluids.WATER, Math.min(1000, waterTank.getSpace())), IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) waterTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            });
        }

        if (upg.itemPull() != null && MachineIO.pullItems(level, pos, upg.itemPull(), itemHandler, 0)) setChanged();

        OreWashingRecipe recipe = findRecipe(level);
        int baseTime   = recipe != null ? recipe.getProcessingTime()    : 200;
        int baseEnergy = recipe != null ? recipe.getEnergyCostPerTick() : 40;
        this.maxProgress = upg.scaleTime(baseTime);
        int effectiveEnergy = upg.scaleEnergy(baseEnergy);

        if (recipe != null) {
            if (energyStorage.getEnergyStored() >= effectiveEnergy) {
                energyStorage.consumeEnergy(effectiveEnergy);
                progress++;
                isWorking = true;
                if (progress >= maxProgress) {
                    craftItem(recipe, upg.parallel());
                    progress = 0;
                }
            }
        } else {
            progress = 0;
        }

        if (upg.itemPush() != null && MachineIO.pushItems(level, pos, upg.itemPush(), itemHandler, 1, 2, 3)) setChanged();

        MachineLit.tick(this, level, pos, state, OreWasherBlock.LIT, isWorking);

        setChanged();
    }

    private OreWashingRecipe findRecipe(Level level) {
        return recipeCache.get(() -> {
            ItemStack in = itemHandler.getStackInSlot(0);
            if (in.isEmpty()) return null;
            for (OreWashingRecipe r : com.jykito.industrialcore.recipe.RecipeLookup.all(level, ModRecipes.ORE_WASHING_TYPE.get())) {
                if (r.isLateOnly()) continue;   // industrial ore washer only
                if (!r.getInput().test(in) || in.getCount() < r.getInputCount()) continue;
                if (waterTank.getFluidAmount() < r.getWaterAmount()) continue;
                if (!outputsFit(r)) continue;
                return r;
            }
            return null;
        });
    }

    private boolean outputsFit(OreWashingRecipe r) {
        ItemStackHandler tmp = new ItemStackHandler(3);
        for (int i = 0; i < 3; i++) tmp.setStackInSlot(i, itemHandler.getStackInSlot(i + 1).copy());
        ItemStack p = r.getResult();
        for (int i = 0; i < 3 && !p.isEmpty(); i++) p = tmp.insertItem(i, p, false);
        if (!p.isEmpty()) return false;
        if (r.hasByproduct()) {
            ItemStack b = r.getByproduct();
            for (int i = 0; i < 3 && !b.isEmpty(); i++) b = tmp.insertItem(i, b, false);
            if (!b.isEmpty()) return false;
        }
        return true;
    }

    private void insertOutput(ItemStack stack) {
        for (int s = 1; s <= 3 && !stack.isEmpty(); s++) stack = itemHandler.insertItem(s, stack, false);
    }

    private void craftItem(OreWashingRecipe recipe, int parallelCount) {

        int done = 0, nanite = naniteSlot();
        ItemStack strain = nanite < 0 ? ItemStack.EMPTY : itemHandler.getStackInSlot(nanite);
        for (int n = 0; n < Math.max(1, parallelCount); n++) {
            if (itemHandler.getStackInSlot(0).getCount() < recipe.getInputCount()) break;
            if (waterTank.getFluidAmount() < recipe.getWaterAmount()) break;
            if (!outputsFit(recipe)) break;

            net.minecraft.world.item.Item input = itemHandler.getStackInSlot(0).getItem();
            itemHandler.extractItem(0, recipe.getInputCount(), false);
            waterTank.drain(recipe.getWaterAmount(), IFluidHandler.FluidAction.EXECUTE);

            if (recipe.getResultChance() >= 1.0f || level.random.nextFloat() < recipe.getResultChance()) {
                insertOutput(recipe.getResult());
                // processor: a second main product if there is room for it (the extra is not lost, just not given out)
                if (NaniteProcessing.extra(level, strain, input, 1) > 0 && fits(recipe.getResult())) insertOutput(recipe.getResult());
            }
            if (recipe.hasByproduct() && level.random.nextFloat() < recipe.getByproductChance()) {
                insertOutput(recipe.getByproduct());
            }
            done++;
        }
        if (nanite >= 0 && done > 0) itemHandler.setStackInSlot(nanite, NaniteProcessing.wear(level, strain, done));
        if (done > 0) setChanged();
    }

    /** Upgrade slot with a processor strain, or −1. */
    private int naniteSlot() {
        for (int s = 4; s <= 7; s++) if (itemHandler.getStackInSlot(s).getItem() instanceof NaniteStrainItem) return s;
        return -1;
    }

    private boolean fits(ItemStack stack) {
        ItemStack p = stack.copy();
        for (int s = 1; s <= 3 && !p.isEmpty(); s++) p = itemHandler.insertItem(s, p, true);
        return p.isEmpty();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.ore_washer"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new OreWasherMenu(id, inventory, this, this.data);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK)        return (T) energyStorage;
        if (cap == Capabilities.ItemHandler.BLOCK)  return (T) automationHandler;
        if (cap == Capabilities.FluidHandler.BLOCK) return (T) fillOnlyHandler;
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
        tag.put("waterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        AdaptiveTank.save(tag, "waterTankCapacity", waterTank);
        tag.putInt("progress", progress);
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        AdaptiveTank.load(tag, "waterTankCapacity", waterTank, TANK_BASE);
        waterTank.readFromNBT(registries, tag.getCompound("waterTank"));
        progress = tag.getInt("progress");
    }
}
