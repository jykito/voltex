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
 * Cryo chamber recipe.
 * <p>
 * Unlike all other types it has the {@code requiredStage} field: the recipe will not start until the chamber
 * has reached the required DEPTH. Depth accumulates over minutes of steady heat supply, so a stage
 * is not "crafting difficulty" but a state of the installation the player pays for with time.
 * <p>
 * The output can be an item, a fluid or both: the first stage gives items,
 * the third gives superfluid condensate.
 */
public class CryoRecipe implements Recipe<ContainerInput> {

    private final Ingredient ingredient;
    private final int ingredientCount;
    private final int requiredStage;
    private final ItemStack result;
    private final ResourceLocation fluidResultId;
    private final int fluidResultAmount;
    private final int processingTime;
    private final int energyCostPerTick;

    public CryoRecipe(Ingredient ingredient, int ingredientCount, int requiredStage,
                      ItemStack result, ResourceLocation fluidResultId, int fluidResultAmount,
                      int processingTime, int energyCostPerTick) {
        this.ingredient        = ingredient;
        this.ingredientCount   = ingredientCount;
        this.requiredStage     = requiredStage;
        this.result            = result;
        this.fluidResultId     = fluidResultId;
        this.fluidResultAmount = fluidResultAmount;
        this.processingTime    = processingTime;
        this.energyCostPerTick = energyCostPerTick;
    }

    public boolean inputMatches(ItemStack stack) {
        return ingredient.test(stack) && stack.getCount() >= ingredientCount;
    }

    public Ingredient getIngredient()      { return ingredient; }
    public int  getIngredientCount()       { return ingredientCount; }
    public int  getRequiredStage()         { return requiredStage; }
    /** The result as is, without a copy, for checks in the tick. Do not modify it. */
    public ItemStack getResult()           { return result; }
    public boolean hasItemResult()         { return !result.isEmpty(); }
    public boolean hasFluidResult()        { return fluidResultAmount > 0 && fluidResultId != null; }
    public int  getProcessingTime()        { return processingTime; }
    public int  getEnergyCostPerTick()     { return energyCostPerTick; }
    public int  getFluidResultAmount()     { return fluidResultAmount; }
    public ResourceLocation getFluidResultId() { return fluidResultId; }

    public FluidStack makeFluidResult() {
        if (!hasFluidResult()) return FluidStack.EMPTY;
        var fluid = BuiltInRegistries.FLUID.get(fluidResultId);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, fluidResultAmount);
    }

    @Override public boolean matches(ContainerInput c, Level level) { return !level.isClientSide() && inputMatches(c.getItem(0)); }
    @Override public ItemStack assemble(ContainerInput c, HolderLookup.Provider a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider a) { return result.copy(); }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient);
        return list;
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.CRYO_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.CRYO_FREEZING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<CryoRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<CryoRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                Codec.INT.optionalFieldOf("ingredientCount", 1).forGetter(r -> r.ingredientCount),
                Codec.INT.optionalFieldOf("requiredStage", 1).forGetter(r -> r.requiredStage),
                RecipeCodecs.optionalResult("result").forGetter(r -> r.result),
                ResourceLocation.CODEC.optionalFieldOf("fluidResult").forGetter(r -> java.util.Optional.ofNullable(r.fluidResultId)),
                Codec.INT.optionalFieldOf("fluidResultAmount", 0).forGetter(r -> r.fluidResultAmount),
                Codec.INT.optionalFieldOf("processingTime", 400).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("energyCostPerTick", 100).forGetter(r -> r.energyCostPerTick)
        ).apply(i, (ingredient, count, stage, result, fluidId, fluidAmount, time, energy) ->
                new CryoRecipe(ingredient, count, stage, result, fluidId.orElse(null), fluidAmount, time, energy)));

        private static final StreamCodec<RegistryFriendlyByteBuf, CryoRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<CryoRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, CryoRecipe> streamCodec() { return STREAM_CODEC; }

        private static CryoRecipe read(RegistryFriendlyByteBuf buf) {
            Ingredient ingredient = RecipeCodecs.readIngredient(buf);
            int count   = buf.readInt();
            int stage   = buf.readInt();
            ItemStack result = RecipeCodecs.readItem(buf);
            ResourceLocation fluidId = buf.readBoolean() ? buf.readResourceLocation() : null;
            int fluidAmount = buf.readInt();
            int time    = buf.readInt();
            int energy  = buf.readInt();
            return new CryoRecipe(ingredient, count, stage, result, fluidId, fluidAmount, time, energy);
        }

        private static void write(RegistryFriendlyByteBuf buf, CryoRecipe r) {
            RecipeCodecs.writeIngredient(buf, r.ingredient);
            buf.writeInt(r.ingredientCount);
            buf.writeInt(r.requiredStage);
            RecipeCodecs.writeItem(buf, r.result);
            buf.writeBoolean(r.fluidResultId != null);
            if (r.fluidResultId != null) buf.writeResourceLocation(r.fluidResultId);
            buf.writeInt(r.fluidResultAmount);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
        }
    }
}
