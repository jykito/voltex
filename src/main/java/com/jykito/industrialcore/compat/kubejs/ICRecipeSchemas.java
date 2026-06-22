package com.jykito.industrialcore.compat.kubejs;

import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

public final class ICRecipeSchemas {

    private ICRecipeSchemas() {}

    public static final RecipeSchema LIQUEFIER       = liquefier();
    public static final RecipeSchema ORE_WASHING     = oreWashing();
    public static final RecipeSchema FLUID_ENRICHER  = fluidEnricher();
    public static final RecipeSchema PHASE_SPLITTER  = phaseSplitter();
    public static final RecipeSchema ROD_FABRICATION = rodFabrication();
    public static final RecipeSchema BLAST_FURNACE   = blastFurnace();
    public static final RecipeSchema GROWING         = growing();

    public static final RecipeSchema MACHINE         = machine();

    private static RecipeSchema liquefier() {
        RecipeKey<InputItem> ingredient  = ItemComponents.INPUT.key("ingredient");
        RecipeKey<String>    fluid       = StringComponent.ANY.key("fluid");
        RecipeKey<Integer>   fluidAmount = NumberComponent.INT.key("fluidAmount").optional(1000);
        RecipeKey<Integer>   itemCount   = NumberComponent.INT.key("itemCount").optional(1);
        RecipeKey<InputItem> ingredient2 = ItemComponents.INPUT.key("ingredient2").defaultOptional();
        RecipeKey<Integer>   itemCount2  = NumberComponent.INT.key("itemCount2").optional(1);
        RecipeKey<Integer>   heat        = NumberComponent.INT.key("requiredHeat").optional(1000);
        RecipeKey<Integer>   time        = NumberComponent.INT.key("processingTime").optional(200);
        RecipeKey<Integer>   energy      = NumberComponent.INT.key("energyCostPerTick").optional(40);
        return new RecipeSchema(ingredient, fluid, fluidAmount, itemCount, ingredient2, itemCount2, heat, time, energy);
    }

    private static RecipeSchema oreWashing() {
        RecipeKey<OutputItem> result          = ItemComponents.OUTPUT.key("result");
        RecipeKey<InputItem>  ingredient       = ItemComponents.INPUT.key("ingredient");
        RecipeKey<Integer>    inputCount       = NumberComponent.INT.key("inputCount").optional(1);
        RecipeKey<Integer>    waterAmount      = NumberComponent.INT.key("waterAmount").optional(1000);
        RecipeKey<Float>      resultChance     = NumberComponent.FLOAT.key("resultChance").optional(1.0f);
        RecipeKey<OutputItem> byproduct        = ItemComponents.OUTPUT.key("byproduct").defaultOptional();
        RecipeKey<Float>      byproductChance  = NumberComponent.FLOAT.key("byproductChance").optional(1.0f);
        RecipeKey<Integer>    time             = NumberComponent.INT.key("processingTime").optional(200);
        RecipeKey<Integer>    energy           = NumberComponent.INT.key("energyCostPerTick").optional(40);
        return new RecipeSchema(result, ingredient, inputCount, waterAmount, resultChance, byproduct, byproductChance, time, energy)
                .uniqueOutputId(result);
    }

    private static RecipeSchema fluidEnricher() {
        RecipeKey<InputItem> ingredient   = ItemComponents.INPUT.key("ingredient");
        RecipeKey<String>    fluidInput   = StringComponent.ANY.key("fluidInput");
        RecipeKey<String>    fluidOutput  = StringComponent.ANY.key("fluidOutput");
        RecipeKey<Integer>   itemCount    = NumberComponent.INT.key("itemCount").optional(1);
        RecipeKey<Integer>   fluidInAmt   = NumberComponent.INT.key("fluidInputAmount").optional(1000);
        RecipeKey<Integer>   fluidOutAmt  = NumberComponent.INT.key("fluidOutputAmount").optional(1000);
        RecipeKey<InputItem> ingredient2  = ItemComponents.INPUT.key("ingredient2").defaultOptional();
        RecipeKey<Integer>   itemCount2   = NumberComponent.INT.key("itemCount2").optional(1);
        RecipeKey<Integer>   time         = NumberComponent.INT.key("processingTime").optional(100);
        RecipeKey<Integer>   energy       = NumberComponent.INT.key("energyCostPerTick").optional(50);
        return new RecipeSchema(ingredient, fluidInput, fluidOutput, itemCount, fluidInAmt, fluidOutAmt, ingredient2, itemCount2, time, energy);
    }

