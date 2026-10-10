package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.ThermogeneratorBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
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

    private final MachineEnergy energyStorage = MachineEnergy.consumer(20000, 20000);
    private LazyOptional<EnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    private int currentGeneration = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() == com.jykito.industrialcore.item.ModItems.ELECTRIC_MOTOR.get()
                || stack.getItem() == com.jykito.industrialcore.item.ModItems.HEAT_CONDUCTOR.get()
                || stack.getItem() == com.jykito.industrialcore.item.ModItems.ADVANCED_HEAT_EXCHANGER.get()
                || stack.getItem() == com.jykito.industrialcore.item.ModItems.CRYOGENIC_HEAT_EXCHANGER.get();
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

    private final com.jykito.industrialcore.heat.IHeatHandler heatHandler = new com.jykito.industrialcore.heat.IHeatHandler() {
        @Override public int getHeat() { return heat; }
        @Override public int getMaxHeat() { return MAX_HEAT; }
        @Override public int receiveHeat(int amount, boolean simulate) { return 0; } // источник: сам греет, не принимает
    };
    private LazyOptional<com.jykito.industrialcore.heat.IHeatHandler> lazyHeat = LazyOptional.empty();

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
        lazyHeat = LazyOptional.of(() -> heatHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergyHandler.invalidate();
        lazyItemHandler.invalidate();
        lazyHeat.invalidate();
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

    /**
     * Смотрит ли генератор в термостанцию. От этого зависит криогенный теплообменник: он даёт
     * поток, который в одиночку девать некуда, поэтому работает только на станцию.
     * Проверяется цель, а не соседство — станция должна быть именно приёмником тепла.
     */
    private static boolean feedsThermostation(Level level, BlockPos pos, BlockState state) {
        BlockEntity target = level.getBlockEntity(pos.relative(state.getValue(ThermogeneratorBlock.FACING)));
        return target instanceof ThermostationBlockEntity;
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
        else if (upgradeSlot.getItem() == ModItems.CRYOGENIC_HEAT_EXCHANGER.get() && feedsThermostation(level, pos, state)) {
            // Криогенный работает ТОЛЬКО в связке со станцией: она буфер и насос, и без неё
            // такой поток тепла девать некуда. Одиночный термогенератор с ним работает как с пустым слотом.
            heatOutput = 60;
            energyCost = 4200;
        }

        if (energyStorage.getEnergyStored() >= energyCost) {
            energyStorage.consumeEnergy(energyCost);
            isWorking = true;

            Direction facing = state.getValue(ThermogeneratorBlock.FACING);
            BlockEntity be = level.getBlockEntity(pos.relative(facing));
            if (be != null) {
                com.jykito.industrialcore.heat.IHeatHandler heat =
                        be.getCapability(com.jykito.industrialcore.heat.HeatCapability.HEAT, facing.getOpposite()).resolve().orElse(null);
                if (heat != null) heat.receiveHeat(heatOutput, false);
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

        MachineLit.tick(this, level, pos, state, BlockStateProperties.LIT, isWorking);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergyHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        if (cap == com.jykito.industrialcore.heat.HeatCapability.HEAT
                && (side == null || side == getBlockState().getValue(ThermogeneratorBlock.FACING)))
            return lazyHeat.cast();
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
