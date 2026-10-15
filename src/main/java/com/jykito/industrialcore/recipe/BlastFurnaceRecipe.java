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

public class BlastFurnaceRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    /** Recipe for the late machine only (its industrial mode); the small machine does not see it. */
    private boolean lateOnly;
    private final Ingredient ingredient;
    private final int ingredientCount;
    private final Ingredient fuel;
    private final int fuelCount;
    private final ItemStack result;
    private final int processingTime;
    private final int requiredHeat;

    public BlastFurnaceRecipe(ResourceLocation id,
                               Ingredient ingredient, int ingredientCount,
                               Ingredient fuel, int fuelCount,
                               ItemStack result, int processingTime, int requiredHeat) {
        this.id              = id;
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
    public boolean matches(SimpleContainer container, Level level) {
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

    @Override public ItemStack assemble(SimpleContainer c, RegistryAccess a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(RegistryAccess a) { return result.copy(); }

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

    @Override public ResourceLocation getId()           { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.BLAST_FURNACE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()            { return ModRecipes.BLAST_FURNACE_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<BlastFurnaceRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public BlastFurnaceRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient     = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            int        ingredientCount = GsonHelper.getAsInt(json, "ingredientCount", 1);
            Ingredient fuel           = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "fuel"));
            int        fuelCount      = GsonHelper.getAsInt(json, "fuelCount", 1);
            ItemStack  result         = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int        processingTime = GsonHelper.getAsInt(json, "processingTime", 300);
            int        requiredHeat   = GsonHelper.getAsInt(json, "requiredHeat", 2400);
            BlastFurnaceRecipe recipe__ = new BlastFurnaceRecipe(id, ingredient, ingredientCount, fuel, fuelCount, result, processingTime, requiredHeat);
            recipe__.lateOnly = GsonHelper.getAsBoolean(json, "lateOnly", false);
            return recipe__;
        }

        @Override
        public @Nullable BlastFurnaceRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient ingredient     = Ingredient.fromNetwork(buf);
            int        ingredientCount = buf.readInt();
            Ingredient fuel           = Ingredient.fromNetwork(buf);
            int        fuelCount      = buf.readInt();
            ItemStack  result         = buf.readItem();
            int        processingTime = buf.readInt();
            int        requiredHeat   = buf.readInt();
            BlastFurnaceRecipe recipe__ = new BlastFurnaceRecipe(id, ingredient, ingredientCount, fuel, fuelCount, result, processingTime, requiredHeat);
            recipe__.lateOnly = buf.readBoolean();
            return recipe__;
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, BlastFurnaceRecipe r) {
            r.ingredient.toNetwork(buf);
            buf.writeInt(r.ingredientCount);
            r.fuel.toNetwork(buf);
            buf.writeInt(r.fuelCount);
            buf.writeItem(r.result);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.requiredHeat);
            buf.writeBoolean(r.lateOnly);
        }
    }
}
