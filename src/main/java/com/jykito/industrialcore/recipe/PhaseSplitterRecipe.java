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

public class PhaseSplitterRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final Ingredient input;
    private final int inputCount;
    private final ItemStack output1;
    private final ItemStack output2;
    private final ItemStack output3;
    private final int processingTime;
    private final int energyCost;

    public PhaseSplitterRecipe(ResourceLocation id, Ingredient input, int inputCount,
                                ItemStack output1, ItemStack output2, ItemStack output3,
                                int processingTime, int energyCost) {
        this.id = id;
        this.input = input;
        this.inputCount = inputCount;
        this.output1 = output1;
        this.output2 = output2;
        this.output3 = output3;
        this.processingTime = processingTime;
        this.energyCost = energyCost;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (level.isClientSide()) return false;
        return input.test(container.getItem(0)) && container.getItem(0).getCount() >= inputCount;
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess access) { return output1.copy(); }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override public boolean isSpecial() { return true; }

    @Override
    public ItemStack getResultItem(RegistryAccess access) { return output1.copy(); }

    public ItemStack getOutput1() { return output1.copy(); }
    public ItemStack getOutput2() { return output2.copy(); }
    public ItemStack getOutput3() { return output3.copy(); }
    public int getInputCount() { return inputCount; }
    public int getProcessingTime() { return processingTime; }
    public int getEnergyCost() { return energyCost; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(input);
        return list;
    }

    @Override
    public ResourceLocation getId() { return id; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipes.PHASE_SPLITTER_SERIALIZER.get(); }

    @Override
    public RecipeType<?> getType() { return ModRecipes.PHASE_SPLITTING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<PhaseSplitterRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public PhaseSplitterRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            int inputCount = GsonHelper.getAsInt(json, "inputCount", 1);

            ItemStack out1 = json.has("result")  ? ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"))  : ItemStack.EMPTY;
            ItemStack out2 = json.has("result2") ? ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result2")) : ItemStack.EMPTY;
            ItemStack out3 = json.has("result3") ? ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result3")) : ItemStack.EMPTY;

            int processingTime = GsonHelper.getAsInt(json, "processingTime", 400);
            int energyCost     = GsonHelper.getAsInt(json, "energyCost", 100);

            return new PhaseSplitterRecipe(id, input, inputCount, out1, out2, out3, processingTime, energyCost);
        }

        @Override
        public @Nullable PhaseSplitterRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            int inputCount   = buf.readInt();
            ItemStack out1   = buf.readItem();
            ItemStack out2   = buf.readItem();
            ItemStack out3   = buf.readItem();
            int processingTime = buf.readInt();
            int energyCost     = buf.readInt();
            return new PhaseSplitterRecipe(id, input, inputCount, out1, out2, out3, processingTime, energyCost);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, PhaseSplitterRecipe r) {
            r.input.toNetwork(buf);
            buf.writeInt(r.inputCount);
            buf.writeItem(r.output1);
            buf.writeItem(r.output2);
            buf.writeItem(r.output3);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCost);
        }
    }
}
