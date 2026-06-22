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

public class GrowingRecipe implements Recipe<SimpleContainer> {

    public enum Category { PLANT, MINERAL }

    private final ResourceLocation id;
    private final Ingredient input;
    private final int inputCount;
    private final ItemStack output;
    private final int time;
    private final int energy;
    private final Category category;
    private final boolean keepInput;

    public GrowingRecipe(ResourceLocation id, Ingredient input, int inputCount, ItemStack output,
                         int time, int energy, Category category, boolean keepInput) {
        this.id = id;
        this.input = input;
        this.inputCount = inputCount;
        this.output = output;
        this.time = time;
        this.energy = energy;
        this.category = category;
        this.keepInput = keepInput;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (level.isClientSide()) return false;
        ItemStack s = container.getItem(0);
        return input.test(s) && s.getCount() >= inputCount;
    }

    @Override public ItemStack assemble(SimpleContainer c, RegistryAccess a) { return output.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(RegistryAccess a) { return output; }

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

    @Override public ResourceLocation getId()            { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.GROWING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.GROWING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<GrowingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public GrowingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "input"));
            int inputCount    = GsonHelper.getAsInt(json, "inputCount", 1);
            ItemStack output  = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "output"));
            int time          = GsonHelper.getAsInt(json, "time", 100);
            int energy        = GsonHelper.getAsInt(json, "energy", 200);
            Category category = Category.valueOf(GsonHelper.getAsString(json, "category", "mineral").toUpperCase());
            boolean keepInput = GsonHelper.getAsBoolean(json, "keepInput", true);
            return new GrowingRecipe(id, input, inputCount, output, time, energy, category, keepInput);
        }

        @Override
        public @Nullable GrowingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            int inputCount    = buf.readInt();
            ItemStack output  = buf.readItem();
            int time          = buf.readInt();
            int energy        = buf.readInt();
            Category category = buf.readEnum(Category.class);
            boolean keepInput = buf.readBoolean();
            return new GrowingRecipe(id, input, inputCount, output, time, energy, category, keepInput);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, GrowingRecipe r) {
            r.input.toNetwork(buf);
            buf.writeInt(r.inputCount);
            buf.writeItem(r.output);
            buf.writeInt(r.time);
            buf.writeInt(r.energy);
            buf.writeEnum(r.category);
            buf.writeBoolean(r.keepInput);
        }
    }
}
