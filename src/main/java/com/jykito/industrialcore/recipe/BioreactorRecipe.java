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

/**
 * Bioreactor recipe (E3): biomass + additive + water -> organics usually dropped by mobs.
 * The culture only grows in heat: heat is not spent, but below {@code requiredHeat} the reactor stands.
 */
public class BioreactorRecipe implements Recipe<ContainerInput> {

    private final Ingredient input, additive;
    private final int inputCount, additiveCount, waterAmount;
    private final ItemStack result;
    private final int processingTime, energyCostPerTick, requiredHeat;

    public BioreactorRecipe(Ingredient input, int inputCount, Ingredient additive, int additiveCount,
                            int waterAmount, ItemStack result, int processingTime, int energyCostPerTick, int requiredHeat) {
        this.input = input;
        this.inputCount = inputCount;
        this.additive = additive;
        this.additiveCount = additiveCount;
        this.waterAmount = waterAmount;
        this.result = result;
        this.processingTime = processingTime;
        this.energyCostPerTick = energyCostPerTick;
        this.requiredHeat = requiredHeat;
    }

    /** Whether the biomass and additive fit (the machine checks water and heat). */
    public boolean matches(ItemStack in, ItemStack add) {
        return input.test(in) && in.getCount() >= inputCount
                && (additive.isEmpty() ? add.isEmpty() || additiveCount == 0 : additive.test(add) && add.getCount() >= additiveCount);
    }

    @Override
    public boolean matches(ContainerInput container, Level level) {
        return !level.isClientSide() && matches(container.getItem(0), container.getItem(1));
    }

    @Override public ItemStack assemble(ContainerInput c, HolderLookup.Provider a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider a) { return result.copy(); }

    public Ingredient getInput()          { return input; }
    public int        getInputCount()     { return inputCount; }
    public Ingredient getAdditive()       { return additive; }
    public int        getAdditiveCount()  { return additiveCount; }
    public boolean    hasAdditive()       { return !additive.isEmpty() && additiveCount > 0; }
    public int        getWaterAmount()    { return waterAmount; }
    public ItemStack  getResult()         { return result.copy(); }
    public int        getProcessingTime() { return processingTime; }
    public int        getEnergyCostPerTick() { return energyCostPerTick; }
    public int        getRequiredHeat()   { return requiredHeat; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(input);
        if (hasAdditive()) list.add(additive);
        return list;
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.BIOREACTOR_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.BIOREACTOR_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<BioreactorRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<BioreactorRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.input),
                Codec.INT.optionalFieldOf("inputCount", 1).forGetter(r -> r.inputCount),
                Ingredient.CODEC_NONEMPTY.optionalFieldOf("additive", Ingredient.EMPTY).forGetter(r -> r.additive),
                Codec.INT.optionalFieldOf("additiveCount", 1).forGetter(r -> r.additiveCount),
                Codec.INT.optionalFieldOf("waterAmount", 250).forGetter(r -> r.waterAmount),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("processingTime", 400).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("energyCostPerTick", 60).forGetter(r -> r.energyCostPerTick),
                Codec.INT.optionalFieldOf("requiredHeat", 800).forGetter(r -> r.requiredHeat)
        ).apply(i, BioreactorRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BioreactorRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<BioreactorRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, BioreactorRecipe> streamCodec() { return STREAM_CODEC; }

        private static BioreactorRecipe read(RegistryFriendlyByteBuf buf) {
            Ingredient input = RecipeCodecs.readIngredient(buf);
            int inputCount = buf.readInt();
            Ingredient additive = RecipeCodecs.readIngredient(buf);
            int additiveCount = buf.readInt();
            int waterAmount = buf.readInt();
            ItemStack result = RecipeCodecs.readItem(buf);
            int processingTime = buf.readInt(), energyCost = buf.readInt(), requiredHeat = buf.readInt();
            return new BioreactorRecipe(input, inputCount, additive, additiveCount, waterAmount, result, processingTime, energyCost, requiredHeat);
        }

        private static void write(RegistryFriendlyByteBuf buf, BioreactorRecipe r) {
            RecipeCodecs.writeIngredient(buf, r.input);
            buf.writeInt(r.inputCount);
            RecipeCodecs.writeIngredient(buf, r.additive);
            buf.writeInt(r.additiveCount);
            buf.writeInt(r.waterAmount);
            RecipeCodecs.writeItem(buf, r.result);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
            buf.writeInt(r.requiredHeat);
        }
    }
}
