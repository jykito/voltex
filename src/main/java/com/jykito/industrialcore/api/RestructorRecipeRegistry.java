package com.jykito.industrialcore.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RestructorRecipeRegistry {

    public record KubeRecipe(
        String inputId,
        int inputCount,
        String outputId,
        int outputCount,
        String catalystId,
        int energy
    ) {}

    public record RestructorRecipeData(
        int inputCount,
        ItemStack result,
        int energyCost
    ) {}

    private static final List<KubeRecipe> RECIPES = new ArrayList<>();

    public static void register(KubeRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static void clear() {
        RECIPES.clear();
    }

    public static List<KubeRecipe> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static Optional<RestructorRecipeData> findMatchData(ItemStack input, ItemStack catalyst) {
        for (KubeRecipe r : RECIPES) {
            Item inputItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(r.inputId()));
            if (inputItem == null) continue;
            if (!input.is(inputItem) || input.getCount() < r.inputCount()) continue;

            if (r.catalystId() != null && !r.catalystId().isEmpty()) {
                Item catItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(r.catalystId()));
                if (catItem == null || catalyst.isEmpty() || !catalyst.is(catItem)) continue;
            }

            Item outputItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(r.outputId()));
            if (outputItem == null) continue;

            return Optional.of(new RestructorRecipeData(
                r.inputCount(),
                new ItemStack(outputItem, r.outputCount()),
                r.energy()
            ));
        }
        return Optional.empty();
    }
}
