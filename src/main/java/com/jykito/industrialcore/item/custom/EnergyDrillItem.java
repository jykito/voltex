package com.jykito.industrialcore.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class EnergyDrillItem extends DiggerItem {

    protected final int capacity;
    protected final float miningSpeed;

    public EnergyDrillItem(Tier tier, int capacity, float miningSpeed, Properties properties) {
        super(1.0f, -2.8f, tier, BlockTags.MINEABLE_WITH_PICKAXE, properties);
        this.capacity = capacity;
        this.miningSpeed = miningSpeed;
    }

    public static int getStoredEnergy(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("energy") : 0;
    }

    public static void setStoredEnergy(ItemStack stack, int energy) {
        stack.getOrCreateTag().putInt("energy", energy);
    }

    public static boolean drainEnergy(ItemStack stack, int amount) {
        int stored = getStoredEnergy(stack);
        if (stored < amount) return false;
        setStoredEnergy(stack, stored - amount);
        return true;
    }

    protected int getMinCost(ItemStack stack) { return 1; }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        boolean canMine = state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                       || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
        if (!canMine) return 1.0f;
        return getStoredEnergy(stack) >= getMinCost(stack) ? miningSpeed : 1.0f;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (getStoredEnergy(stack) < getMinCost(stack)) return false;
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return true;
        return super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean isDamageable(ItemStack stack) { return false; }

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * getStoredEnergy(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) { return com.jykito.industrialcore.ModStyle.ENERGY; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(getStoredEnergy(stack) + " / " + capacity + " FE")
                .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final EnergyStorage energyStorage = new EnergyStorage(capacity, capacity, capacity) {
                @Override
                public int getEnergyStored() {
                    return stack.hasTag() ? stack.getTag().getInt("energy") : 0;
                }

                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    int stored = getEnergyStored();
                    int received = Math.min(maxReceive, capacity - stored);
                    if (!simulate && received > 0) stack.getOrCreateTag().putInt("energy", stored + received);
                    return received;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    int stored = getEnergyStored();
                    int extracted = Math.min(maxExtract, stored);
                    if (!simulate && extracted > 0) stack.getOrCreateTag().putInt("energy", stored - extracted);
                    return extracted;
                }
            };

            private final LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> energyStorage);

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap,
                                                               @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? lazyEnergy.cast() : LazyOptional.empty();
            }
        };
    }
}
