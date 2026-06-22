package com.jykito.industrialcore.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public record CapsuleFillerJeiRecipe(ItemStack emptyContainer, FluidStack fluid, ItemStack filledContainer) {}
