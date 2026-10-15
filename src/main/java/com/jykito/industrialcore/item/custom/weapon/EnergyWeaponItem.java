package com.jykito.industrialcore.item.custom.weapon;

import com.jykito.industrialcore.ModStyle;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Shared by energy weapons: charge in NBT ({@code energy}, like the drills), the charge bar and line, accepting energy from
 * chargers and the energy pack, charge shared with its suit (Nexite for the blade, Plasma for the gauss): while the weapon is in hand
 * and charged below half, once a second it tops up to {@link #TOP_UP} FE from the suit pieces.
 */
public abstract class EnergyWeaponItem extends Item {

    public static final int TOP_UP = 5_000;
    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    protected final int capacity;

    protected EnergyWeaponItem(int capacity, Properties properties) {
        super(properties);
        this.capacity = capacity;
    }

    public int getCapacity() { return capacity; }

    public static int getEnergy(ItemStack stack) { return stack.hasTag() ? stack.getTag().getInt("energy") : 0; }

    public void setEnergy(ItemStack stack, int amount) { stack.getOrCreateTag().putInt("energy", Mth.clamp(amount, 0, capacity)); }

    /** Take {@code cost} if there is enough; otherwise nothing is spent. */
    public boolean drain(ItemStack stack, int cost) {
        int have = getEnergy(stack);
        if (have < cost) return false;
        setEnergy(stack, have - cost);
        return true;
    }

    // ───────── suit ─────────

    /** Suit pieces of this weapon worn by the player. */
    protected abstract List<ItemStack> suit(Player player);
    protected abstract int suitEnergy(ItemStack piece);
    protected abstract void setSuitEnergy(ItemStack piece, int amount);

    protected static List<ItemStack> pieces(Player player, Predicate<ItemStack> kind) {
        List<ItemStack> out = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR) { ItemStack s = player.getItemBySlot(slot); if (kind.test(s)) out.add(s); }
        return out;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !selected || !(entity instanceof Player player) || player.tickCount % 20 != 0) return;
        int have = getEnergy(stack);
        if (have >= capacity / 2) return;
        int want = Math.min(TOP_UP, capacity - have), got = 0;
        for (ItemStack piece : suit(player)) {
            int take = Math.min(suitEnergy(piece), want - got);
            if (take > 0) { setSuitEnergy(piece, suitEnergy(piece) - take); got += take; }
            if (got >= want) break;
        }
        if (got > 0) { setEnergy(stack, have + got); player.getInventory().setChanged(); }
    }

    // ───────── look and tooltip ─────────

    /** Charge changes the NBT, and the hand treats that as a new item and lowers it; the re-swing happens only when the item changes. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13.0f * getEnergy(stack) / capacity); }
    @Override public int getBarColor(ItemStack stack) { return ModStyle.ENERGY; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(String.format("%,d / %,d FE", getEnergy(stack), capacity)).withStyle(ModStyle.ENERGY_STYLE));
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final EnergyStorage storage = new EnergyStorage(capacity, capacity, capacity) {
                @Override public int getEnergyStored() { return getEnergy(stack); }
                @Override public int receiveEnergy(int maxReceive, boolean simulate) {
                    int stored = getEnergyStored(), received = Math.min(maxReceive, capacity - stored);
                    if (!simulate && received > 0) setEnergy(stack, stored + received);
                    return received;
                }
                @Override public int extractEnergy(int maxExtract, boolean simulate) {
                    int stored = getEnergyStored(), extracted = Math.min(maxExtract, stored);
                    if (!simulate && extracted > 0) setEnergy(stack, stored - extracted);
                    return extracted;
                }
            };
            private final LazyOptional<IEnergyStorage> lazy = LazyOptional.of(() -> storage);

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? lazy.cast() : LazyOptional.empty();
            }
        };
    }
}
