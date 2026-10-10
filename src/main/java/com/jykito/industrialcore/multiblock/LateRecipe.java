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
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * Рецепт глазами ядра лейт-мультиблока: ингредиенты с количеством на одну операцию,
 * необязательная жидкость на входе, предмет и/или жидкость на выходе, время цикла и расход FE
 * за тик на одну операцию. У предмета может быть шанс и побочный продукт со своим шансом
 * (промывка) — шанс бросается на каждую операцию партии отдельно. {@code heat} — порог тепла в тепловых
 * портах, ниже которого цикл стоит (сжижитель); тепло не тратится, как у малого сжижителя.
 * Ядру всё равно, из какого типа рецепта это пришло.
 */
public record LateRecipe(ResourceLocation id, Ingredient[] items, int[] counts, FluidStack fluid,
                         ItemStack result, float resultChance, ItemStack byproduct, float byproductChance,
                         FluidStack fluidResult, int time, int energyPerTick, int heat) {

    /** Без шансов, побочного продукта и тепла: так устроено большинство рецептов. */
    public LateRecipe(ResourceLocation id, Ingredient[] items, int[] counts, FluidStack fluid,
                      ItemStack result, FluidStack fluidResult, int time, int energyPerTick) {
        this(id, items, counts, fluid, result, 1f, ItemStack.EMPTY, 0f, fluidResult, time, energyPerTick, 0);
    }

    /** Откуда ядро берёт свои рецепты. */
    @FunctionalInterface
    public interface Source {
        List<LateRecipe> all(Level level);
    }

    /**
     * Режим ядра: имя (ключ lang {@code gui.industrial_core.late_core.mode.<key>}) и свой набор рецептов.
     * Режим выбирает игрок, а не вход: в сборке у разных режимов могут оказаться рецепты из одного сырья.
     */
    public record Mode(String key, Source source) {}

    /** Обычные машинные рецепты с одним входом (дробилка, компрессор, …). */
    public static Source machine(Supplier<RecipeType<MachineRecipe>> type) { return machine(type, false); }
    /** {@code industrial} — промышленный режим: только рецепты lateOnly; обычный режим — только общие с малой машиной. */
    public static Source machine(Supplier<RecipeType<MachineRecipe>> type, boolean industrial) {
        return level -> level.getRecipeManager().getAllRecipesFor(type.get()).stream()
                .filter(r -> r.isLateOnly() == industrial)
                .map(r -> new LateRecipe(r.getId(), new Ingredient[]{r.getIngredients().get(0)}, new int[]{r.getInputCount()},
                        FluidStack.EMPTY, r.getResultItem(level.registryAccess()), FluidStack.EMPTY, r.getProcessingTime(), r.getEnergyCost()))
                .toList();
    }

    /** Рецепты сборщика. */
    public static Source assembling() {
        return level -> level.getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLING_TYPE.get()).stream()
                .map(r -> new LateRecipe(r.getId(), r.getInputs(), r.getCounts(), r.getFluid(), r.getResult(),
                        r.getFluidResult(), r.getProcessingTime(), r.getEnergyPerTick()))
                .toList();
    }

    /**
     * Ванильная плавка, как у электропечи: время и расход на операцию — её же константы,
     * ванильный cookingtime не используется (JEI и малая печь показывают то же самое).
     */
    public static Source smelting() {
        return level -> level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream()
                .filter(r -> !r.getIngredients().isEmpty())
                .map(r -> new LateRecipe(r.getId(), new Ingredient[]{r.getIngredients().get(0)}, new int[]{1},
                        FluidStack.EMPTY, r.getResultItem(level.registryAccess()), FluidStack.EMPTY,
                        ElectricFurnaceBlockEntity.BASE_MAX_PROGRESS, ElectricFurnaceBlockEntity.BASE_ENERGY_PER_TICK))
                .toList();
    }

    /** Рецепты промывки руды: вода из портов, основной продукт и побочный — каждый со своим шансом. */
    public static Source washing() { return washing(false); }
    public static Source washing(boolean industrial) {
        return level -> level.getRecipeManager().getAllRecipesFor(ModRecipes.ORE_WASHING_TYPE.get()).stream()
                .filter(r -> r.isLateOnly() == industrial)
                .map(r -> new LateRecipe(r.getId(), new Ingredient[]{r.getInput()}, new int[]{r.getInputCount()},
                        r.getWaterAmount() > 0 ? new FluidStack(Fluids.WATER, r.getWaterAmount()) : FluidStack.EMPTY,
                        r.getResult(), r.getResultChance(), r.getByproduct(), r.hasByproduct() ? r.getByproductChance() : 0f,
                        FluidStack.EMPTY, r.getProcessingTime(), r.getEnergyCostPerTick(), 0))
                .toList();
    }

    /** Рецепты обогатителя: жидкость плюс один-два реагента в другую жидкость. */
    public static Source enriching() { return enriching(false); }
    public static Source enriching(boolean industrial) {
        return level -> level.getRecipeManager().getAllRecipesFor(ModRecipes.FLUID_ENRICHER_TYPE.get()).stream()
                .filter(r -> r.isLateOnly() == industrial)
                .map(r -> new LateRecipe(r.getId(),
                        r.hasSecondReagent() ? new Ingredient[]{r.getItemIngredient(), r.getItemIngredient2()} : new Ingredient[]{r.getItemIngredient()},
                        r.hasSecondReagent() ? new int[]{r.getItemCount(), r.getItemCount2()} : new int[]{r.getItemCount()},
                        r.makeFluidInput(), ItemStack.EMPTY, r.makeFluidOutput(), r.getProcessingTime(), r.getEnergyCostPerTick()))
                .toList();
    }

    /**
     * Рецепты ускорения старого магнита. Поздний магнит берёт входы, результат, время и расход;
     * катушка ({@code minTier}) и полоса нестабильности у него не участвуют.
     */
    public static Source accelerating() { return accelerating(false); }
    /** Промышленный режим лейт-магнита: рецепты lateOnly, у них может быть жидкость на входе (Сердце Вольтекса). */
    public static Source accelerating(boolean industrial) {
        return level -> level.getRecipeManager().getAllRecipesFor(ModRecipes.ACCELERATING_TYPE.get()).stream()
                .filter(r -> r.isLateOnly() == industrial)
                .map(r -> new LateRecipe(r.getId(), r.getInputs(), r.getCounts(), r.getFluid(), r.getResult(),
                        FluidStack.EMPTY, r.getProcessingTime(), r.getEnergyPerTick()))
                .toList();
    }

    /** Рецепты сжижителя: один-два предмета в жидкость, с порогом тепла из рецепта. */
    public static Source liquefying() {
        return level -> level.getRecipeManager().getAllRecipesFor(ModRecipes.LIQUEFYING_TYPE.get()).stream()
                .map(r -> new LateRecipe(r.getId(),
                        r.hasSecondIngredient() ? new Ingredient[]{r.getIngredient(), r.getIngredient2()} : new Ingredient[]{r.getIngredient()},
                        r.hasSecondIngredient() ? new int[]{r.getIngredientCount(), r.getIngredientCount2()} : new int[]{r.getIngredientCount()},
                        FluidStack.EMPTY, ItemStack.EMPTY, 1f, ItemStack.EMPTY, 0f, r.makeFluidOutput(),
                        r.getProcessingTime(), r.getEnergyCostPerTick(), r.getRequiredHeat()))
                .toList();
    }

    /** Расход FE за тик на операцию у доменки 2-го тира: у рецептов малой доменки энергии нет (она греется теплом). */
    public static final int BLAST_ENERGY_PER_TICK = 40;

    /**
     * Рецепты доменки — те же json, что у малой: сырьё и топливо, время и порог тепла из рецепта.
     * Малая берёт их из двух слотов в любом порядке, ядро — из входных портов, порядок не важен.
     */
    public static Source blasting() { return blasting(false); }
    public static Source blasting(boolean industrial) {
        return level -> level.getRecipeManager().getAllRecipesFor(ModRecipes.BLAST_FURNACE_TYPE.get()).stream()
                .filter(r -> r.isLateOnly() == industrial)
                .map(r -> new LateRecipe(r.getId(), new Ingredient[]{r.getIngredient(), r.getFuel()},
                        new int[]{r.getIngredientCount(), r.getFuelCount()}, FluidStack.EMPTY,
                        r.getResultItem(level.registryAccess()), 1f, ItemStack.EMPTY, 0f, FluidStack.EMPTY,
                        r.getProcessingTime(), BLAST_ENERGY_PER_TICK, r.getRequiredHeat()))
                .toList();
    }
}
