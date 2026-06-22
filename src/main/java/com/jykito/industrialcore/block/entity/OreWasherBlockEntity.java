package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.OreWasherBlock;
import com.jykito.industrialcore.menu.OreWasherMenu;
import com.jykito.industrialcore.recipe.ModRecipes;
import com.jykito.industrialcore.recipe.OreWashingRecipe;
import com.jykito.industrialcore.item.upgrade.AcceleratorItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import com.jykito.industrialcore.item.upgrade.ParallelModuleItem;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OreWasherBlockEntity extends BlockEntity implements MenuProvider {

    private final com.jykito.industrialcore.recipe.CachedRecipe<OreWashingRecipe> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(8) {
        @Override protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {

            if (slot >= 4) return stack.getItem() instanceof MachineUpgradeItem;
            if (slot == 0) return !(stack.getItem() instanceof MachineUpgradeItem);
            return true;
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

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

    private static class WasherEnergy extends EnergyStorage {
        WasherEnergy(int capacity, int maxReceive) { super(capacity, maxReceive, 0); }
        void consumeEnergy(int amount) { this.energy = Math.max(0, this.energy - amount); }
        void setEnergy(int amount) { this.energy = Math.min(getMaxEnergyStored(), Math.max(0, amount)); }
    }
    private final WasherEnergy energyStorage = new WasherEnergy(64000, 1024);
    private LazyOptional<EnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    public final FluidTank waterTank = new FluidTank(128000, fs -> fs.getFluid() == Fluids.WATER) {
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
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

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

        Direction itemPusherDir = null, itemPullerDir = null, fluidPullerDir = null;
        float timeMultiplier = 1.0f, energyMultiplier = 1.0f;
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
                if (d != null) switch (dir.getMode()) {
                    case ITEM_PUSH  -> itemPusherDir = d;
                    case ITEM_PULL  -> itemPullerDir = d;
                    case FLUID_PULL -> fluidPullerDir = d;
                    default -> {  }
                }
            } else if (upgrade.getItem() instanceof ParallelModuleItem pm) {
                parallelCount = Math.max(parallelCount, pm.getParallelCount());
            } else if (upgrade.getItem() instanceof com.jykito.industrialcore.item.upgrade.AdvancedParallelModuleItem adv) {
                parallelCount = Math.max(parallelCount, adv.getParallelCount());
            }
        }

        if (fluidPullerDir != null && waterTank.getSpace() > 0) {
            final Direction d = fluidPullerDir;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(d));
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite()).ifPresent(nh -> {
                    FluidStack drained = nh.drain(new FluidStack(Fluids.WATER, Math.min(1000, waterTank.getSpace())), IFluidHandler.FluidAction.EXECUTE);
                    if (!drained.isEmpty()) waterTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                });
            }
        }

        if (itemPullerDir != null && itemHandler.getStackInSlot(0).isEmpty()) {
            final Direction d = itemPullerDir;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(d));
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).ifPresent(nh -> {
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

        OreWashingRecipe recipe = findRecipe(level);
        int baseTime   = recipe != null ? recipe.getProcessingTime()    : 200;
        int baseEnergy = recipe != null ? recipe.getEnergyCostPerTick() : 40;
        this.maxProgress = Math.max(1, (int) (baseTime * timeMultiplier));
        int effectiveEnergy = Math.max(1, (int) (baseEnergy * energyMultiplier));

        if (recipe != null) {
            if (energyStorage.getEnergyStored() >= effectiveEnergy) {
                energyStorage.consumeEnergy(effectiveEnergy);
                progress++;
                isWorking = true;
                if (progress >= maxProgress) {
                    craftItem(recipe, parallelCount);
                    progress = 0;
                }
            }
        } else {
            progress = 0;
        }

        if (itemPusherDir != null) {
            final Direction d = itemPusherDir;
            BlockEntity neighbor = level.getBlockEntity(pos.relative(d));
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).ifPresent(nh -> {
                    for (int outSlot : new int[]{1, 2, 3}) {
                        ItemStack output = itemHandler.getStackInSlot(outSlot);
                        if (output.isEmpty()) continue;
                        ItemStack remaining = ItemHandlerHelper.insertItem(nh, output.copy(), false);
                        if (remaining.getCount() < output.getCount()) {
                            itemHandler.setStackInSlot(outSlot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
                            setChanged();
                        }
                    }
                });
            }
        }

        if (state.getValue(OreWasherBlock.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(OreWasherBlock.LIT, isWorking), 3);
        }

        setChanged();
    }

    private OreWashingRecipe findRecipe(Level level) {
        return recipeCache.get(() -> {
            ItemStack in = itemHandler.getStackInSlot(0);
            if (in.isEmpty()) return null;
            for (OreWashingRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.ORE_WASHING_TYPE.get())) {
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

        int done = 0;
        for (int n = 0; n < Math.max(1, parallelCount); n++) {
            if (itemHandler.getStackInSlot(0).getCount() < recipe.getInputCount()) break;
            if (waterTank.getFluidAmount() < recipe.getWaterAmount()) break;
            if (!outputsFit(recipe)) break;

            itemHandler.extractItem(0, recipe.getInputCount(), false);
            waterTank.drain(recipe.getWaterAmount(), IFluidHandler.FluidAction.EXECUTE);

            if (recipe.getResultChance() >= 1.0f || level.random.nextFloat() < recipe.getResultChance()) {
                insertOutput(recipe.getResult());
            }
            if (recipe.hasByproduct() && level.random.nextFloat() < recipe.getByproductChance()) {
                insertOutput(recipe.getByproduct());
            }
            done++;
        }
        if (done > 0) setChanged();
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

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
        lazyFluidHandler  = LazyOptional.of(() -> fillOnlyHandler);
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
        tag.put("waterTank", waterTank.writeToNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        waterTank.readFromNBT(tag.getCompound("waterTank"));
        progress = tag.getInt("progress");
    }
}
