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

/**
 * Bioreactor recipe (E3): biomass + additive + water -> organics usually dropped by mobs.
 * The culture only grows in heat: heat is not spent, but below {@code requiredHeat} the reactor stands.
 */
public class BioreactorRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final Ingredient input, additive;
    private final int inputCount, additiveCount, waterAmount;
    private final ItemStack result;
    private final int processingTime, energyCostPerTick, requiredHeat;

    public BioreactorRecipe(ResourceLocation id, Ingredient input, int inputCount, Ingredient additive, int additiveCount,
                            int waterAmount, ItemStack result, int processingTime, int energyCostPerTick, int requiredHeat) {
        this.id = id;
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
    public boolean matches(SimpleContainer container, Level level) {
        return !level.isClientSide() && matches(container.getItem(0), container.getItem(1));
    }

    @Override public ItemStack assemble(SimpleContainer c, RegistryAccess a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(RegistryAccess a) { return result.copy(); }

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

    @Override public ResourceLocation getId()            { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.BIOREACTOR_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.BIOREACTOR_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<BioreactorRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public BioreactorRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input   = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            int inputCount     = GsonHelper.getAsInt(json, "inputCount", 1);
            Ingredient additive = json.has("additive") ? Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "additive")) : Ingredient.EMPTY;
            int additiveCount  = GsonHelper.getAsInt(json, "additiveCount", 1);
            int waterAmount    = GsonHelper.getAsInt(json, "waterAmount", 250);
            ItemStack result   = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int processingTime = GsonHelper.getAsInt(json, "processingTime", 400);
            int energyCost     = GsonHelper.getAsInt(json, "energyCostPerTick", 60);
            int requiredHeat   = GsonHelper.getAsInt(json, "requiredHeat", 800);
            return new BioreactorRecipe(id, input, inputCount, additive, additiveCount, waterAmount, result, processingTime, energyCost, requiredHeat);
        }

        @Override
        public @Nullable BioreactorRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            int inputCount = buf.readInt();
            Ingredient additive = Ingredient.fromNetwork(buf);
            int additiveCount = buf.readInt();
            int waterAmount = buf.readInt();
            ItemStack result = buf.readItem();
            int processingTime = buf.readInt(), energyCost = buf.readInt(), requiredHeat = buf.readInt();
            return new BioreactorRecipe(id, input, inputCount, additive, additiveCount, waterAmount, result, processingTime, energyCost, requiredHeat);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, BioreactorRecipe r) {
            r.input.toNetwork(buf);
            buf.writeInt(r.inputCount);
            r.additive.toNetwork(buf);
            buf.writeInt(r.additiveCount);
            buf.writeInt(r.waterAmount);
            buf.writeItem(r.result);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
            buf.writeInt(r.requiredHeat);
        }
    }
}
