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

public class GrowingRecipe implements Recipe<ContainerInput> {

    public enum Category { PLANT, MINERAL }

    private final Ingredient input;
    private final int inputCount;
    private final ItemStack output;
    private final int time;
    private final int energy;
    private final Category category;
    private final boolean keepInput;

    public GrowingRecipe(Ingredient input, int inputCount, ItemStack output,
                         int time, int energy, Category category, boolean keepInput) {
        this.input = input;
        this.inputCount = inputCount;
        this.output = output;
        this.time = time;
        this.energy = energy;
        this.category = category;
        this.keepInput = keepInput;
    }

    @Override
    public boolean matches(ContainerInput container, Level level) {
        if (level.isClientSide()) return false;
        ItemStack s = container.getItem(0);
        return input.test(s) && s.getCount() >= inputCount;
    }

    @Override public ItemStack assemble(ContainerInput c, HolderLookup.Provider a) { return output.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider a) { return output; }

    public Ingredient getInput()    { return input; }
    public int       getInputCount(){ return inputCount; }
    public ItemStack getOutput()    { return output; }
    public int       getTime()      { return time; }
    public int       getEnergy()    { return energy; }
    public Category  getCategory()  { return category; }
    public boolean   keepInput()    { return keepInput; }

    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(input);
        return list;
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.GROWING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.GROWING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<GrowingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        /** "plant" / "mineral", any case. */
        private static final Codec<Category> CATEGORY = Codec.STRING.comapFlatMap(s -> {
            try { return com.mojang.serialization.DataResult.success(Category.valueOf(s.toUpperCase(java.util.Locale.ROOT))); }
            catch (IllegalArgumentException e) { return com.mojang.serialization.DataResult.error(() -> "Unknown growing category: " + s); }
        }, c -> c.name().toLowerCase(java.util.Locale.ROOT));

        private static final MapCodec<GrowingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(r -> r.input),
                Codec.INT.optionalFieldOf("inputCount", 1).forGetter(r -> r.inputCount),
                ItemStack.CODEC.fieldOf("output").forGetter(r -> r.output),
                Codec.INT.optionalFieldOf("time", 100).forGetter(r -> r.time),
                Codec.INT.optionalFieldOf("energy", 200).forGetter(r -> r.energy),
                CATEGORY.optionalFieldOf("category", Category.MINERAL).forGetter(r -> r.category),
                Codec.BOOL.optionalFieldOf("keepInput", true).forGetter(r -> r.keepInput)
        ).apply(i, GrowingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, GrowingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<GrowingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, GrowingRecipe> streamCodec() { return STREAM_CODEC; }

        private static GrowingRecipe read(RegistryFriendlyByteBuf buf) {
            Ingredient input = RecipeCodecs.readIngredient(buf);
            int inputCount    = buf.readInt();
            ItemStack output  = RecipeCodecs.readItem(buf);
            int time          = buf.readInt();
            int energy        = buf.readInt();
            Category category = buf.readEnum(Category.class);
            boolean keepInput = buf.readBoolean();
            return new GrowingRecipe(input, inputCount, output, time, energy, category, keepInput);
        }

        private static void write(RegistryFriendlyByteBuf buf, GrowingRecipe r) {
            RecipeCodecs.writeIngredient(buf, r.input);
            buf.writeInt(r.inputCount);
            RecipeCodecs.writeItem(buf, r.output);
            buf.writeInt(r.time);
            buf.writeInt(r.energy);
            buf.writeEnum(r.category);
            buf.writeBoolean(r.keepInput);
        }
    }
}
