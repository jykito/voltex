package com.jykito.industrialcore.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReactorComponentItem extends Item {

    public enum ComponentType {
        HEAT_VENT,
        HEAT_EXCHANGER,
        COMPONENT_VENT,
        ADVANCED_VENT,
        CONDENSATOR,
        GRAPHITE,
        INTEGRAL_VENT,
        ADVANCED_EXCHANGER
    }

    private final ComponentType type;
    private final int coolingRate;
    private final int maxAbsorb;
    private final String descKey;

    public ReactorComponentItem(ComponentType type, int coolingRate, String descKey, Properties props) {
        super(props);
        this.type        = type;
        this.coolingRate = coolingRate;
        this.maxAbsorb   = 0;
        this.descKey     = descKey;
    }

    public ReactorComponentItem(ComponentType type, int coolingRate, int maxAbsorb, String descKey, Properties props) {
        super(props);
        this.type        = type;
        this.coolingRate = coolingRate;
        this.maxAbsorb   = maxAbsorb;
        this.descKey     = descKey;
    }

    public ComponentType getComponentType() { return type; }
    public int getCoolingRate()             { return coolingRate; }
    public int getMaxAbsorb()               { return maxAbsorb; }

    public int getAbsorbedHeat(ItemStack stack) {
        return stack.getOrCreateTag().getInt("absorbedHeat");
    }

    public void addAbsorbedHeat(ItemStack stack, int amount) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("absorbedHeat", Math.min(maxAbsorb, tag.getInt("absorbedHeat") + amount));
    }

    public boolean isCondensatorFull(ItemStack stack) {
        return maxAbsorb > 0 && getAbsorbedHeat(stack) >= maxAbsorb;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(descKey).withStyle(ChatFormatting.GRAY));
        switch (type) {
            case HEAT_VENT ->
                tooltip.add(Component.translatable("tooltip.industrial_core.reactor_own_cooling", coolingRate)
                        .withStyle(ChatFormatting.AQUA));
            case HEAT_EXCHANGER ->
                tooltip.add(Component.translatable("tooltip.industrial_core.reactor_exchanger", coolingRate)
                        .withStyle(ChatFormatting.AQUA));
            case COMPONENT_VENT ->
                tooltip.add(Component.translatable("tooltip.industrial_core.reactor_adj_cooling", coolingRate)
                        .withStyle(ChatFormatting.AQUA));
            case ADVANCED_VENT ->
                tooltip.add(Component.translatable("tooltip.industrial_core.reactor_own_cooling", coolingRate)
                        .withStyle(ChatFormatting.AQUA));
            case CONDENSATOR -> {
                int absorbed = getAbsorbedHeat(stack);
                int pct = maxAbsorb > 0 ? absorbed * 100 / maxAbsorb : 0;
                tooltip.add(Component.translatable("tooltip.industrial_core.reactor_condensator",
                        absorbed, maxAbsorb, pct).withStyle(ChatFormatting.AQUA));
            }
            case GRAPHITE ->
                tooltip.add(Component.translatable("tooltip.industrial_core.reactor_graphite")
                        .withStyle(ChatFormatting.GREEN));
            case INTEGRAL_VENT ->
                tooltip.add(Component.translatable("tooltip.industrial_core.reactor_adj_cooling", coolingRate)
                        .withStyle(ChatFormatting.AQUA));
            case ADVANCED_EXCHANGER ->
                tooltip.add(Component.translatable("tooltip.industrial_core.reactor_exchanger", coolingRate)
                        .withStyle(ChatFormatting.AQUA));
        }
    }
}
