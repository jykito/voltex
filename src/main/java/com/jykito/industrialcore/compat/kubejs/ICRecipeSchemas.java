package com.jykito.industrialcore.compat.kubejs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.fluid.InputFluid;
import dev.latvian.mods.kubejs.fluid.OutputFluid;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent;
import dev.latvian.mods.kubejs.recipe.component.FluidComponents;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.world.item.crafting.Ingredient;

public final class ICRecipeSchemas {

    private ICRecipeSchemas() {}

    /**
     * A recipe with a list of inputs where each input has its own count right in the object: {"item": ..., "count": 4}
     * (this is how the assembler and electromagnet read them). A regular RecipeJS writes an ingredient without a count.
     */
    public static class CountedInputsRecipeJS extends RecipeJS {
        @Override
        public JsonElement writeInputItem(InputItem value) {
            JsonElement j = value.ingredient.toJson();
            JsonObject o;
            if (j.isJsonObject()) o = j.getAsJsonObject().deepCopy();
            else {   // several ingredient variants are an array; inside an object with a count it becomes a Forge compound ingredient
                o = new JsonObject();
                o.addProperty("type", "forge:compound");
                o.add("children", j);
            }
            if (value.count > 1) o.addProperty("count", value.count);
            return o;
        }

        @Override
        public InputItem readInputItem(Object from) {
            if (from instanceof JsonObject o && o.has("count")) {
                JsonObject c = o.deepCopy();
                int n = c.remove("count").getAsInt();
                return InputItem.of(Ingredient.fromJson(c), n);
            }
            return super.readInputItem(from);
        }
    }

    public static final RecipeSchema ASSEMBLING      = assembling();
    public static final RecipeSchema ACCELERATING    = accelerating();
    public static final RecipeSchema BIOREACTOR      = bioreactor();
    public static final RecipeSchema CRYO            = cryo();

    /** Industrial assembler: many inputs with counts, optional fluid in and out. */
    private static RecipeSchema assembling() {
        RecipeKey<OutputItem>  result      = ItemComponents.OUTPUT.key("result");
        RecipeKey<InputItem[]> inputs      = ItemComponents.INPUT_ARRAY.key("inputs");
        RecipeKey<InputFluid>  fluid       = FluidComponents.INPUT.key("fluid").defaultOptional();
        RecipeKey<OutputFluid> fluidResult = FluidComponents.OUTPUT.key("fluidResult").defaultOptional();
        RecipeKey<Integer>     time        = NumberComponent.INT.key("processingTime").optional(200);
        RecipeKey<Integer>     energy      = NumberComponent.INT.key("energyCostPerTick").optional(1000);
        return new RecipeSchema(CountedInputsRecipeJS.class, CountedInputsRecipeJS::new, result, inputs, fluid, fluidResult, time, energy)
                .uniqueOutputId(result);
    }

    /** Electromagnet: inputs with counts, magnet tier, instability window; lateOnly means industrial electromagnet only. */
    private static RecipeSchema accelerating() {
        RecipeKey<OutputItem>  result    = ItemComponents.OUTPUT.key("result");
        RecipeKey<InputItem[]> inputs    = ItemComponents.INPUT_ARRAY.key("inputs");
        RecipeKey<InputFluid>  fluid     = FluidComponents.INPUT.key("fluid").defaultOptional();
        RecipeKey<Integer>     minTier   = NumberComponent.INT.key("minTier").optional(1);
        RecipeKey<Integer>     time      = NumberComponent.INT.key("processingTime").optional(200);
        RecipeKey<Integer>     energy    = NumberComponent.INT.key("energyCostPerTick").optional(1000);
        RecipeKey<Float>       minInstab = NumberComponent.FLOAT.key("minInstab").optional(0f);
        RecipeKey<Float>       bandLo    = NumberComponent.FLOAT.key("bandLo").optional(0f);
        RecipeKey<Float>       bandHi    = NumberComponent.FLOAT.key("bandHi").optional(0f);
        RecipeKey<Boolean>     lateOnly  = lateOnly();
        return new RecipeSchema(CountedInputsRecipeJS.class, CountedInputsRecipeJS::new,
                result, inputs, fluid, minTier, time, energy, minInstab, bandLo, bandHi, lateOnly)
                .uniqueOutputId(result);
    }

