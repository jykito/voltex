package com.jykito.industrialcore.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class MachineTypeRecipeSerializer implements RecipeSerializer<MachineRecipe> {

    private final Supplier<RecipeType<?>> type;

    public MachineTypeRecipeSerializer(Supplier<RecipeType<?>> type) {
        this.type = type;
    }

    @Override
    public MachineRecipe fromJson(ResourceLocation id, JsonObject json) {
        Ingredient input   = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
        int inputCount     = GsonHelper.getAsInt(json, "inputCount", 1);
        ItemStack output   = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
        int processingTime = GsonHelper.getAsInt(json, "processingTime", 200);
        int energyCost     = GsonHelper.getAsInt(json, "energyCost", 50);
        String catalystId  = json.has("catalyst") ? GsonHelper.getAsString(json, "catalyst") : null;
        return new MachineRecipe(id, input, inputCount, output, processingTime, energyCost, catalystId, type.get());
    }

    @Override
    public @Nullable MachineRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        Ingredient input   = Ingredient.fromNetwork(buf);
        int inputCount     = buf.readInt();
        ItemStack output   = buf.readItem();
        int processingTime = buf.readInt();
        int energyCost     = buf.readInt();
        String catalystId  = buf.readBoolean() ? buf.readUtf() : null;
        return new MachineRecipe(id, input, inputCount, output, processingTime, energyCost, catalystId, type.get());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, MachineRecipe recipe) {
        recipe.getIngredients().get(0).toNetwork(buf);
        buf.writeInt(recipe.getInputCount());
        buf.writeItem(recipe.getResultItem(null));
        buf.writeInt(recipe.getProcessingTime());
        buf.writeInt(recipe.getEnergyCost());
        String cat = recipe.getCatalystId();
        buf.writeBoolean(cat != null);
        if (cat != null) buf.writeUtf(cat);
    }
}
