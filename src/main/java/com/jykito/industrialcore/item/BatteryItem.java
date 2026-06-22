package com.jykito.industrialcore.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BatteryItem extends Item {
    private final int capacity;
    private final int maxTransfer;

    public BatteryItem(Properties properties, int capacity, int maxTransfer) {
        super(properties);
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {

            pTooltipComponents.add(Component.translatable("tooltip.industrial_core.battery_energy", energy.getEnergyStored(), energy.getMaxEnergyStored())
                    .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE));
        });

        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Override
    public boolean isBarVisible(ItemStack pStack) {
        return pStack.getCapability(ForgeCapabilities.ENERGY).isPresent();
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        return pStack.getCapability(ForgeCapabilities.ENERGY)
                .map(energy -> Math.round(13.0F * energy.getEnergyStored() / energy.getMaxEnergyStored()))
                .orElse(0);
    }

    @Override
    public int getBarColor(ItemStack pStack) {
        return com.jykito.industrialcore.ModStyle.ENERGY;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final EnergyStorage energyStorage = new EnergyStorage(capacity, maxTransfer, maxTransfer) {

                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    int stored = getEnergyStored();
                    int received = Math.min(Math.min(maxReceive, maxTransfer), capacity - stored);
                    if (!simulate && received > 0) stack.getOrCreateTag().putInt("energy", stored + received);
                    return received;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    int stored = getEnergyStored();
                    int extracted = Math.min(Math.min(maxExtract, maxTransfer), stored);
                    if (!simulate && extracted > 0) stack.getOrCreateTag().putInt("energy", stored - extracted);
                    return extracted;
                }

                @Override
                public int getEnergyStored() {
                    return stack.hasTag() ? stack.getTag().getInt("energy") : 0;
                }
            };

            private final LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> energyStorage);

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == ForgeCapabilities.ENERGY) {
                    return lazyEnergy.cast();
                }
                return LazyOptional.empty();
            }
        };
    }
}
