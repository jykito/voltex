package com.jykito.industrialcore.multiblock;

import com.jykito.industrialcore.block.entity.ElectricFurnaceBlockEntity;
import com.jykito.industrialcore.recipe.MachineRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * A recipe as the late multiblock core sees it: ingredients with counts per operation,
 * optional fluid in, item and/or fluid out, cycle time and FE usage
 * per tick per operation. The item can have a chance and a by-product with its own chance
 * (washing); the chance is rolled for each operation of the batch separately. {@code heat} is the heat threshold in the heat
 * ports below which the cycle stops (liquefier); heat is not spent, as with the small liquefier.
 * The core does not care which recipe type it came from.
 */
public record LateRecipe(ResourceLocation id, Ingredient[] items, int[] counts, FluidStack fluid,
                         ItemStack result, float resultChance, ItemStack byproduct, float byproductChance,
                         FluidStack fluidResult, int time, int energyPerTick, int heat) {

    /** No chances, by-product or heat: that is how most recipes are. */
    public LateRecipe(ResourceLocation id, Ingredient[] items, int[] counts, FluidStack fluid,
                      ItemStack result, FluidStack fluidResult, int time, int energyPerTick) {
        this(id, items, counts, fluid, result, 1f, ItemStack.EMPTY, 0f, fluidResult, time, energyPerTick, 0);
    }

    /** Where the core gets its recipes. */
    @FunctionalInterface
    public interface Source {
        List<LateRecipe> all(Level level);
    }

    /**
     * Core mode: name (lang key {@code gui.industrial_core.late_core.mode.<key>}) and its own recipe set.
     * The player picks the mode, not the input: in a modpack different modes may get recipes from the same input.
     */
    public record Mode(String key, Source source) {}

    /** Regular single-input machine recipes (crusher, compressor, ...). */
    public static Source machine(Supplier<RecipeType<MachineRecipe>> type) { return machine(type, false); }
    /** {@code industrial}: the industrial mode, lateOnly recipes only; the regular mode, only the ones shared with the small machine. */
    public static Source machine(Supplier<RecipeType<MachineRecipe>> type, boolean industrial) {
        return level -> level.getRecipeManager().getAllRecipesFor(type.get()).stream()
                .filter(h -> h.value().isLateOnly() == industrial)
                .map(h -> { var r = h.value(); return new LateRecipe(h.id(), new Ingredient[]{r.getIngredients().get(0)}, new int[]{r.getInputCount()},
                        FluidStack.EMPTY, r.getResultItem(level.registryAccess()), FluidStack.EMPTY, r.getProcessingTime(), r.getEnergyCost()); })
                .toList();
    }

    /** Assembler recipes. */
    public static Source assembling() {
        return level -> level.getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLING_TYPE.get()).stream()
                .map(h -> { var r = h.value(); return new LateRecipe(h.id(), r.getInputs(), r.getCounts(), r.getFluid(), r.getResult(),
                        r.getFluidResult(), r.getProcessingTime(), r.getEnergyPerTick()); })
                .toList();
    }

    /**
     * Vanilla smelting, as in the electric furnace: time and usage per operation are its constants,
     * the vanilla cookingtime is not used (JEI and the small furnace show the same).
     */
    public static Source smelting() {
        return level -> level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream()
                .filter(h -> !h.value().getIngredients().isEmpty())
                .map(h -> { var r = h.value(); return new LateRecipe(h.id(), new Ingredient[]{r.getIngredients().get(0)}, new int[]{1},
                        FluidStack.EMPTY, r.getResultItem(level.registryAccess()), FluidStack.EMPTY,
                        ElectricFurnaceBlockEntity.BASE_MAX_PROGRESS, ElectricFurnaceBlockEntity.BASE_ENERGY_PER_TICK); })
                .toList();
    }

    /** Ore washing recipes: water from the ports, main product and by-product, each with its own chance. */
    public static Source washing() { return washing(false); }
    public static Source washing(boolean industrial) {
        return level -> level.getRecipeManager().getAllRecipesFor(ModRecipes.ORE_WASHING_TYPE.get()).stream()
                .filter(h -> h.value().isLateOnly() == industrial)
                .map(h -> { var r = h.value(); return new LateRecipe(h.id(), new Ingredient[]{r.getInput()}, new int[]{r.getInputCount()},
                        r.getWaterAmount() > 0 ? new FluidStack(Fluids.WATER, r.getWaterAmount()) : FluidStack.EMPTY,
                        r.getResult(), r.getResultChance(), r.getByproduct(), r.hasByproduct() ? r.getByproductChance() : 0f,
                        FluidStack.EMPTY, r.getProcessingTime(), r.getEnergyCostPerTick(), 0); })
                .toList();
    }

    /** Enricher recipes: a fluid plus one or two reagents into another fluid. */
    public static Source enriching() { return enriching(false); }
    public static Source enriching(boolean industrial) {
        return level -> level.getRecipeManager().getAllRecipesFor(ModRecipes.FLUID_ENRICHER_TYPE.get()).stream()
                .filter(h -> h.value().isLateOnly() == industrial)
                .map(h -> { var r = h.value(); return new LateRecipe(h.id(),
                        r.hasSecondReagent() ? new Ingredient[]{r.getItemIngredient(), r.getItemIngredient2()} : new Ingredient[]{r.getItemIngredient()},
                        r.hasSecondReagent() ? new int[]{r.getItemCount(), r.getItemCount2()} : new int[]{r.getItemCount()},
                        r.makeFluidInput(), ItemStack.EMPTY, r.makeFluidOutput(), r.getProcessingTime(), r.getEnergyCostPerTick()); })
                .toList();
    }

    /**
     * Old magnet acceleration recipes. The late magnet takes inputs, result, time and usage;
     * the coil ({@code minTier}) and the instability band play no part for it.
     */
    public static Source accelerating() { return accelerating(false); }
    /** Industrial mode of the late magnet: lateOnly recipes, they can have a fluid input (Heart of Voltex). */
    public static Source accelerating(boolean industrial) {
        return level -> level.getRecipeManager().getAllRecipesFor(ModRecipes.ACCELERATING_TYPE.get()).stream()
                .filter(h -> h.value().isLateOnly() == industrial)
                .map(h -> { var r = h.value(); return new LateRecipe(h.id(), r.getInputs(), r.getCounts(), r.getFluid(), r.getResult(),
                        FluidStack.EMPTY, r.getProcessingTime(), r.getEnergyPerTick()); })
                .toList();
    }

    /** Liquefier recipes: one or two items into a fluid, with a heat threshold from the recipe. */
    public static Source liquefying() {
        return level -> level.getRecipeManager().getAllRecipesFor(ModRecipes.LIQUEFYING_TYPE.get()).stream()
                .map(h -> { var r = h.value(); return new LateRecipe(h.id(),
                        r.hasSecondIngredient() ? new Ingredient[]{r.getIngredient(), r.getIngredient2()} : new Ingredient[]{r.getIngredient()},
                        r.hasSecondIngredient() ? new int[]{r.getIngredientCount(), r.getIngredientCount2()} : new int[]{r.getIngredientCount()},
                        FluidStack.EMPTY, ItemStack.EMPTY, 1f, ItemStack.EMPTY, 0f, r.makeFluidOutput(),
                        r.getProcessingTime(), r.getEnergyCostPerTick(), r.getRequiredHeat()); })
                .toList();
    }

    /** FE usage per tick per operation of the tier 2 blast furnace: small blast furnace recipes have no energy (it is heated). */
    public static final int BLAST_ENERGY_PER_TICK = 40;

    /**
     * Blast furnace recipes are the same json as the small one's: input and fuel, time and heat threshold from the recipe.
     * The small one takes them from two slots in any order, the core from the input ports, order does not matter.
     */
    public static Source blasting() { return blasting(false); }
    public static Source blasting(boolean industrial) {
        return level -> level.getRecipeManager().getAllRecipesFor(ModRecipes.BLAST_FURNACE_TYPE.get()).stream()
                .filter(h -> h.value().isLateOnly() == industrial)
                .map(h -> { var r = h.value(); return new LateRecipe(h.id(), new Ingredient[]{r.getIngredient(), r.getFuel()},
                        new int[]{r.getIngredientCount(), r.getFuelCount()}, FluidStack.EMPTY,
                        r.getResultItem(level.registryAccess()), 1f, ItemStack.EMPTY, 0f, FluidStack.EMPTY,
                        r.getProcessingTime(), BLAST_ENERGY_PER_TICK, r.getRequiredHeat()); })
                .toList();
    }
}
