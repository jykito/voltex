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

public class LiquefierRecipe implements Recipe<ContainerInput> {

    private final Ingredient ingredient;
    private final int ingredientCount;
    private final Ingredient ingredient2;
    private final int ingredientCount2;
    private final int requiredHeat;
    private final ResourceLocation fluidOutputId;
    private final int fluidOutputAmount;
    private final int processingTime;
    private final int energyCostPerTick;

    public LiquefierRecipe(Ingredient ingredient, int ingredientCount,
                           Ingredient ingredient2, int ingredientCount2,
                           int requiredHeat,
                           ResourceLocation fluidOutputId, int fluidOutputAmount,
                           int processingTime, int energyCostPerTick) {
        this.ingredient        = ingredient;
        this.ingredientCount   = ingredientCount;
        this.ingredient2       = ingredient2;
        this.ingredientCount2  = ingredientCount2;
        this.requiredHeat      = requiredHeat;
        this.fluidOutputId     = fluidOutputId;
        this.fluidOutputAmount = fluidOutputAmount;
        this.processingTime    = processingTime;
        this.energyCostPerTick = energyCostPerTick;
    }

    @Override
    public boolean matches(ContainerInput container, Level level) {
        if (level.isClientSide()) return false;
        return inputsMatch(container.getItem(0), container.getItem(1));
    }

    public boolean hasSecondIngredient() { return !ingredient2.isEmpty(); }

    public boolean inputsMatch(ItemStack s0, ItemStack s1) {
        if (!hasSecondIngredient()) {
            return (ingredient.test(s0) && s0.getCount() >= ingredientCount)
                || (ingredient.test(s1) && s1.getCount() >= ingredientCount);
        }
        return inputPair(s0, s1) || inputPair(s1, s0);
    }

    public boolean inputPair(ItemStack slotA, ItemStack slotB) {
        return ingredient.test(slotA)  && slotA.getCount() >= ingredientCount
            && ingredient2.test(slotB) && slotB.getCount() >= ingredientCount2;
    }

    @Override public ItemStack assemble(ContainerInput c, HolderLookup.Provider a) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider a) { return ItemStack.EMPTY; }

    public FluidStack makeFluidOutput() {
        var fluid = BuiltInRegistries.FLUID.get(fluidOutputId);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, fluidOutputAmount);
    }

    public Ingredient getIngredient()        { return ingredient; }
    public int        getIngredientCount()   { return ingredientCount; }
    public Ingredient getIngredient2()       { return ingredient2; }
    public int        getIngredientCount2()  { return ingredientCount2; }
    public int        getRequiredHeat()      { return requiredHeat; }
    public int        getProcessingTime()    { return processingTime; }
    public int        getEnergyCostPerTick() { return energyCostPerTick; }
    public int        getFluidOutputAmount() { return fluidOutputAmount; }
    public ResourceLocation getFluidOutputId() { return fluidOutputId; }

    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient);
        if (hasSecondIngredient()) list.add(ingredient2);
        return list;
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.LIQUEFIER_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.LIQUEFYING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<LiquefierRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        /** Here "fluid" is a plain id string. No "ingredient2" means its count is 0. */
        private static final MapCodec<LiquefierRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                Codec.INT.optionalFieldOf("itemCount", 1).forGetter(r -> r.ingredientCount),
                Ingredient.CODEC_NONEMPTY.optionalFieldOf("ingredient2", Ingredient.EMPTY).forGetter(r -> r.ingredient2),
                Codec.INT.optionalFieldOf("itemCount2", 1).forGetter(r -> r.ingredientCount2),
                Codec.INT.optionalFieldOf("requiredHeat", 1000).forGetter(r -> r.requiredHeat),
                ResourceLocation.CODEC.fieldOf("fluid").forGetter(r -> r.fluidOutputId),
                Codec.INT.optionalFieldOf("fluidAmount", 1000).forGetter(r -> r.fluidOutputAmount),
                Codec.INT.optionalFieldOf("processingTime", 200).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("energyCostPerTick", 40).forGetter(r -> r.energyCostPerTick)
        ).apply(i, (ing, count, ing2, count2, heat, fluidOut, fluidAmount, time, energy) ->
                new LiquefierRecipe(ing, count, ing2, ing2.isEmpty() ? 0 : count2, heat, fluidOut, fluidAmount, time, energy)));

        private static final StreamCodec<RegistryFriendlyByteBuf, LiquefierRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<LiquefierRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, LiquefierRecipe> streamCodec() { return STREAM_CODEC; }

        private static LiquefierRecipe read(RegistryFriendlyByteBuf buf) {
            Ingredient ingredient   = RecipeCodecs.readIngredient(buf);
            int ingredientCount     = buf.readInt();
            Ingredient ingredient2  = RecipeCodecs.readIngredient(buf);
            int ingredientCount2    = buf.readInt();
            int requiredHeat        = buf.readInt();
            ResourceLocation fluidOut = buf.readResourceLocation();
            int fluidAmount         = buf.readInt();
            int processingTime      = buf.readInt();
            int energyCostPerTick   = buf.readInt();
            return new LiquefierRecipe(ingredient, ingredientCount, ingredient2, ingredientCount2,
                    requiredHeat, fluidOut, fluidAmount, processingTime, energyCostPerTick);
        }

        private static void write(RegistryFriendlyByteBuf buf, LiquefierRecipe r) {
            RecipeCodecs.writeIngredient(buf, r.ingredient);
            buf.writeInt(r.ingredientCount);
            RecipeCodecs.writeIngredient(buf, r.ingredient2);
            buf.writeInt(r.ingredientCount2);
            buf.writeInt(r.requiredHeat);
            buf.writeResourceLocation(r.fluidOutputId);
            buf.writeInt(r.fluidOutputAmount);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
        }
    }
}
