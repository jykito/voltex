package com.jykito.industrialcore.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyBackpackItem extends Item {

    public static final int MODE_HAND   = 0;
    public static final int MODE_HOTBAR = 1;
    public static final int MODE_ALL    = 2;

    private static final String[] MODE_KEYS = {
        "tooltip.industrial_core.backpack_mode_hand",
        "tooltip.industrial_core.backpack_mode_hotbar",
        "tooltip.industrial_core.backpack_mode_all"
    };

    private final int capacity;
    private final int maxTransfer;

    public EnergyBackpackItem(int capacity, int maxTransfer) {
        super(new Item.Properties().stacksTo(1));
        this.capacity    = capacity;
        this.maxTransfer = maxTransfer;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;

        int stored = getEnergy(stack);
        if (stored <= 0) return;

        int mode   = getMode(stack);
        int budget = Math.min(maxTransfer, stored);
        int spent  = 0;

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand != stack)
            spent += tryCharge(mainHand, budget - spent);

        if (mode >= MODE_HOTBAR) {
            for (int i = 0; i < 9 && budget - spent > 0; i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (slot == stack || slot == mainHand) continue;
                spent += tryCharge(slot, budget - spent);
            }
        }

        if (mode >= MODE_ALL && budget - spent > 0) {
            ItemStack offhand = player.getOffhandItem();
            if (offhand != stack) spent += tryCharge(offhand, budget - spent);
        }

        if (spent > 0) setEnergy(stack, stored - spent);
    }

    private int tryCharge(ItemStack target, int budget) {
        if (target.isEmpty() || budget <= 0) return 0;
        return target.getCapability(ForgeCapabilities.ENERGY).map(e -> {
            if (!e.canReceive()) return 0;
            return e.receiveEnergy(budget, false);
        }).orElse(0);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isCrouching()) {
            if (!level.isClientSide()) {
                int next = (getMode(stack) + 1) % 3;
                setMode(stack, next);
                player.displayClientMessage(
                    Component.translatable(MODE_KEYS[next]).withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public @NotNull ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<IEnergyStorage> lazy =
                LazyOptional.of(() -> new BackpackEnergy(stack, capacity, maxTransfer));

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap,
                                                               @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? lazy.cast() : LazyOptional.empty();
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> lines, TooltipFlag flag) {
        int stored = getEnergy(stack);
        lines.add(Component.literal(String.format("%,d / %,d FE", stored, capacity))
            .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE));
        lines.add(Component.literal(String.format("%,d FE/t", maxTransfer))
            .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE));
        lines.add(Component.translatable(MODE_KEYS[getMode(stack)])
            .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.industrial_core.backpack_hint")
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        int stored = getEnergy(stack);
        return capacity == 0 ? 0 : Math.round(13f * stored / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) { return com.jykito.industrialcore.ModStyle.ENERGY; }

    public static int getEnergy(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("Energy") : 0;
    }

    public static void setEnergy(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt("Energy", Math.max(0, amount));
    }

    public static int getMode(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("Mode") : MODE_HAND;
    }

    public static void setMode(ItemStack stack, int mode) {
        stack.getOrCreateTag().putInt("Mode", mode);
    }

    private static class BackpackEnergy implements IEnergyStorage {
        private final ItemStack stack;
        private final int capacity;
        private final int maxTransfer;

        BackpackEnergy(ItemStack stack, int capacity, int maxTransfer) {
            this.stack       = stack;
            this.capacity    = capacity;
            this.maxTransfer = maxTransfer;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int stored   = getEnergyStored();
            int received = Math.min(maxTransfer, Math.min(maxReceive, capacity - stored));
            if (!simulate && received > 0) setEnergy(stack, stored + received);
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int stored    = getEnergyStored();
            int extracted = Math.min(maxTransfer, Math.min(maxExtract, stored));
            if (!simulate && extracted > 0) setEnergy(stack, stored - extracted);
            return extracted;
        }

        @Override public int getEnergyStored()   { return EnergyBackpackItem.getEnergy(stack); }
        @Override public int getMaxEnergyStored() { return capacity; }
        @Override public boolean canExtract()     { return true; }
        @Override public boolean canReceive()     { return true; }
    }
}
