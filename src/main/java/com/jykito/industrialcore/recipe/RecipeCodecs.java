package com.jykito.industrialcore.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Codec pieces shared by the mod's recipe types. They read exactly the json the 1.20.1 serializers read,
 * so datapacks and the recipe files did not change shape (only results moved from "item" to "id").
 */
public final class RecipeCodecs {
    private RecipeCodecs() {}

    /** A fluid id; an unknown id becomes the empty fluid, as the old {@code BuiltInRegistries.FLUID.get} did. */
    public static final Codec<Fluid> FLUID_ID = ResourceLocation.CODEC.xmap(
            BuiltInRegistries.FLUID::get, BuiltInRegistries.FLUID::getKey);

    /** {@code {"fluid": id, "amount": mB}}, amount 1000 by default (the 1.20.1 shape). */
    private static final Codec<FluidStack> FLUID_OBJECT_LEGACY = RecordCodecBuilder.create(i -> i.group(
            FLUID_ID.fieldOf("fluid").forGetter(FluidStack::getFluid),
            Codec.INT.optionalFieldOf("amount", 1000).forGetter(FluidStack::getAmount)
    ).apply(i, (fluid, amount) -> fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount)));

    /**
     * Reads both the 1.21 fluid stack {@code {"id": id, "amount": mB}} and {@code {"fluid": id, "amount": mB}} (1.20.1
     * datapacks); writes the first. KubeJS 7 reads and writes recipes only in the 1.21 shape, so the built-in recipes
     * use it too.
     */
    public static final Codec<FluidStack> FLUID_OBJECT = Codec.withAlternative(FluidStack.CODEC, FLUID_OBJECT_LEGACY);

    /** Optional {@link #FLUID_OBJECT} field, empty when absent. */
    public static MapCodec<FluidStack> optionalFluid(String key) {
        return FLUID_OBJECT.optionalFieldOf(key, FluidStack.EMPTY);
    }

    /** A flat fluid id field plus a separate amount field ({@code "fluidInput": id, "fluidInputAmount": mB}). */
    public static FluidStack flatFluid(Fluid fluid, int amount) {
        return fluid == Fluids.EMPTY || amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    /** Item result, optional: an empty stack when absent. */
    public static MapCodec<ItemStack> optionalResult(String key) {
        return ItemStack.CODEC.optionalFieldOf(key, ItemStack.EMPTY);
    }

    /** Ingredients with a count inside the object: {@code [{"item": ..., "count": 4}, ...]}. */
    public static final Codec<List<SizedIngredient>> COUNTED_INPUTS = SizedIngredient.FLAT_CODEC.listOf();

    public static Ingredient[] ingredients(List<SizedIngredient> list) {
        Ingredient[] out = new Ingredient[list.size()];
        for (int i = 0; i < out.length; i++) out[i] = list.get(i).ingredient();
        return out;
    }

    public static int[] counts(List<SizedIngredient> list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) out[i] = list.get(i).count();
        return out;
    }

    public static List<SizedIngredient> sized(Ingredient[] ingredients, int[] counts) {
        List<SizedIngredient> out = new ArrayList<>(ingredients.length);
        for (int i = 0; i < ingredients.length; i++) out.add(new SizedIngredient(ingredients[i], counts[i]));
        return out;
    }

    // ── network ──

    public static void writeIngredient(RegistryFriendlyByteBuf buf, Ingredient ing) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
    }

    public static Ingredient readIngredient(RegistryFriendlyByteBuf buf) {
        return Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
    }

    public static void writeItem(RegistryFriendlyByteBuf buf, ItemStack stack) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
    }

    public static ItemStack readItem(RegistryFriendlyByteBuf buf) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }

    public static void writeFluid(RegistryFriendlyByteBuf buf, FluidStack fluid) {
        FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, fluid);
    }

    public static FluidStack readFluid(RegistryFriendlyByteBuf buf) {
        return FluidStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }

    public static void writeCounted(RegistryFriendlyByteBuf buf, Ingredient[] ingredients, int[] counts) {
        buf.writeVarInt(ingredients.length);
        for (int i = 0; i < ingredients.length; i++) { writeIngredient(buf, ingredients[i]); buf.writeVarInt(counts[i]); }
    }

    /** Ingredients and their counts, as the recipes keep them. */
    public record Counted(Ingredient[] inputs, int[] counts) {}

    /** Reads what {@link #writeCounted} wrote. */
    public static Counted readCounted(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Ingredient[] ing = new Ingredient[n];
        int[] cnt = new int[n];
        for (int i = 0; i < n; i++) { ing[i] = readIngredient(buf); cnt[i] = buf.readVarInt(); }
        return new Counted(ing, cnt);
    }
}
