package com.jykito.industrialcore.item.custom;
import com.jykito.industrialcore.item.EnergyItem;
import com.jykito.industrialcore.item.ModDataComponents;
import java.util.Optional;
import com.jykito.industrialcore.capability.Caps;

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
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyBackpackItem extends Item implements EnergyItem {

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

        // "All", as the name and the guide promise: the offhand, worn armor and the rest of the inventory
        // (before, only the offhand: armor and the player's backpack were not charged in any mode; 10-10, 3.0.1)
        if (mode >= MODE_ALL && budget - spent > 0) {
            ItemStack offhand = player.getOffhandItem();
            if (offhand != stack) spent += tryCharge(offhand, budget - spent);
            for (ItemStack armor : player.getInventory().armor) {
                if (budget - spent <= 0) break;
                if (armor != stack) spent += tryCharge(armor, budget - spent);
            }
            for (int i = 9; i < player.getInventory().items.size() && budget - spent > 0; i++) {
                ItemStack slot = player.getInventory().items.get(i);
                if (slot == stack || slot == mainHand) continue;
                spent += tryCharge(slot, budget - spent);
            }
        }

        if (spent > 0) setEnergy(stack, stored - spent);
    }

    private int tryCharge(ItemStack target, int budget) {
        // we do not touch other amulets/packs: two of them in "All" mode would pass energy back and forth
        if (target.isEmpty() || budget <= 0 || target.getItem() instanceof EnergyBackpackItem) return 0;
        return Caps.get(target, Caps.ENERGY).map(e -> {
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
    public IEnergyStorage createEnergyStorage(ItemStack stack) {
        return new BackpackEnergy(stack, capacity, maxTransfer);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
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
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    public static void setEnergy(ItemStack stack, int amount) {
        stack.set(ModDataComponents.ENERGY.get(), Math.max(0, amount));
    }

    public static int getMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MODE.get(), MODE_HAND);
    }

    public static void setMode(ItemStack stack, int mode) {
        stack.set(ModDataComponents.MODE.get(), mode);
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