    /** Bioreactor: main input + optional additive, water and a heat threshold. */
    private static RecipeSchema bioreactor() {
        RecipeKey<OutputItem> result        = ItemComponents.OUTPUT.key("result");
        RecipeKey<InputItem>  ingredient    = ItemComponents.INPUT.key("ingredient");
        RecipeKey<InputItem>  additive      = ItemComponents.INPUT.key("additive").defaultOptional();
        RecipeKey<Integer>    inputCount    = NumberComponent.INT.key("inputCount").optional(1);
        RecipeKey<Integer>    additiveCount = NumberComponent.INT.key("additiveCount").optional(1);
        RecipeKey<Integer>    waterAmount   = NumberComponent.INT.key("waterAmount").optional(250);
        RecipeKey<Integer>    heat          = NumberComponent.INT.key("requiredHeat").optional(800);
        RecipeKey<Integer>    time          = NumberComponent.INT.key("processingTime").optional(400);
        RecipeKey<Integer>    energy        = NumberComponent.INT.key("energyCostPerTick").optional(60);
        return new RecipeSchema(result, ingredient, additive, inputCount, additiveCount, waterAmount, heat, time, energy)
                .uniqueOutputId(result);
    }

    /**
     * Cryo chamber: the item result is optional (condensate is fluid only), so the constructors are explicit:
     * (ingredient), (result, ingredient), (result, ingredient, stage); fluid is .fluidResult(id).fluidResultAmount(mB).
     */
    private static RecipeSchema cryo() {
        RecipeKey<OutputItem> result      = ItemComponents.OUTPUT.key("result").defaultOptional();
        RecipeKey<InputItem>  ingredient  = ItemComponents.INPUT.key("ingredient");
        RecipeKey<Integer>    stage       = NumberComponent.INT.key("requiredStage").optional(1);
        RecipeKey<Integer>    count       = NumberComponent.INT.key("ingredientCount").optional(1);
        RecipeKey<String>     fluidResult = StringComponent.ANY.key("fluidResult").defaultOptional();
        RecipeKey<Integer>    fluidAmount = NumberComponent.INT.key("fluidResultAmount").optional(0);
        RecipeKey<Integer>    time        = NumberComponent.INT.key("processingTime").optional(400);
        RecipeKey<Integer>    energy      = NumberComponent.INT.key("energyCostPerTick").optional(100);
        return new RecipeSchema(result, ingredient, stage, count, fluidResult, fluidAmount, time, energy)
                .constructor(ingredient)
                .constructor(result, ingredient)
                .constructor(result, ingredient, stage);
    }

    /** "Late machine only" marker (its industrial mode): the small machine does not see such a recipe. */
    private static RecipeKey<Boolean> lateOnly() {
        return BooleanComponent.BOOLEAN.key("lateOnly").optional(false);
    }

    public static final RecipeSchema LIQUEFIER       = liquefier();
    public static final RecipeSchema ORE_WASHING     = oreWashing();
    public static final RecipeSchema FLUID_ENRICHER  = fluidEnricher();
    public static final RecipeSchema PHASE_SPLITTER  = phaseSplitter();
    public static final RecipeSchema ROD_FABRICATION = rodFabrication();
    public static final RecipeSchema BLAST_FURNACE   = blastFurnace();
    public static final RecipeSchema GROWING         = growing();

    public static final RecipeSchema CRYSTALLIZER    = crystallizer();

    public static final RecipeSchema MACHINE         = machine();

    /** The seed is optional: a recipe can crystallize from a fluid alone. */
    private static RecipeSchema crystallizer() {
        RecipeKey<OutputItem> result     = ItemComponents.OUTPUT.key("result");
        RecipeKey<String>     fluidInput  = StringComponent.ANY.key("fluidInput");
        RecipeKey<Integer>    fluidAmount = NumberComponent.INT.key("fluidInputAmount").optional(1000);
        RecipeKey<InputItem>  ingredient  = ItemComponents.INPUT.key("ingredient").defaultOptional();
        RecipeKey<Integer>    count       = NumberComponent.INT.key("ingredientCount").optional(1);
        RecipeKey<Integer>    time        = NumberComponent.INT.key("processingTime").optional(200);
        RecipeKey<Integer>    energy      = NumberComponent.INT.key("energyCostPerTick").optional(40);
        return new RecipeSchema(result, fluidInput, fluidAmount, ingredient, count, time, energy)
                .uniqueOutputId(result);
    }

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
        return new RecipeSchema(result, ingredient, inputCount, waterAmount, resultChance, byproduct, byproductChance, time, energy, lateOnly())
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
        return new RecipeSchema(ingredient, fluidInput, fluidOutput, itemCount, fluidInAmt, fluidOutAmt, ingredient2, itemCount2, time, energy, lateOnly());
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
        return new RecipeSchema(result, ingredient, fuel, ingredientCount, fuelCount, time, heat, lateOnly())
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
        return new RecipeSchema(result, ingredient, inputCount, time, energy, catalyst, lateOnly())
                .uniqueOutputId(result);
    }
}
