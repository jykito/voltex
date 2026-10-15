package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.block.custom.BatteryChargerBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
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
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BatteryChargerBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

    private final MachineEnergy energyStorage = MachineEnergy.consumer(1000000, 50000);

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

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
            var cap = Caps.get(batteryStack, Caps.ENERGY);
            if (cap.isPresent()) {
                IEnergyStorage itemEnergy = cap.get();

                int maxAvailable = Math.min(energyStorage.getEnergyStored(), 50000);

                int maxTransfer = itemEnergy.receiveEnergy(maxAvailable, true);

                if (maxTransfer > 0) {

                    int accepted = itemEnergy.receiveEnergy(maxTransfer, false);

                    this.energyStorage.consumeEnergy(accepted);

                    isCharging = true;
                    setChanged();
                }
            }
        }

        MachineLit.tick(this, level, pos, state, BatteryChargerBlock.LIT, isCharging);
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
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK) return (T) energyStorage;
        if (cap == Capabilities.ItemHandler.BLOCK) return (T) itemHandler;
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
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        this.energyStorage.setEnergy(tag.getInt("energy"));
    }
}
