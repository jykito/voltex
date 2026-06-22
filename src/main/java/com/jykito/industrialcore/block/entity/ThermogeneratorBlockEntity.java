package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.ThermogeneratorBlock;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.menu.ThermogeneratorMenu;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThermogeneratorBlockEntity extends BlockEntity implements MenuProvider {

    private static class ThermogenEnergyStorage extends EnergyStorage {
        public ThermogenEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0);
        }
        @Override
        public boolean canExtract() { return false; }

        public void consumeInternal(int amount) {
            this.energy = Math.max(0, this.energy - amount);
        }

        public void setEnergy(int amount) {
            this.energy = Math.max(0, Math.min(this.capacity, amount));
        }
    }

    private final ThermogenEnergyStorage energyStorage = new ThermogenEnergyStorage(20000, 20000);
    private LazyOptional<EnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    private int currentGeneration = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() == com.jykito.industrialcore.item.ModItems.ELECTRIC_MOTOR.get()
                || stack.getItem() == com.jykito.industrialcore.item.ModItems.HEAT_CONDUCTOR.get()
                || stack.getItem() == com.jykito.industrialcore.item.ModItems.ADVANCED_HEAT_EXCHANGER.get();
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    private int heat = 0;
    private final int MAX_HEAT = 3000;

    protected final ContainerData data;

    public ThermogeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMOGENERATOR_BE.get(), pos, state);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> ThermogeneratorBlockEntity.this.heat;
                    case 1 -> energyStorage.getEnergyStored() & 0xFFFF;
                    case 2 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 3 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 4 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    case 5 -> ThermogeneratorBlockEntity.this.currentGeneration;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> ThermogeneratorBlockEntity.this.heat = value;
                    case 5 -> ThermogeneratorBlockEntity.this.currentGeneration = value;
                }
            }

            @Override
            public int getCount() { return 6; }
        };
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        lazyItemHandler = LazyOptional.of(() -> automationHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergyHandler.invalidate();
        lazyItemHandler.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.thermogenerator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ThermogeneratorMenu(id, inventory, this, this.data);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        boolean isWorking = false;

        int heatOutput = 3;
        int energyCost = 200;

        ItemStack upgradeSlot = itemHandler.getStackInSlot(0);
        if (upgradeSlot.getItem() == ModItems.ELECTRIC_MOTOR.get()) {
            heatOutput = 5;
            energyCost = 400;
        }
        else if (upgradeSlot.getItem() == ModItems.HEAT_CONDUCTOR.get()) {
            heatOutput = 9;
            energyCost = 800;
        }
        else if (upgradeSlot.getItem() == ModItems.ADVANCED_HEAT_EXCHANGER.get()) {
            heatOutput = 17;
            energyCost = 2000;
        }

        if (energyStorage.getEnergyStored() >= energyCost) {
            energyStorage.consumeInternal(energyCost);
            isWorking = true;

            Direction facing = state.getValue(ThermogeneratorBlock.FACING);
            BlockEntity be = level.getBlockEntity(pos.relative(facing));
            if (be instanceof BlastFurnaceBlockEntity furnace) {
                furnace.addHeat(heatOutput);
            } else if (be instanceof LiquefierBlockEntity liquefier) {
                liquefier.addHeat(heatOutput);
            }
        }

        this.currentGeneration = isWorking ? heatOutput : 0;

        if (isWorking) {
            if (this.heat < MAX_HEAT) {
                this.heat = Math.min(MAX_HEAT, this.heat + 50);
            }
            setChanged();
        } else {
            if (this.heat > 0) {
                this.heat = Math.max(0, this.heat - 20);
                setChanged();
            }
        }

        if (state.getValue(BlockStateProperties.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, isWorking), 3);
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergyHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("heat", heat);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));

        energyStorage.setEnergy(tag.getInt("energy"));
        heat = tag.getInt("heat");
    }
}
