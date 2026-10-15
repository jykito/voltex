package com.jykito.industrialcore.jei;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public record CapsuleFillerJeiRecipe(ItemStack emptyContainer, FluidStack fluid, ItemStack filledContainer) {}
