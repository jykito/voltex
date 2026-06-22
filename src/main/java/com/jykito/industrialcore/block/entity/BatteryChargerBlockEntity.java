package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.BatteryChargerBlock;
import com.jykito.industrialcore.menu.BatteryChargerMenu;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BatteryChargerBlockEntity extends BlockEntity implements MenuProvider {

    private static class ChargerEnergyStorage extends EnergyStorage {
        public ChargerEnergyStorage(int capacity, int maxReceive) {
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

    private final ChargerEnergyStorage energyStorage = new ChargerEnergyStorage(1000000, 50000);
    private LazyOptional<EnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;

    public BatteryChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_CHARGER_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> BatteryChargerBlockEntity.this.energyStorage.getEnergyStored() & 0xFFFF;
                    case 1 -> (BatteryChargerBlockEntity.this.energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 2 -> BatteryChargerBlockEntity.this.energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 3 -> (BatteryChargerBlockEntity.this.energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) {}
            @Override
            public int getCount() { return 4; }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        boolean isCharging = false;

        ItemStack batteryStack = itemHandler.getStackInSlot(0);
        if (!batteryStack.isEmpty() && energyStorage.getEnergyStored() > 0) {
            var cap = batteryStack.getCapability(ForgeCapabilities.ENERGY);
            if (cap.isPresent()) {
                IEnergyStorage itemEnergy = cap.resolve().get();

                int maxAvailable = Math.min(energyStorage.getEnergyStored(), 50000);

                int maxTransfer = itemEnergy.receiveEnergy(maxAvailable, true);

                if (maxTransfer > 0) {

                    int accepted = itemEnergy.receiveEnergy(maxTransfer, false);

                    this.energyStorage.consumeInternal(accepted);

                    isCharging = true;
                    setChanged();
                }
            }
        }

        if (state.getValue(BatteryChargerBlock.LIT) != isCharging) {
            level.setBlock(pos, state.setValue(BatteryChargerBlock.LIT, isCharging), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.battery_charger");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BatteryChargerMenu(id, inventory, this, this.data);
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
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergyHandler.invalidate();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        this.energyStorage.setEnergy(tag.getInt("energy"));
    }
}
