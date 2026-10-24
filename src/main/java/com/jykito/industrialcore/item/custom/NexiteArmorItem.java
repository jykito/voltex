package com.jykito.industrialcore.item.custom;
import net.minecraft.world.item.Item;
import com.jykito.industrialcore.item.EnergyItem;
import com.jykito.industrialcore.item.ModDataComponents;
import com.jykito.industrialcore.capability.Caps;

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
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NexiteArmorItem extends ArmorItem implements EnergyItem, com.jykito.industrialcore.api.PowerSuitArmor {

    public static final int CAPACITY     = 200_000;
    public static final int MAX_TRANSFER = 200_000;
    public static final int FE_PER_DMG   = 100;

    public NexiteArmorItem(net.minecraft.core.Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.durability(com.jykito.industrialcore.item.ModArmorMaterials.durability(material, type)));
    }

    /** One texture for all parts of its model (Nexite pattern, see PowerSuitModel). */
    @Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return net.minecraft.resources.ResourceLocation.parse(IndustrialCore.MODID + ":textures/models/armor/nexite_suit.png");
    }

    @Override
    public IEnergyStorage createEnergyStorage(ItemStack stack) {
        return new NexiteEnergy(stack);
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    public static void setEnergy(ItemStack stack, int amount) {
        stack.set(ModDataComponents.ENERGY.get(), Math.max(0, Math.min(amount, CAPACITY)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        int stored = getEnergy(stack);
        lines.add(Component.literal(String.format("%,d / %,d FE", stored, CAPACITY))
                .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE));
        lines.add(Component.translatable("tooltip.industrial_core.nexite_info")
                .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isDamageable(ItemStack stack) { return false; }

    /** 1.21 wears items by their components and no longer asks isDamageable: no wear at all, as in 1.20.1. */
    @Override
    public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity,
                                                                              java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        return 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return com.jykito.industrialcore.ModStyle.energyBar(getEnergy(stack), CAPACITY);
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
