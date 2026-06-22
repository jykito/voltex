package com.jykito.industrialcore.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class OreWashingRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final Ingredient input;
    private final int inputCount;
    private final int waterAmount;
    private final ItemStack result;
    private final float resultChance;
    private final ItemStack byproduct;
    private final float byproductChance;
    private final int processingTime;
    private final int energyCostPerTick;

    public OreWashingRecipe(ResourceLocation id, Ingredient input, int inputCount, int waterAmount,
                            ItemStack result, float resultChance, ItemStack byproduct, float byproductChance,
                            int processingTime, int energyCostPerTick) {
        this.id = id;
        this.input = input;
        this.inputCount = inputCount;
        this.waterAmount = waterAmount;
        this.result = result;
        this.resultChance = resultChance;
        this.byproduct = byproduct;
        this.byproductChance = byproductChance;
        this.processingTime = processingTime;
        this.energyCostPerTick = energyCostPerTick;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (level.isClientSide()) return false;
        return input.test(container.getItem(0)) && container.getItem(0).getCount() >= inputCount;
    }

    @Override public ItemStack assemble(SimpleContainer c, RegistryAccess a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(RegistryAccess a) { return result.copy(); }

    public Ingredient getInput()           { return input; }
    public int        getInputCount()      { return inputCount; }
    public int        getWaterAmount()     { return waterAmount; }
    public ItemStack  getResult()          { return result.copy(); }
    public float      getResultChance()    { return resultChance; }
    public ItemStack  getByproduct()       { return byproduct.copy(); }
    public boolean    hasByproduct()       { return !byproduct.isEmpty(); }
    public float      getByproductChance() { return byproductChance; }
    public int        getProcessingTime()  { return processingTime; }
    public int        getEnergyCostPerTick(){ return energyCostPerTick; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(input);
        return list;
    }

    @Override public ResourceLocation getId()            { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ORE_WASHING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.ORE_WASHING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<OreWashingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public OreWashingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input  = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            int inputCount    = GsonHelper.getAsInt(json, "inputCount", 1);
            int waterAmount   = GsonHelper.getAsInt(json, "waterAmount", 1000);
            ItemStack result  = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            float resultChance = GsonHelper.getAsFloat(json, "resultChance", 1.0f);
            ItemStack byprod  = json.has("byproduct")
                    ? ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "byproduct"))
                    : ItemStack.EMPTY;
            float byprodChance = GsonHelper.getAsFloat(json, "byproductChance", 1.0f);
            int processingTime = GsonHelper.getAsInt(json, "processingTime", 200);
            int energyCost     = GsonHelper.getAsInt(json, "energyCostPerTick", 40);
            return new OreWashingRecipe(id, input, inputCount, waterAmount, result, resultChance, byprod, byprodChance, processingTime, energyCost);
        }

        @Override
        public @Nullable OreWashingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            int inputCount   = buf.readInt();
            int waterAmount  = buf.readInt();
            ItemStack result = buf.readItem();
            float resultChance = buf.readFloat();
            ItemStack byprod = buf.readItem();
            float byprodChance = buf.readFloat();
            int processingTime = buf.readInt();
            int energyCost     = buf.readInt();
            return new OreWashingRecipe(id, input, inputCount, waterAmount, result, resultChance, byprod, byprodChance, processingTime, energyCost);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, OreWashingRecipe r) {
            r.input.toNetwork(buf);
            buf.writeInt(r.inputCount);
            buf.writeInt(r.waterAmount);
            buf.writeItem(r.result);
            buf.writeFloat(r.resultChance);
            buf.writeItem(r.byproduct);
            buf.writeFloat(r.byproductChance);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
        }
    }
}
