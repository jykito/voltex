package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
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

public class PlasmaArmorItem extends ArmorItem {

    public static final int CAPACITY     = 1_000_000;
    public static final int MAX_TRANSFER = 1_000_000;
    public static final int FE_PER_DMG   = 100;
    public static final int BURST_MAX    = 500;

    public PlasmaArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        if (level.isClientSide()) return;
        boolean full = hasFullPlasmaSet(player);
        if (full) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else if (!player.isCreative() && !player.isSpectator()) {
            if (player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    private static boolean hasFullPlasmaSet(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(player.getItemBySlot(slot).getItem() instanceof PlasmaArmorItem)) return false;
        }
        return true;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        int layer = slot == EquipmentSlot.LEGS ? 2 : 1;
        return IndustrialCore.MODID + ":textures/models/armor/plasma_" + layer + ".png";
    }

    @Override
    public boolean isDamageable(ItemStack stack) { return false; }

    @Override
    public @NotNull ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<IEnergyStorage> lazy =
                    LazyOptional.of(() -> new PlasmaEnergy(stack));

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

    public static int getBurstCharge(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("BurstCharge") : 0;
    }

    public static void setBurstCharge(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt("BurstCharge", Math.max(0, Math.min(amount, BURST_MAX)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> lines, TooltipFlag flag) {
        int energy = getEnergy(stack);
        int burst  = getBurstCharge(stack);
        lines.add(Component.literal(String.format("%,d / %,d FE", energy, CAPACITY))
                .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE));
        lines.add(Component.literal(String.format("⚡ %d / %d", burst, BURST_MAX))
                .withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("tooltip.industrial_core.plasma_info")
                .withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * getEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) { return com.jykito.industrialcore.ModStyle.ENERGY; }

    public static class PlasmaEnergy implements IEnergyStorage {
        private final ItemStack stack;

        public PlasmaEnergy(ItemStack stack) { this.stack = stack; }

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

        @Override public int getEnergyStored()    { return PlasmaArmorItem.getEnergy(stack); }
        @Override public int getMaxEnergyStored()  { return CAPACITY; }
        @Override public boolean canExtract()      { return true; }
        @Override public boolean canReceive()      { return true; }
    }
}