    private static RecipeSchema phaseSplitter() {
        RecipeKey<OutputItem> result     = ItemComponents.OUTPUT.key("result");
        RecipeKey<InputItem>  ingredient  = ItemComponents.INPUT.key("ingredient");
        RecipeKey<OutputItem> result2     = ItemComponents.OUTPUT.key("result2").defaultOptional();
        RecipeKey<OutputItem> result3     = ItemComponents.OUTPUT.key("result3").defaultOptional();
        RecipeKey<Integer>    inputCount  = NumberComponent.INT.key("inputCount").optional(1);
        RecipeKey<Integer>    time        = NumberComponent.INT.key("processingTime").optional(400);
        RecipeKey<Integer>    energy      = NumberComponent.INT.key("energyCost").optional(100);
        return new RecipeSchema(result, ingredient, result2, result3, inputCount, time, energy)
                .uniqueOutputId(result);
    }

    private static RecipeSchema rodFabrication() {
        RecipeKey<OutputItem> result      = ItemComponents.OUTPUT.key("result");
        RecipeKey<InputItem>  ingredient1  = ItemComponents.INPUT.key("ingredient1");
        RecipeKey<InputItem>  ingredient2  = ItemComponents.INPUT.key("ingredient2");
        RecipeKey<Integer>    count1       = NumberComponent.INT.key("count1").optional(1);
        RecipeKey<Integer>    count2       = NumberComponent.INT.key("count2").optional(1);
        RecipeKey<Integer>    time         = NumberComponent.INT.key("processingTime").optional(200);
        RecipeKey<Integer>    energy       = NumberComponent.INT.key("energyCostPerTick").optional(500);
        return new RecipeSchema(result, ingredient1, ingredient2, count1, count2, time, energy)
                .uniqueOutputId(result);
    }

    private static RecipeSchema blastFurnace() {
        RecipeKey<OutputItem> result          = ItemComponents.OUTPUT.key("result");
        RecipeKey<InputItem>  ingredient       = ItemComponents.INPUT.key("ingredient");
        RecipeKey<InputItem>  fuel             = ItemComponents.INPUT.key("fuel");
        RecipeKey<Integer>    ingredientCount  = NumberComponent.INT.key("ingredientCount").optional(1);
        RecipeKey<Integer>    fuelCount        = NumberComponent.INT.key("fuelCount").optional(1);
        RecipeKey<Integer>    time             = NumberComponent.INT.key("processingTime").optional(300);
        RecipeKey<Integer>    heat             = NumberComponent.INT.key("requiredHeat").optional(2400);
        return new RecipeSchema(result, ingredient, fuel, ingredientCount, fuelCount, time, heat)
                .uniqueOutputId(result);
    }

    private static RecipeSchema growing() {
        RecipeKey<OutputItem> output     = ItemComponents.OUTPUT.key("output");
        RecipeKey<InputItem>  input       = ItemComponents.INPUT.key("input");
        RecipeKey<Integer>    inputCount  = NumberComponent.INT.key("inputCount").optional(1);
        RecipeKey<Integer>    time        = NumberComponent.INT.key("time").optional(100);
        RecipeKey<Integer>    energy      = NumberComponent.INT.key("energy").optional(200);
        RecipeKey<String>     category    = StringComponent.ANY.key("category").optional("mineral");
        RecipeKey<Boolean>    keepInput   = BooleanComponent.BOOLEAN.key("keepInput").optional(true);
        return new RecipeSchema(output, input, inputCount, time, energy, category, keepInput)
                .uniqueOutputId(output);
    }

    private static RecipeSchema machine() {
        RecipeKey<OutputItem> result     = ItemComponents.OUTPUT.key("result");
        RecipeKey<InputItem>  ingredient  = ItemComponents.INPUT.key("ingredient");
        RecipeKey<Integer>    inputCount  = NumberComponent.INT.key("inputCount").optional(1);
        RecipeKey<Integer>    time        = NumberComponent.INT.key("processingTime").optional(200);
        RecipeKey<Integer>    energy      = NumberComponent.INT.key("energyCost").optional(50);
        RecipeKey<String>     catalyst    = StringComponent.ANY.key("catalyst").defaultOptional();
        return new RecipeSchema(result, ingredient, inputCount, time, energy, catalyst)
                .uniqueOutputId(result);
    }
}
