package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.CoalGeneratorBlock;
import com.jykito.industrialcore.menu.CoalGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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

public class CoalGeneratorBlockEntity extends BlockEntity implements MenuProvider {

    private static class CustomEnergyStorage extends EnergyStorage {
        public CustomEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }
        @Override
        public boolean canReceive() { return false; }
        public void addEnergy(int amount) { this.energy = Math.min(this.capacity, this.energy + amount); }
        public void setEnergy(int amount) { this.energy = amount; }
    }

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return itemHandler.insertItem(slot, stack, simulate); }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    private final CustomEnergyStorage energyStorage = new CustomEnergyStorage(50000, 0, 2000);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    private int burnProgress        = 0;
    private int currentMaxBurnTime  = 400;
    private int currentEnergyPerTick = 50;
    private static final float BURN_SCALE = 0.25f;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> burnProgress;
                case 5 -> currentMaxBurnTime;
                case 6 -> currentEnergyPerTick;
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {}
        @Override
        public int getCount() { return 7; }
    };

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COAL_GENERATOR_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.coal_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CoalGeneratorMenu(id, inventory, this, this.data);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        boolean hasChanges = false;

        if (burnProgress > 0) {
            burnProgress--;
            energyStorage.addEnergy(currentEnergyPerTick);
            hasChanges = true;
        }
        else if (burnProgress <= 0 && energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
            ItemStack stackInSlot = itemHandler.getStackInSlot(0);
            if (stackInSlot.getItem() == net.minecraft.world.item.Items.COAL_BLOCK) {
                itemHandler.extractItem(0, 1, false);
                currentMaxBurnTime   = 400;
                currentEnergyPerTick = 500;
                burnProgress         = currentMaxBurnTime;
                hasChanges = true;
            } else {
                int vanillaBurnTime = ForgeHooks.getBurnTime(stackInSlot, RecipeType.SMELTING);
                if (vanillaBurnTime > 0) {
                    itemHandler.extractItem(0, 1, false);
                    currentMaxBurnTime   = Math.max(1, (int)(vanillaBurnTime * BURN_SCALE));
                    currentEnergyPerTick = 50;
                    burnProgress         = currentMaxBurnTime;
                    hasChanges = true;
                }
            }
        }

        boolean isBurning = burnProgress > 0;
        if (state.getValue(CoalGeneratorBlock.LIT) != isBurning) {
            level.setBlock(pos, state.setValue(CoalGeneratorBlock.LIT, isBurning), 3);
        }

        if (distributeEnergy()) {
            hasChanges = true;
        }

        if (hasChanges) setChanged();
    }

    private boolean distributeEnergy() {
        if (energyStorage.getEnergyStored() <= 0) return false;

        boolean didPush = false;

        int energyToGive = Math.min(energyStorage.getEnergyStored(), 2000);

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor != null) {
                var cap = neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite());
                if (cap.isPresent()) {
                    IEnergyStorage neighborEnergy = cap.resolve().get();
                    if (neighborEnergy.canReceive()) {
                        int accepted = neighborEnergy.receiveEnergy(energyToGive, false);
                        if (accepted > 0) {
                            energyStorage.extractEnergy(accepted, false);
                            energyToGive -= accepted;
                            didPush = true;
                        }
                    }
                }
            }

            if (energyToGive <= 0) break;
        }
        return didPush;
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
        tag.putInt("burnProgress", burnProgress);
        tag.putInt("currentMaxBurnTime", currentMaxBurnTime);
        tag.putInt("currentEnergyPerTick", currentEnergyPerTick);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        burnProgress = tag.getInt("burnProgress");
        if (tag.contains("currentMaxBurnTime"))   currentMaxBurnTime   = tag.getInt("currentMaxBurnTime");
        if (tag.contains("currentEnergyPerTick")) currentEnergyPerTick = tag.getInt("currentEnergyPerTick");
    }
}
