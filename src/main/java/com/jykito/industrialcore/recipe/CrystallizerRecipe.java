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
 * Кристаллизация: жидкость плюс предмет-затравка дают предмет.
 * <p>
 * Это единственный в моде путь «жидкость → предмет»: сжижитель работает предмет→жидкость,
 * обогатитель предмет+жидкость→жидкость, промывщик предмет+жидкость→предметы. Обратного
 * не умел никто, поэтому тип заведён отдельно, а не через {@code machine_recipe}.
 * <p>
 * Затравка необязательна: без неё машина кристаллизует из одной жидкости.
 */
public class CrystallizerRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final ResourceLocation fluidInputId;
    private final int fluidInputAmount;
    private final Ingredient ingredient;
    private final int ingredientCount;
    private final ItemStack result;
    private final int processingTime;
    private final int energyCostPerTick;

    public CrystallizerRecipe(ResourceLocation id,
                              ResourceLocation fluidInputId, int fluidInputAmount,
                              Ingredient ingredient, int ingredientCount,
                              ItemStack result, int processingTime, int energyCostPerTick) {
        this.id                = id;
        this.fluidInputId      = fluidInputId;
        this.fluidInputAmount  = fluidInputAmount;
        this.ingredient        = ingredient;
        this.ingredientCount   = ingredientCount;
        this.result            = result;
        this.processingTime    = processingTime;
        this.energyCostPerTick = energyCostPerTick;
    }

    /** Подходит ли содержимое слота затравки. Рецепт без затравки принимает пустой слот. */
    public boolean seedMatches(ItemStack seed) {
        if (!hasSeed()) return true;
        return ingredient.test(seed) && seed.getCount() >= ingredientCount;
    }

    /** Результат как есть, без копии — для проверок в тике. Менять его нельзя. */
    public ItemStack        getResult()         { return result; }
    public boolean          hasSeed()           { return !ingredient.isEmpty(); }
    public Ingredient       getIngredient()     { return ingredient; }
    public int              getIngredientCount(){ return ingredientCount; }
    public int              getProcessingTime() { return processingTime; }
    public int              getEnergyCostPerTick() { return energyCostPerTick; }
    public int              getFluidInputAmount()  { return fluidInputAmount; }
    public ResourceLocation getFluidInputId()   { return fluidInputId; }

    public FluidStack makeFluidInput() {
        var fluid = ForgeRegistries.FLUIDS.getValue(fluidInputId);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, fluidInputAmount);
    }

    @Override public boolean matches(SimpleContainer c, Level level) { return !level.isClientSide() && seedMatches(c.getItem(0)); }
    @Override public ItemStack assemble(SimpleContainer c, RegistryAccess a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(RegistryAccess a) { return result.copy(); }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        if (hasSeed()) list.add(ingredient);
        return list;
    }

    @Override public ResourceLocation getId()            { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.CRYSTALLIZER_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.CRYSTALLIZING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<CrystallizerRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public CrystallizerRecipe fromJson(ResourceLocation id, JsonObject json) {
            ResourceLocation fluidIn = new ResourceLocation(GsonHelper.getAsString(json, "fluidInput"));
            int fluidInAmt           = GsonHelper.getAsInt(json, "fluidInputAmount", 1000);

            Ingredient ingredient = Ingredient.EMPTY;
            int        count      = 0;
            if (json.has("ingredient")) {
                ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
                count      = GsonHelper.getAsInt(json, "ingredientCount", 1);
            }

            ItemStack result   = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int processingTime = GsonHelper.getAsInt(json, "processingTime", 200);
            int energyCost     = GsonHelper.getAsInt(json, "energyCostPerTick", 40);
            return new CrystallizerRecipe(id, fluidIn, fluidInAmt, ingredient, count, result, processingTime, energyCost);
        }

        @Override
        public @Nullable CrystallizerRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            ResourceLocation fluidIn = buf.readResourceLocation();
            int        fluidInAmt    = buf.readInt();
            Ingredient ingredient    = Ingredient.fromNetwork(buf);
            int        count         = buf.readInt();
            ItemStack  result        = buf.readItem();
            int        processingTime = buf.readInt();
            int        energyCost     = buf.readInt();
            return new CrystallizerRecipe(id, fluidIn, fluidInAmt, ingredient, count, result, processingTime, energyCost);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, CrystallizerRecipe r) {
            buf.writeResourceLocation(r.fluidInputId);
            buf.writeInt(r.fluidInputAmount);
            r.ingredient.toNetwork(buf);
            buf.writeInt(r.ingredientCount);
            buf.writeItem(r.result);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
        }
    }
}
