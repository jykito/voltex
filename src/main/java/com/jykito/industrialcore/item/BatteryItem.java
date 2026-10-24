package com.jykito.industrialcore.item;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import com.jykito.industrialcore.item.EnergyItem;
import java.util.Optional;
import com.jykito.industrialcore.capability.Caps;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BatteryItem extends Item implements EnergyItem {
    private final int capacity;
    private final int maxTransfer;

    public BatteryItem(Properties properties, int capacity, int maxTransfer) {
        super(properties);
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext context, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        @Nullable Level pLevel = context.level();
        Caps.get(pStack, Caps.ENERGY).ifPresent(energy -> {

            pTooltipComponents.add(Component.translatable("tooltip.industrial_core.battery_energy", energy.getEnergyStored(), energy.getMaxEnergyStored())
                    .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE));
        });

        super.appendHoverText(pStack, context, pTooltipComponents, pIsAdvanced);
    }

    @Override
    public boolean isBarVisible(ItemStack pStack) {
        return Caps.get(pStack, Caps.ENERGY).isPresent();
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        return Caps.get(pStack, Caps.ENERGY)
                .map(energy -> com.jykito.industrialcore.ModStyle.energyBar(energy.getEnergyStored(), energy.getMaxEnergyStored()))
                .orElse(0);
    }

    @Override
    public int getBarColor(ItemStack pStack) {
        return com.jykito.industrialcore.ModStyle.ENERGY;
    }

    @Nullable
    @Override
    public IEnergyStorage createEnergyStorage(ItemStack stack) {
        return new ComponentEnergyStorage(stack, ModDataComponents.ENERGY.get(), capacity, maxTransfer, maxTransfer);
    }

}
