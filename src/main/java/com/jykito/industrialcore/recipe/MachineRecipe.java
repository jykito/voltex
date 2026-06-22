package com.jykito.industrialcore.recipe;

import com.google.gson.JsonObject;
import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class MachineRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final Ingredient input;
    private final int inputCount;
    private final ItemStack output;
    private final int processingTime;
    private final int energyCost;
    @Nullable private final String catalystId;
    private final RecipeType<?> type;

    public MachineRecipe(ResourceLocation id, Ingredient input, int inputCount, ItemStack output,
                         int processingTime, int energyCost, @Nullable String catalystId, RecipeType<?> type) {
        this.id = id;
        this.input = input;
        this.inputCount = inputCount;
        this.output = output;
        this.processingTime = processingTime;
        this.energyCost = energyCost;
        this.catalystId = catalystId;
        this.type = type;
    }

    @Override
    public boolean matches(SimpleContainer pContainer, Level pLevel) {
        if (pLevel.isClientSide()) return false;
        if (!input.test(pContainer.getItem(0)) || pContainer.getItem(0).getCount() < inputCount) return false;
        if (catalystId == null) return true;
        ItemStack cat = pContainer.getContainerSize() > 1 ? pContainer.getItem(1) : ItemStack.EMPTY;
        if (cat.isEmpty()) return false;
        ResourceLocation catKey = ForgeRegistries.ITEMS.getKey(cat.getItem());
        return catKey != null && catKey.toString().equals(catalystId);
    }

    @Nullable public String getCatalystId() { return catalystId; }

    @Override
    public ItemStack assemble(SimpleContainer pContainer, RegistryAccess pRegistryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override public boolean isSpecial() { return true; }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        return output.copy();
    }

    public int getProcessingTime() { return processingTime; }
    public int getEnergyCost() { return energyCost; }
    public int getInputCount() { return inputCount; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.input);
        return list;
    }

    @Override
    public ResourceLocation getId() { return id; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipes.MACHINE_SERIALIZER.get(); }

    @Override
    public RecipeType<?> getType() { return type; }

    public static class Serializer implements RecipeSerializer<MachineRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public MachineRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            String typeStr = GsonHelper.getAsString(pSerializedRecipe, "machine_type");

            RecipeType<?> recipeType = null;
            for (var entry : ModRecipes.RECIPE_TYPES.getEntries()) {
                if (entry.getId().toString().equals(typeStr)) {
                    recipeType = entry.get();
                    break;
                }
            }
            if (recipeType == null) {
                throw new com.google.gson.JsonSyntaxException("Unknown machine_type: " + typeStr + " (recipe " + pRecipeId + ")");
            }

            Ingredient input = Ingredient.fromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "ingredient"));
            int inputCount = GsonHelper.getAsInt(pSerializedRecipe, "inputCount", 1);
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "result"));
            int processingTime = GsonHelper.getAsInt(pSerializedRecipe, "processingTime", 200);
            int energyCost = GsonHelper.getAsInt(pSerializedRecipe, "energyCost", 50);
            String catalystId = pSerializedRecipe.has("catalyst")
                    ? GsonHelper.getAsString(pSerializedRecipe, "catalyst") : null;

            return new MachineRecipe(pRecipeId, input, inputCount, output, processingTime, energyCost, catalystId, recipeType);
        }

        @Override
        public @Nullable MachineRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            Ingredient input = Ingredient.fromNetwork(pBuffer);
            int inputCount = pBuffer.readInt();
            ItemStack output = pBuffer.readItem();
            int processingTime = pBuffer.readInt();
            int energyCost = pBuffer.readInt();
            String catalystId = pBuffer.readBoolean() ? pBuffer.readUtf() : null;
            String typeStr = pBuffer.readUtf();

            RecipeType<?> recipeType = ModRecipes.CRUSHING_TYPE.get();
            for (var entry : ModRecipes.RECIPE_TYPES.getEntries()) {
                if (entry.getId().toString().equals(typeStr)) {
                    recipeType = entry.get();
                    break;
                }
            }

            return new MachineRecipe(pRecipeId, input, inputCount, output, processingTime, energyCost, catalystId, recipeType);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, MachineRecipe pRecipe) {
            pRecipe.input.toNetwork(pBuffer);
            pBuffer.writeInt(pRecipe.inputCount);
            pBuffer.writeItem(pRecipe.output);
            pBuffer.writeInt(pRecipe.processingTime);
            pBuffer.writeInt(pRecipe.energyCost);

            pBuffer.writeBoolean(pRecipe.catalystId != null);
            if (pRecipe.catalystId != null) pBuffer.writeUtf(pRecipe.catalystId);

            ResourceLocation typeId = ModRecipes.RECIPE_TYPES.getEntries().stream()
                    .filter(e -> e.get() == pRecipe.getType())
                    .findFirst().get().getId();
            pBuffer.writeUtf(typeId.toString());
        }
    }
}
