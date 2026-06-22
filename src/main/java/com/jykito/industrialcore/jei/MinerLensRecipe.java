package com.jykito.industrialcore.jei;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record MinerLensRecipe(ItemStack lens, List<Item> ores, List<Integer> weights, int poolTotalWeight) {

    public MinerLensRecipe(ItemStack lens, List<Item> ores, List<Integer> weights) {
        this(lens, ores, weights, weights.stream().mapToInt(Integer::intValue).sum());
    }

    public int totalWeight() { return poolTotalWeight; }

    public double chancePercent(int oreIndex) {
        if (poolTotalWeight <= 0) return 0;
        return weights.get(oreIndex) * 100.0 / poolTotalWeight;
    }
}
