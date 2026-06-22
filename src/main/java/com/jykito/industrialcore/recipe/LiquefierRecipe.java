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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class LiquefierRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final int ingredientCount;
    private final Ingredient ingredient2;
    private final int ingredientCount2;
    private final int requiredHeat;
    private final ResourceLocation fluidOutputId;
    private final int fluidOutputAmount;
    private final int processingTime;
    private final int energyCostPerTick;

    public LiquefierRecipe(ResourceLocation id,
                           Ingredient ingredient, int ingredientCount,
                           Ingredient ingredient2, int ingredientCount2,
                           int requiredHeat,
                           ResourceLocation fluidOutputId, int fluidOutputAmount,
                           int processingTime, int energyCostPerTick) {
        this.id                = id;
        this.ingredient        = ingredient;
        this.ingredientCount   = ingredientCount;
        this.ingredient2       = ingredient2;
        this.ingredientCount2  = ingredientCount2;
        this.requiredHeat      = requiredHeat;
        this.fluidOutputId     = fluidOutputId;
        this.fluidOutputAmount = fluidOutputAmount;
        this.processingTime    = processingTime;
        this.energyCostPerTick = energyCostPerTick;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (level.isClientSide()) return false;
        return inputsMatch(container.getItem(0), container.getItem(1));
    }

    public boolean hasSecondIngredient() { return !ingredient2.isEmpty(); }

    public boolean inputsMatch(ItemStack s0, ItemStack s1) {
        if (!hasSecondIngredient()) {
            return (ingredient.test(s0) && s0.getCount() >= ingredientCount)
                || (ingredient.test(s1) && s1.getCount() >= ingredientCount);
        }
        return inputPair(s0, s1) || inputPair(s1, s0);
    }

    public boolean inputPair(ItemStack slotA, ItemStack slotB) {
        return ingredient.test(slotA)  && slotA.getCount() >= ingredientCount
            && ingredient2.test(slotB) && slotB.getCount() >= ingredientCount2;
    }

    @Override public ItemStack assemble(SimpleContainer c, RegistryAccess a) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(RegistryAccess a) { return ItemStack.EMPTY; }

    public FluidStack makeFluidOutput() {
        var fluid = ForgeRegistries.FLUIDS.getValue(fluidOutputId);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, fluidOutputAmount);
    }

    public Ingredient getIngredient()        { return ingredient; }
    public int        getIngredientCount()   { return ingredientCount; }
    public Ingredient getIngredient2()       { return ingredient2; }
    public int        getIngredientCount2()  { return ingredientCount2; }
    public int        getRequiredHeat()      { return requiredHeat; }
    public int        getProcessingTime()    { return processingTime; }
    public int        getEnergyCostPerTick() { return energyCostPerTick; }
    public int        getFluidOutputAmount() { return fluidOutputAmount; }
    public ResourceLocation getFluidOutputId() { return fluidOutputId; }

    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient);
        if (hasSecondIngredient()) list.add(ingredient2);
        return list;
    }

    @Override public ResourceLocation getId()            { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.LIQUEFIER_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.LIQUEFYING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<LiquefierRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public LiquefierRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient   = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            int ingredientCount     = GsonHelper.getAsInt(json, "itemCount", 1);

            Ingredient ingredient2  = Ingredient.EMPTY;
            int ingredientCount2    = 0;
            if (json.has("ingredient2")) {
                ingredient2      = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient2"));
                ingredientCount2 = GsonHelper.getAsInt(json, "itemCount2", 1);
            }
            int requiredHeat        = GsonHelper.getAsInt(json, "requiredHeat", 1000);
            ResourceLocation fluidOut = new ResourceLocation(GsonHelper.getAsString(json, "fluid"));
            int fluidAmount         = GsonHelper.getAsInt(json, "fluidAmount", 1000);
            int processingTime      = GsonHelper.getAsInt(json, "processingTime", 200);
            int energyCostPerTick   = GsonHelper.getAsInt(json, "energyCostPerTick", 40);
            return new LiquefierRecipe(id, ingredient, ingredientCount, ingredient2, ingredientCount2,
                    requiredHeat, fluidOut, fluidAmount, processingTime, energyCostPerTick);
        }

        @Override
        public @Nullable LiquefierRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient ingredient   = Ingredient.fromNetwork(buf);
            int ingredientCount     = buf.readInt();
            Ingredient ingredient2  = Ingredient.fromNetwork(buf);
            int ingredientCount2    = buf.readInt();
            int requiredHeat        = buf.readInt();
            ResourceLocation fluidOut = buf.readResourceLocation();
            int fluidAmount         = buf.readInt();
            int processingTime      = buf.readInt();
            int energyCostPerTick   = buf.readInt();
            return new LiquefierRecipe(id, ingredient, ingredientCount, ingredient2, ingredientCount2,
                    requiredHeat, fluidOut, fluidAmount, processingTime, energyCostPerTick);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, LiquefierRecipe r) {
            r.ingredient.toNetwork(buf);
            buf.writeInt(r.ingredientCount);
            r.ingredient2.toNetwork(buf);
            buf.writeInt(r.ingredientCount2);
            buf.writeInt(r.requiredHeat);
            buf.writeResourceLocation(r.fluidOutputId);
            buf.writeInt(r.fluidOutputAmount);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
        }
    }
}
