package com.jykito.industrialcore.recipe;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, IndustrialCore.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, IndustrialCore.MODID);

    public static final RegistryObject<RecipeSerializer<MachineRecipe>> MACHINE_SERIALIZER =
            SERIALIZERS.register("machine_recipe", () -> MachineRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<MachineRecipe>> CRUSHING_TYPE =
            RECIPE_TYPES.register("crushing", () -> new RecipeType<MachineRecipe>() {
                @Override public String toString() { return "crushing"; }
            });

    public static final RegistryObject<RecipeType<MachineRecipe>> COMPRESSING_TYPE =
            RECIPE_TYPES.register("compressing", () -> new RecipeType<MachineRecipe>() {
                @Override public String toString() { return "compressing"; }
            });

    public static final RegistryObject<RecipeType<MachineRecipe>> ELECTRIC_SMELTING_TYPE =
            RECIPE_TYPES.register("electric_smelting", () -> new RecipeType<MachineRecipe>() {
                @Override public String toString() { return "electric_smelting"; }
            });

    public static final RegistryObject<RecipeType<MachineRecipe>> EXTRACTING_TYPE =
            RECIPE_TYPES.register("extracting", () -> new RecipeType<MachineRecipe>() {
                @Override public String toString() { return "extracting"; }
            });

    public static final RegistryObject<RecipeType<MachineRecipe>> EXTRUDING_TYPE =
            RECIPE_TYPES.register("extruding", () -> new RecipeType<MachineRecipe>() {
                @Override public String toString() { return "extruding"; }
            });

    public static final RegistryObject<RecipeType<MachineRecipe>> ROLLING_TYPE =
            RECIPE_TYPES.register("rolling", () -> new RecipeType<MachineRecipe>() {
                @Override public String toString() { return "rolling"; }
            });

    public static final RegistryObject<RecipeType<PhaseSplitterRecipe>> PHASE_SPLITTING_TYPE =
            RECIPE_TYPES.register("phase_splitting", () -> new RecipeType<PhaseSplitterRecipe>() {
                @Override public String toString() { return "phase_splitting"; }
            });

    public static final RegistryObject<RecipeSerializer<PhaseSplitterRecipe>> PHASE_SPLITTER_SERIALIZER =
            SERIALIZERS.register("phase_splitter_recipe", () -> PhaseSplitterRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<MachineRecipe>> MOLECULAR_TRANSFORMING_TYPE =
            RECIPE_TYPES.register("molecular_transforming", () -> new RecipeType<MachineRecipe>() {
                @Override public String toString() { return "molecular_transforming"; }
            });

    public static final RegistryObject<RecipeType<RodFabricatorRecipe>> ROD_FABRICATION_TYPE =
            RECIPE_TYPES.register("rod_fabrication", () -> new RecipeType<RodFabricatorRecipe>() {
                @Override public String toString() { return "rod_fabrication"; }
            });

    public static final RegistryObject<RecipeSerializer<RodFabricatorRecipe>> ROD_FABRICATION_SERIALIZER =
            SERIALIZERS.register("rod_fabrication_recipe", () -> RodFabricatorRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<BlastFurnaceRecipe>> BLAST_FURNACE_TYPE =
            RECIPE_TYPES.register("blast_furnace", () -> new RecipeType<BlastFurnaceRecipe>() {
                @Override public String toString() { return "blast_furnace"; }
            });

    public static final RegistryObject<RecipeSerializer<BlastFurnaceRecipe>> BLAST_FURNACE_SERIALIZER =
            SERIALIZERS.register("blast_furnace_recipe", () -> BlastFurnaceRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<FluidEnricherRecipe>> FLUID_ENRICHER_TYPE =
            RECIPE_TYPES.register("fluid_enricher", () -> new RecipeType<FluidEnricherRecipe>() {
                @Override public String toString() { return "fluid_enricher"; }
            });

    public static final RegistryObject<RecipeSerializer<FluidEnricherRecipe>> FLUID_ENRICHER_SERIALIZER =
            SERIALIZERS.register("fluid_enricher_recipe", () -> FluidEnricherRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<LiquefierRecipe>> LIQUEFYING_TYPE =
            RECIPE_TYPES.register("liquefying", () -> new RecipeType<LiquefierRecipe>() {
                @Override public String toString() { return "liquefying"; }
            });

    public static final RegistryObject<RecipeSerializer<LiquefierRecipe>> LIQUEFIER_SERIALIZER =
            SERIALIZERS.register("liquefier_recipe", () -> LiquefierRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<OreWashingRecipe>> ORE_WASHING_TYPE =
            RECIPE_TYPES.register("ore_washing", () -> new RecipeType<OreWashingRecipe>() {
                @Override public String toString() { return "ore_washing"; }
            });

    public static final RegistryObject<RecipeSerializer<OreWashingRecipe>> ORE_WASHING_SERIALIZER =
            SERIALIZERS.register("ore_washing_recipe", () -> OreWashingRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeType<GrowingRecipe>> GROWING_TYPE =
            RECIPE_TYPES.register("growing", () -> new RecipeType<GrowingRecipe>() {
                @Override public String toString() { return "growing"; }
            });

    public static final RegistryObject<RecipeSerializer<GrowingRecipe>> GROWING_SERIALIZER =
            SERIALIZERS.register("growing_recipe", () -> GrowingRecipe.Serializer.INSTANCE);

    public static final RegistryObject<RecipeSerializer<MachineRecipe>> CRUSHING_SERIALIZER =
            SERIALIZERS.register("crushing", () -> new MachineTypeRecipeSerializer(() -> CRUSHING_TYPE.get()));
    public static final RegistryObject<RecipeSerializer<MachineRecipe>> COMPRESSING_SERIALIZER =
            SERIALIZERS.register("compressing", () -> new MachineTypeRecipeSerializer(() -> COMPRESSING_TYPE.get()));
    public static final RegistryObject<RecipeSerializer<MachineRecipe>> ELECTRIC_SMELTING_SERIALIZER =
            SERIALIZERS.register("electric_smelting", () -> new MachineTypeRecipeSerializer(() -> ELECTRIC_SMELTING_TYPE.get()));
    public static final RegistryObject<RecipeSerializer<MachineRecipe>> EXTRACTING_SERIALIZER =
            SERIALIZERS.register("extracting", () -> new MachineTypeRecipeSerializer(() -> EXTRACTING_TYPE.get()));
    public static final RegistryObject<RecipeSerializer<MachineRecipe>> EXTRUDING_SERIALIZER =
            SERIALIZERS.register("extruding", () -> new MachineTypeRecipeSerializer(() -> EXTRUDING_TYPE.get()));
    public static final RegistryObject<RecipeSerializer<MachineRecipe>> ROLLING_SERIALIZER =
            SERIALIZERS.register("rolling", () -> new MachineTypeRecipeSerializer(() -> ROLLING_TYPE.get()));
    public static final RegistryObject<RecipeSerializer<MachineRecipe>> MOLECULAR_TRANSFORMING_SERIALIZER =
            SERIALIZERS.register("molecular_transforming", () -> new MachineTypeRecipeSerializer(() -> MOLECULAR_TRANSFORMING_TYPE.get()));

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        SERIALIZERS.register(eventBus);
    }
}
