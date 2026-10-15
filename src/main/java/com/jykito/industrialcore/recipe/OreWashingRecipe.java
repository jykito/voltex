package com.jykito.industrialcore.recipe;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;

import net.minecraft.core.HolderLookup;
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

public class OreWashingRecipe implements Recipe<ContainerInput> {

    /** Recipe for the late machine only (its industrial mode); the small machine does not see it. */
    private boolean lateOnly;
    private final Ingredient input;
    private final int inputCount;
    private final int waterAmount;
    private final ItemStack result;
    private final float resultChance;
    private final ItemStack byproduct;
    private final float byproductChance;
    private final int processingTime;
    private final int energyCostPerTick;

    public OreWashingRecipe(Ingredient input, int inputCount, int waterAmount,
                            ItemStack result, float resultChance, ItemStack byproduct, float byproductChance,
                            int processingTime, int energyCostPerTick) {
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

    public boolean isLateOnly() { return lateOnly; }

    @Override
    public boolean matches(ContainerInput container, Level level) {
        if (lateOnly) return false;
        if (level.isClientSide()) return false;
        return input.test(container.getItem(0)) && container.getItem(0).getCount() >= inputCount;
    }

    @Override public ItemStack assemble(ContainerInput c, HolderLookup.Provider a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider a) { return result.copy(); }

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

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ORE_WASHING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.ORE_WASHING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<OreWashingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<OreWashingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.input),
                Codec.INT.optionalFieldOf("inputCount", 1).forGetter(r -> r.inputCount),
                Codec.INT.optionalFieldOf("waterAmount", 1000).forGetter(r -> r.waterAmount),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.FLOAT.optionalFieldOf("resultChance", 1.0f).forGetter(r -> r.resultChance),
                RecipeCodecs.optionalResult("byproduct").forGetter(r -> r.byproduct),
                Codec.FLOAT.optionalFieldOf("byproductChance", 1.0f).forGetter(r -> r.byproductChance),
                Codec.INT.optionalFieldOf("processingTime", 200).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("energyCostPerTick", 40).forGetter(r -> r.energyCostPerTick),
                Codec.BOOL.optionalFieldOf("lateOnly", false).forGetter(r -> r.lateOnly)
        ).apply(i, (input, inputCount, water, result, resultChance, byprod, byprodChance, time, energy, lateOnly) -> {
            OreWashingRecipe r = new OreWashingRecipe(input, inputCount, water, result, resultChance, byprod, byprodChance, time, energy);
            r.lateOnly = lateOnly;
            return r;
        }));

        private static final StreamCodec<RegistryFriendlyByteBuf, OreWashingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<OreWashingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, OreWashingRecipe> streamCodec() { return STREAM_CODEC; }

        private static OreWashingRecipe read(RegistryFriendlyByteBuf buf) {
            Ingredient input = RecipeCodecs.readIngredient(buf);
            int inputCount   = buf.readInt();
            int waterAmount  = buf.readInt();
            ItemStack result = RecipeCodecs.readItem(buf);
            float resultChance = buf.readFloat();
            ItemStack byprod = RecipeCodecs.readItem(buf);
            float byprodChance = buf.readFloat();
            int processingTime = buf.readInt();
            int energyCost     = buf.readInt();
            OreWashingRecipe recipe = new OreWashingRecipe(input, inputCount, waterAmount, result, resultChance, byprod, byprodChance, processingTime, energyCost);
            recipe.lateOnly = buf.readBoolean();
            return recipe;
        }

        private static void write(RegistryFriendlyByteBuf buf, OreWashingRecipe r) {
            RecipeCodecs.writeIngredient(buf, r.input);
            buf.writeInt(r.inputCount);
            buf.writeInt(r.waterAmount);
            RecipeCodecs.writeItem(buf, r.result);
            buf.writeFloat(r.resultChance);
            RecipeCodecs.writeItem(buf, r.byproduct);
            buf.writeFloat(r.byproductChance);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
            buf.writeBoolean(r.lateOnly);
        }
    }
}
