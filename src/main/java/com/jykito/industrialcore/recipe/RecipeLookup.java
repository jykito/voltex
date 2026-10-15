package com.jykito.industrialcore.recipe;

import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Recipe manager lookups returning the recipes themselves. In 1.21 the manager hands out {@link RecipeHolder}s
 * (the id moved out of the recipe); most of the mod only needs the recipe, so the holder is unwrapped here.
 */
public final class RecipeLookup {
    private RecipeLookup() {}

    /** All recipes of a type. */
    public static <I extends RecipeInput, T extends Recipe<I>> List<T> all(RecipeManager rm, RecipeType<T> type) {
        return rm.getAllRecipesFor(type).stream().map(RecipeHolder::value).toList();
    }

    public static <I extends RecipeInput, T extends Recipe<I>> List<T> all(Level level, RecipeType<T> type) {
        return all(level.getRecipeManager(), type);
    }

    /** First matching recipe for a machine container. */
    public static <T extends Recipe<ContainerInput>> Optional<T> find(RecipeManager rm, RecipeType<T> type,
                                                                     Container container, Level level) {
        return rm.getRecipeFor(type, new ContainerInput(container), level).map(RecipeHolder::value);
    }
}
