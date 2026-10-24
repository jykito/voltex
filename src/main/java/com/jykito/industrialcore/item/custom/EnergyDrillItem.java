package com.jykito.industrialcore.item.custom;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import com.jykito.industrialcore.item.EnergyItem;
import com.jykito.industrialcore.item.ModDataComponents;
import com.jykito.industrialcore.capability.Caps;

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
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class EnergyDrillItem extends DiggerItem implements EnergyItem {

    protected final int capacity;
    protected final float miningSpeed;

    public EnergyDrillItem(Tier tier, int capacity, float miningSpeed, Properties properties) {
        super(tier, BlockTags.MINEABLE_WITH_PICKAXE, properties.attributes(DiggerItem.createAttributes(tier, 1.0f, -2.8f)));
        this.capacity = capacity;
        this.miningSpeed = miningSpeed;
    }

    public int getCapacity() { return capacity; }

    /** The drill drills rather than strikes: the arm swing is cancelled (for you and for other players); digging works as usual. */
    @Override
    public boolean onEntitySwing(ItemStack stack, net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.InteractionHand hand) {
        return true;
    }

    /**
     * The charge lives in NBT and changes with every broken block and every top-up from the pack, and the hand treats any
     * NBT change as a new item and lowers it. The re-swing happens only when the item or slot changes.
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    /** For the same reason spending or recharging energy does not reset the block breaking progress. */
    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !ItemStack.isSameItem(oldStack, newStack);
    }

    public static int getStoredEnergy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    public static void setStoredEnergy(ItemStack stack, int energy) {
        stack.set(ModDataComponents.ENERGY.get(), energy);
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
        return com.jykito.industrialcore.ModStyle.energyBar(getStoredEnergy(stack), capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) { return com.jykito.industrialcore.ModStyle.ENERGY; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        @Nullable Level level = context.level();
        tooltip.add(Component.literal(getStoredEnergy(stack) + " / " + capacity + " FE")
                .withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Nullable
    @Override
    public IEnergyStorage createEnergyStorage(ItemStack stack) {
        return new ComponentEnergyStorage(stack, ModDataComponents.ENERGY.get(), capacity, capacity, capacity);
    }

}
