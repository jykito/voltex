package com.jykito.industrialcore.recipe;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * Recipe input over a plain container. In 1.21 recipes match a {@link RecipeInput} instead of a Container;
 * the machines still build a SimpleContainer, so this wraps it and keeps the old accessor names.
 */
public record ContainerInput(Container container) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) { return container.getItem(index); }

    @Override
    public int size() { return container.getContainerSize(); }

    /** Old Container name, so the recipe bodies did not have to change. */
    public int getContainerSize() { return size(); }
}
