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
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Crystallization: a fluid plus a seed item give an item.
 * <p>
 * This is the mod's only "fluid -> item" path: the liquefier does item -> fluid,
 * the enricher item + fluid -> fluid, the ore washer item + fluid -> items. Nothing did the reverse,
 * so the type is separate rather than going through {@code machine_recipe}.
 * <p>
 * The seed is optional: without it the machine crystallizes from the fluid alone.
 */
public class CrystallizerRecipe implements Recipe<ContainerInput> {

    private final ResourceLocation fluidInputId;
    private final int fluidInputAmount;
    private final Ingredient ingredient;
    private final int ingredientCount;
    private final ItemStack result;
    private final int processingTime;
    private final int energyCostPerTick;

    public CrystallizerRecipe(ResourceLocation fluidInputId, int fluidInputAmount,
                              Ingredient ingredient, int ingredientCount,
                              ItemStack result, int processingTime, int energyCostPerTick) {
        this.fluidInputId      = fluidInputId;
        this.fluidInputAmount  = fluidInputAmount;
        this.ingredient        = ingredient;
        this.ingredientCount   = ingredientCount;
        this.result            = result;
        this.processingTime    = processingTime;
        this.energyCostPerTick = energyCostPerTick;
    }

    /** Whether the seed slot contents fit. A recipe without a seed accepts an empty slot. */
    public boolean seedMatches(ItemStack seed) {
        if (!hasSeed()) return true;
        return ingredient.test(seed) && seed.getCount() >= ingredientCount;
    }

    /** The result as is, without a copy, for checks in the tick. Do not modify it. */
    public ItemStack        getResult()         { return result; }
    public boolean          hasSeed()           { return !ingredient.isEmpty(); }
    public Ingredient       getIngredient()     { return ingredient; }
    public int              getIngredientCount(){ return ingredientCount; }
    public int              getProcessingTime() { return processingTime; }
    public int              getEnergyCostPerTick() { return energyCostPerTick; }
    public int              getFluidInputAmount()  { return fluidInputAmount; }
    public ResourceLocation getFluidInputId()   { return fluidInputId; }

    public FluidStack makeFluidInput() {
        var fluid = BuiltInRegistries.FLUID.get(fluidInputId);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, fluidInputAmount);
    }

    @Override public boolean matches(ContainerInput c, Level level) { return !level.isClientSide() && seedMatches(c.getItem(0)); }
    @Override public ItemStack assemble(ContainerInput c, HolderLookup.Provider a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider a) { return result.copy(); }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        if (hasSeed()) list.add(ingredient);
        return list;
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.CRYSTALLIZER_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.CRYSTALLIZING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<CrystallizerRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        /** No "ingredient" means no seed, and then the count is 0 whatever "ingredientCount" says. */
        private static final MapCodec<CrystallizerRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("fluidInput").forGetter(r -> r.fluidInputId),
                Codec.INT.optionalFieldOf("fluidInputAmount", 1000).forGetter(r -> r.fluidInputAmount),
                Ingredient.CODEC_NONEMPTY.optionalFieldOf("ingredient", Ingredient.EMPTY).forGetter(r -> r.ingredient),
                Codec.INT.optionalFieldOf("ingredientCount", 1).forGetter(r -> r.ingredientCount),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("processingTime", 200).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("energyCostPerTick", 40).forGetter(r -> r.energyCostPerTick)
        ).apply(i, (fluidIn, fluidInAmt, ingredient, count, result, time, energy) ->
                new CrystallizerRecipe(fluidIn, fluidInAmt, ingredient, ingredient.isEmpty() ? 0 : count, result, time, energy)));

        private static final StreamCodec<RegistryFriendlyByteBuf, CrystallizerRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<CrystallizerRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, CrystallizerRecipe> streamCodec() { return STREAM_CODEC; }

        private static CrystallizerRecipe read(RegistryFriendlyByteBuf buf) {
            ResourceLocation fluidIn = buf.readResourceLocation();
            int        fluidInAmt    = buf.readInt();
            Ingredient ingredient    = RecipeCodecs.readIngredient(buf);
            int        count         = buf.readInt();
            ItemStack  result        = RecipeCodecs.readItem(buf);
            int        processingTime = buf.readInt();
            int        energyCost     = buf.readInt();
            return new CrystallizerRecipe(fluidIn, fluidInAmt, ingredient, count, result, processingTime, energyCost);
        }

        private static void write(RegistryFriendlyByteBuf buf, CrystallizerRecipe r) {
            buf.writeResourceLocation(r.fluidInputId);
            buf.writeInt(r.fluidInputAmount);
            RecipeCodecs.writeIngredient(buf, r.ingredient);
            buf.writeInt(r.ingredientCount);
            RecipeCodecs.writeItem(buf, r.result);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
        }
    }
}
