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

public class RodFabricatorRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final Ingredient ingredient1;
    private final int count1;
    private final Ingredient ingredient2;
    private final int count2;
    private final ItemStack result;
    private final int processingTime;
    private final int energyCostPerTick;

    public RodFabricatorRecipe(ResourceLocation id,
                         Ingredient ingredient1, int count1,
                         Ingredient ingredient2, int count2,
                         ItemStack result, int processingTime, int energyCostPerTick) {
        this.id               = id;
        this.ingredient1      = ingredient1;
        this.count1           = count1;
        this.ingredient2      = ingredient2;
        this.count2           = count2;
        this.result           = result;
        this.processingTime   = processingTime;
        this.energyCostPerTick = energyCostPerTick;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (level.isClientSide()) return false;
        return ingredient1.test(container.getItem(0)) && container.getItem(0).getCount() >= count1
            && ingredient2.test(container.getItem(1)) && container.getItem(1).getCount() >= count2;
    }

    @Override public ItemStack assemble(SimpleContainer c, RegistryAccess a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(RegistryAccess a) { return result.copy(); }

    public Ingredient getIngredient1()    { return ingredient1; }
    public int        getCount1()         { return count1; }
    public Ingredient getIngredient2()    { return ingredient2; }
    public int        getCount2()         { return count2; }
    public int        getProcessingTime() { return processingTime; }
    public int        getEnergyCostPerTick() { return energyCostPerTick; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient1);
        list.add(ingredient2);
        return list;
    }

    @Override public ResourceLocation getId()           { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ROD_FABRICATION_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()            { return ModRecipes.ROD_FABRICATION_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<RodFabricatorRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public RodFabricatorRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient1    = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient1"));
            int        count1         = GsonHelper.getAsInt(json, "count1", 1);
            Ingredient ingredient2    = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient2"));
            int        count2         = GsonHelper.getAsInt(json, "count2", 1);
            ItemStack  result         = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int        processingTime = GsonHelper.getAsInt(json, "processingTime", 200);
            int        energyCostPerTick = GsonHelper.getAsInt(json, "energyCostPerTick", 500);
            return new RodFabricatorRecipe(id, ingredient1, count1, ingredient2, count2, result, processingTime, energyCostPerTick);
        }

        @Override
        public @Nullable RodFabricatorRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient ingredient1    = Ingredient.fromNetwork(buf);
            int        count1         = buf.readInt();
            Ingredient ingredient2    = Ingredient.fromNetwork(buf);
            int        count2         = buf.readInt();
            ItemStack  result         = buf.readItem();
            int        processingTime = buf.readInt();
            int        energyCostPerTick = buf.readInt();
            return new RodFabricatorRecipe(id, ingredient1, count1, ingredient2, count2, result, processingTime, energyCostPerTick);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RodFabricatorRecipe r) {
            r.ingredient1.toNetwork(buf);
            buf.writeInt(r.count1);
            r.ingredient2.toNetwork(buf);
            buf.writeInt(r.count2);
            buf.writeItem(r.result);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
        }
    }
}
