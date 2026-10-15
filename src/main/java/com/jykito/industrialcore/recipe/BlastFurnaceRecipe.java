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

public class BlastFurnaceRecipe implements Recipe<ContainerInput> {

    /** Recipe for the late machine only (its industrial mode); the small machine does not see it. */
    private boolean lateOnly;
    private final Ingredient ingredient;
    private final int ingredientCount;
    private final Ingredient fuel;
    private final int fuelCount;
    private final ItemStack result;
    private final int processingTime;
    private final int requiredHeat;

    public BlastFurnaceRecipe(Ingredient ingredient, int ingredientCount,
                               Ingredient fuel, int fuelCount,
                               ItemStack result, int processingTime, int requiredHeat) {
        this.ingredient      = ingredient;
        this.ingredientCount = ingredientCount;
        this.fuel            = fuel;
        this.fuelCount       = fuelCount;
        this.result          = result;
        this.processingTime  = processingTime;
        this.requiredHeat    = requiredHeat;
    }

    public boolean isLateOnly() { return lateOnly; }

    @Override
    public boolean matches(ContainerInput container, Level level) {
        if (lateOnly) return false;
        if (level.isClientSide()) return false;
        return ingredientSlot(container.getItem(0), container.getItem(1)) >= 0;
    }

    public int ingredientSlot(ItemStack slot0, ItemStack slot1) {
        if (ingredient.test(slot0) && slot0.getCount() >= ingredientCount
                && fuel.test(slot1) && slot1.getCount() >= fuelCount) return 0;
        if (ingredient.test(slot1) && slot1.getCount() >= ingredientCount
                && fuel.test(slot0) && slot0.getCount() >= fuelCount) return 1;
        return -1;
    }

    @Override public ItemStack assemble(ContainerInput c, HolderLookup.Provider a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider a) { return result.copy(); }

    public Ingredient getIngredient()     { return ingredient; }
    public int        getIngredientCount(){ return ingredientCount; }
    public Ingredient getFuel()           { return fuel; }
    public int        getFuelCount()      { return fuelCount; }
    public int        getProcessingTime() { return processingTime; }
    public int        getRequiredHeat()   { return requiredHeat; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient);
        list.add(fuel);
        return list;
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.BLAST_FURNACE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()            { return ModRecipes.BLAST_FURNACE_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<BlastFurnaceRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<BlastFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                Codec.INT.optionalFieldOf("ingredientCount", 1).forGetter(r -> r.ingredientCount),
                Ingredient.CODEC_NONEMPTY.fieldOf("fuel").forGetter(r -> r.fuel),
                Codec.INT.optionalFieldOf("fuelCount", 1).forGetter(r -> r.fuelCount),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("processingTime", 300).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("requiredHeat", 2400).forGetter(r -> r.requiredHeat),
                Codec.BOOL.optionalFieldOf("lateOnly", false).forGetter(r -> r.lateOnly)
        ).apply(i, (ingredient, ingredientCount, fuel, fuelCount, result, time, heat, lateOnly) -> {
            BlastFurnaceRecipe r = new BlastFurnaceRecipe(ingredient, ingredientCount, fuel, fuelCount, result, time, heat);
            r.lateOnly = lateOnly;
            return r;
        }));

        private static final StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<BlastFurnaceRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipe> streamCodec() { return STREAM_CODEC; }

        private static BlastFurnaceRecipe read(RegistryFriendlyByteBuf buf) {
            Ingredient ingredient      = RecipeCodecs.readIngredient(buf);
            int        ingredientCount = buf.readInt();
            Ingredient fuel            = RecipeCodecs.readIngredient(buf);
            int        fuelCount       = buf.readInt();
            ItemStack  result          = RecipeCodecs.readItem(buf);
            int        processingTime  = buf.readInt();
            int        requiredHeat    = buf.readInt();
            BlastFurnaceRecipe recipe = new BlastFurnaceRecipe(ingredient, ingredientCount, fuel, fuelCount, result, processingTime, requiredHeat);
            recipe.lateOnly = buf.readBoolean();
            return recipe;
        }

        private static void write(RegistryFriendlyByteBuf buf, BlastFurnaceRecipe r) {
            RecipeCodecs.writeIngredient(buf, r.ingredient);
            buf.writeInt(r.ingredientCount);
            RecipeCodecs.writeIngredient(buf, r.fuel);
            buf.writeInt(r.fuelCount);
            RecipeCodecs.writeItem(buf, r.result);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.requiredHeat);
            buf.writeBoolean(r.lateOnly);
        }
    }
}
