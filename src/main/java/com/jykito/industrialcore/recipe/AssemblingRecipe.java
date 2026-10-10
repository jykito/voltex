package com.jykito.industrialcore.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Рецепт промышленного сборщика: несколько ингредиентов с количеством, необязательная жидкость,
 * результат: предмет и/или жидкость. Сырьё ядро берёт из входных портов структуры, жидкость — из портов
 * в режиме жидкости; жидкий результат уходит в выходные порты с баком.
 * JSON: { inputs:[{item|tag,count},...], fluid:{fluid,amount}?, result:{item,count}?, fluidResult:{fluid,amount}?,
 *         processingTime, energyCostPerTick }
 */
public class AssemblingRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final Ingredient[] inputs;
    private final int[] counts;
    private final FluidStack fluid;
    private final ItemStack result;
    private final FluidStack fluidResult;
    private final int processingTime, energyPerTick;

    public AssemblingRecipe(ResourceLocation id, Ingredient[] inputs, int[] counts, FluidStack fluid, ItemStack result,
                            FluidStack fluidResult, int processingTime, int energyPerTick) {
        this.id = id; this.inputs = inputs; this.counts = counts; this.fluid = fluid; this.result = result;
        this.fluidResult = fluidResult;
        this.processingTime = processingTime; this.energyPerTick = energyPerTick;
    }

    public Ingredient[] getInputs()  { return inputs; }
    public int[] getCounts()         { return counts; }
    public FluidStack getFluid()     { return fluid; }
    public ItemStack getResult()     { return result; }
    public FluidStack getFluidResult() { return fluidResult; }
    public int getProcessingTime()   { return processingTime; }
    public int getEnergyPerTick()    { return energyPerTick; }

    /** Сборщик сам считает запасы по портам; ванильный поиск по контейнеру ему не нужен. */
    @Override public boolean matches(SimpleContainer c, Level level) { return false; }
    @Override public ItemStack assemble(SimpleContainer c, RegistryAccess a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(RegistryAccess a) { return result; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (Ingredient in : inputs) list.add(in);
        return list;
    }

    @Override public ResourceLocation getId()            { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ASSEMBLING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.ASSEMBLING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<AssemblingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public AssemblingRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonArray arr = GsonHelper.getAsJsonArray(json, "inputs");
            int n = arr.size();
            Ingredient[] ing = new Ingredient[n];
            int[] cnt = new int[n];
            for (int i = 0; i < n; i++) {
                JsonObject el = arr.get(i).getAsJsonObject();
                ing[i] = Ingredient.fromJson(el);
                cnt[i] = GsonHelper.getAsInt(el, "count", 1);
            }
            FluidStack fluid = fluidFromJson(json, "fluid");
            FluidStack fluidResult = fluidFromJson(json, "fluidResult");
            ItemStack result = ItemStack.EMPTY;
            if (json.has("result")) {
                JsonObject res = GsonHelper.getAsJsonObject(json, "result");
                result = new ItemStack(GsonHelper.getAsItem(res, "item"), GsonHelper.getAsInt(res, "count", 1));
            }
            int time   = GsonHelper.getAsInt(json, "processingTime", 200);
            int energy = GsonHelper.getAsInt(json, "energyCostPerTick", 1000);
            return new AssemblingRecipe(id, ing, cnt, fluid, result, fluidResult, time, energy);
        }

        private static FluidStack fluidFromJson(JsonObject json, String key) {
            if (!json.has(key)) return FluidStack.EMPTY;
            JsonObject f = GsonHelper.getAsJsonObject(json, key);
            var fl = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(GsonHelper.getAsString(f, "fluid")));
            return fl == null ? FluidStack.EMPTY : new FluidStack(fl, GsonHelper.getAsInt(f, "amount", 1000));
        }

        @Override
        public @Nullable AssemblingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            Ingredient[] ing = new Ingredient[n];
            int[] cnt = new int[n];
            for (int i = 0; i < n; i++) { ing[i] = Ingredient.fromNetwork(buf); cnt[i] = buf.readVarInt(); }
            FluidStack fluid = FluidStack.readFromPacket(buf);
            ItemStack result = buf.readItem();
            FluidStack fluidResult = FluidStack.readFromPacket(buf);
            int time = buf.readVarInt(), energy = buf.readVarInt();
            return new AssemblingRecipe(id, ing, cnt, fluid, result, fluidResult, time, energy);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, AssemblingRecipe r) {
            buf.writeVarInt(r.inputs.length);
            for (int i = 0; i < r.inputs.length; i++) { r.inputs[i].toNetwork(buf); buf.writeVarInt(r.counts[i]); }
            r.fluid.writeToPacket(buf);
            buf.writeItem(r.result);
            r.fluidResult.writeToPacket(buf);
            buf.writeVarInt(r.processingTime); buf.writeVarInt(r.energyPerTick);
        }
    }
}
