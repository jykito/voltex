package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NexiteArmorItem extends ArmorItem {

    public static final int CAPACITY     = 200_000;
    public static final int MAX_TRANSFER = 200_000;
    public static final int FE_PER_DMG   = 100;

    public NexiteArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        int layer = slot == EquipmentSlot.LEGS ? 2 : 1;
        return IndustrialCore.MODID + ":textures/models/armor/nexite_" + layer + ".png";
    }

    @Override
    public @NotNull ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<IEnergyStorage> lazy =
                    LazyOptional.of(() -> new NexiteEnergy(stack));

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? lazy.cast() : LazyOptional.empty();
            }
        };
    }

    public static int getEnergy(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("Energy") : 0;
    }

    public static void setEnergy(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt("Energy", Math.max(0, Math.min(amount, CAPACITY)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> lines, TooltipFlag flag) {
        int stored = getEnergy(stack);
        lines.add(Component.literal(String.format("%,d / %,d FE", stored, CAPACITY))
                .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE));
        lines.add(Component.translatable("tooltip.industrial_core.nexite_info")
                .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isDamageable(ItemStack stack) { return false; }

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * getEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) { return com.jykito.industrialcore.ModStyle.ENERGY; }

    public static class NexiteEnergy implements IEnergyStorage {
        private final ItemStack stack;

        public NexiteEnergy(ItemStack stack) { this.stack = stack; }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int stored   = getEnergyStored();
            int received = Math.min(MAX_TRANSFER, Math.min(maxReceive, CAPACITY - stored));
            if (!simulate && received > 0) setEnergy(stack, stored + received);
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int stored    = getEnergyStored();
            int extracted = Math.min(MAX_TRANSFER, Math.min(maxExtract, stored));
            if (!simulate && extracted > 0) setEnergy(stack, stored - extracted);
            return extracted;
        }

        @Override public int getEnergyStored()    { return NexiteArmorItem.getEnergy(stack); }
        @Override public int getMaxEnergyStored()  { return CAPACITY; }
        @Override public boolean canExtract()      { return true; }
        @Override public boolean canReceive()      { return true; }
    }
}
