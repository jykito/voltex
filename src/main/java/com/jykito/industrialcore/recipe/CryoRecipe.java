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

/**
 * Рецепт криокамеры.
 * <p>
 * Отличие от всех прочих типов — поле {@code requiredStage}: рецепт не запустится, пока камера
 * не набрала нужную ГЛУБИНУ. Глубина копится минутами устойчивой подачи тепла, поэтому ступень
 * это не «сложность крафта», а состояние установки, за которое игрок платит временем.
 * <p>
 * Выход может быть предметом, жидкостью или тем и другим: первая ступень даёт предметы,
 * третья — сверхтекучий конденсат.
 */
public class CryoRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final int ingredientCount;
    private final int requiredStage;
    private final ItemStack result;
    private final ResourceLocation fluidResultId;
    private final int fluidResultAmount;
    private final int processingTime;
    private final int energyCostPerTick;

    public CryoRecipe(ResourceLocation id, Ingredient ingredient, int ingredientCount, int requiredStage,
                      ItemStack result, ResourceLocation fluidResultId, int fluidResultAmount,
                      int processingTime, int energyCostPerTick) {
        this.id                = id;
        this.ingredient        = ingredient;
        this.ingredientCount   = ingredientCount;
        this.requiredStage     = requiredStage;
        this.result            = result;
        this.fluidResultId     = fluidResultId;
        this.fluidResultAmount = fluidResultAmount;
        this.processingTime    = processingTime;
        this.energyCostPerTick = energyCostPerTick;
    }

    public boolean inputMatches(ItemStack stack) {
        return ingredient.test(stack) && stack.getCount() >= ingredientCount;
    }

    public Ingredient getIngredient()      { return ingredient; }
    public int  getIngredientCount()       { return ingredientCount; }
    public int  getRequiredStage()         { return requiredStage; }
    /** Результат как есть, без копии — для проверок в тике. Менять его нельзя. */
    public ItemStack getResult()           { return result; }
    public boolean hasItemResult()         { return !result.isEmpty(); }
    public boolean hasFluidResult()        { return fluidResultAmount > 0 && fluidResultId != null; }
    public int  getProcessingTime()        { return processingTime; }
    public int  getEnergyCostPerTick()     { return energyCostPerTick; }
    public int  getFluidResultAmount()     { return fluidResultAmount; }
    public ResourceLocation getFluidResultId() { return fluidResultId; }

    public FluidStack makeFluidResult() {
        if (!hasFluidResult()) return FluidStack.EMPTY;
        var fluid = ForgeRegistries.FLUIDS.getValue(fluidResultId);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, fluidResultAmount);
    }

    @Override public boolean matches(SimpleContainer c, Level level) { return !level.isClientSide() && inputMatches(c.getItem(0)); }
    @Override public ItemStack assemble(SimpleContainer c, RegistryAccess a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(RegistryAccess a) { return result.copy(); }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient);
        return list;
    }

    @Override public ResourceLocation getId()            { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.CRYO_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.CRYO_FREEZING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<CryoRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public CryoRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            int count             = GsonHelper.getAsInt(json, "ingredientCount", 1);
            int stage             = GsonHelper.getAsInt(json, "requiredStage", 1);

            ItemStack result = json.has("result")
                    ? ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"))
                    : ItemStack.EMPTY;

            ResourceLocation fluidId = json.has("fluidResult")
                    ? new ResourceLocation(GsonHelper.getAsString(json, "fluidResult")) : null;
            int fluidAmount = GsonHelper.getAsInt(json, "fluidResultAmount", 0);

            int time   = GsonHelper.getAsInt(json, "processingTime", 400);
            int energy = GsonHelper.getAsInt(json, "energyCostPerTick", 100);
            return new CryoRecipe(id, ingredient, count, stage, result, fluidId, fluidAmount, time, energy);
        }

        @Override
        public @Nullable CryoRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient ingredient = Ingredient.fromNetwork(buf);
            int count   = buf.readInt();
            int stage   = buf.readInt();
            ItemStack result = buf.readItem();
            ResourceLocation fluidId = buf.readBoolean() ? buf.readResourceLocation() : null;
            int fluidAmount = buf.readInt();
            int time    = buf.readInt();
            int energy  = buf.readInt();
            return new CryoRecipe(id, ingredient, count, stage, result, fluidId, fluidAmount, time, energy);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, CryoRecipe r) {
            r.ingredient.toNetwork(buf);
            buf.writeInt(r.ingredientCount);
            buf.writeInt(r.requiredStage);
            buf.writeItem(r.result);
            buf.writeBoolean(r.fluidResultId != null);
            if (r.fluidResultId != null) buf.writeResourceLocation(r.fluidResultId);
            buf.writeInt(r.fluidResultAmount);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
        }
    }
}
