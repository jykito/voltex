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
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Рецепт ускорителя: 1–8 РАЗНЫХ ингредиентов (общие слоты 0-7) → выход. 2 входа = «столкновение», 8 = «сборка».
 * minInstab — экзо-гейт (крафт идёт только при нестабильности ≥ порога). [bandLo,bandHi] — полоса нестабильности,
 * удержание в которой на завершении даёт ×2 (контролируемо колоннами). minTier — минимальная катушка.
 * JSON: { inputs:[{item|tag,count},...], result:{item,count}, minTier, processingTime, energyCostPerTick, minInstab, bandLo, bandHi }
 */
public class AcceleratorRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final Ingredient[] inputs;
    private final int[] counts;
    private final ItemStack result;
    private final int minTier;
    private final int processingTime;
    private final int energyPerTick;
    private final float minInstab;
    private final float bandLo, bandHi;

    public AcceleratorRecipe(ResourceLocation id, Ingredient[] inputs, int[] counts, ItemStack result,
                             int minTier, int processingTime, int energyPerTick,
                             float minInstab, float bandLo, float bandHi) {
        this.id = id; this.inputs = inputs; this.counts = counts; this.result = result;
        this.minTier = minTier; this.processingTime = processingTime; this.energyPerTick = energyPerTick;
        this.minInstab = minInstab; this.bandLo = bandLo; this.bandHi = bandHi;
    }

    public Ingredient[] getInputs()  { return inputs; }
    public int[] getCounts()         { return counts; }
    public ItemStack getResult()     { return result; }
    public int getMinTier()          { return minTier; }
    public int getProcessingTime()   { return processingTime; }
    public int getEnergyPerTick()    { return energyPerTick; }
    public float getMinInstab()      { return minInstab; }
    /** В оптимальной полосе нестабильности → удвоение выхода. */
    public boolean inBand(float instab) { return bandHi > bandLo && instab >= bandLo && instab <= bandHi; }
    /** Полоса [bandLo,bandHi] теперь = диапазон, в котором дрейфует резонансная цель (см. ElectromagnetCoreBlockEntity). */
    public float getBandLo() { return bandLo; }
    public float getBandHi() { return bandHi; }

    /** Все ингредиенты есть в слотах 0-7 (жадное сопоставление: каждому — свой слот с нужным count). */
    public boolean matches(ItemStack[] slots) {
        boolean[] used = new boolean[slots.length];
        for (int i = 0; i < inputs.length; i++) {
            boolean found = false;
            for (int j = 0; j < slots.length; j++) {
                if (used[j]) continue;
                if (inputs[i].test(slots[j]) && slots[j].getCount() >= counts[i]) { used[j] = true; found = true; break; }
            }
            if (!found) return false;
        }
        return true;
    }

    @Override
    public boolean matches(SimpleContainer c, Level level) {
        if (level.isClientSide()) return false;
        ItemStack[] slots = new ItemStack[c.getContainerSize()];
        for (int i = 0; i < slots.length; i++) slots[i] = c.getItem(i);
        return matches(slots);
    }

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
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ACCELERATOR_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.ACCELERATING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<AcceleratorRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public AcceleratorRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonArray arr = GsonHelper.getAsJsonArray(json, "inputs");
            int n = arr.size();
            Ingredient[] ing = new Ingredient[n];
            int[] cnt = new int[n];
            for (int i = 0; i < n; i++) {
                JsonObject el = arr.get(i).getAsJsonObject();
                ing[i] = Ingredient.fromJson(el);
                cnt[i] = GsonHelper.getAsInt(el, "count", 1);
            }
            JsonObject res = GsonHelper.getAsJsonObject(json, "result");
            ItemStack result = new ItemStack(GsonHelper.getAsItem(res, "item"), GsonHelper.getAsInt(res, "count", 1));
            int minTier = GsonHelper.getAsInt(json, "minTier", 1);
            int time    = GsonHelper.getAsInt(json, "processingTime", 200);
            int energy  = GsonHelper.getAsInt(json, "energyCostPerTick", 1000);
            float minInstab = GsonHelper.getAsFloat(json, "minInstab", 0f);
            float bandLo    = GsonHelper.getAsFloat(json, "bandLo", 0f);
            float bandHi    = GsonHelper.getAsFloat(json, "bandHi", 0f);
            return new AcceleratorRecipe(id, ing, cnt, result, minTier, time, energy, minInstab, bandLo, bandHi);
        }

        @Override
        public @Nullable AcceleratorRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            Ingredient[] ing = new Ingredient[n];
            int[] cnt = new int[n];
            for (int i = 0; i < n; i++) { ing[i] = Ingredient.fromNetwork(buf); cnt[i] = buf.readVarInt(); }
            ItemStack result = buf.readItem();
            int minTier = buf.readVarInt(), time = buf.readVarInt(), energy = buf.readVarInt();
            float minInstab = buf.readFloat(), bandLo = buf.readFloat(), bandHi = buf.readFloat();
            return new AcceleratorRecipe(id, ing, cnt, result, minTier, time, energy, minInstab, bandLo, bandHi);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, AcceleratorRecipe r) {
            buf.writeVarInt(r.inputs.length);
            for (int i = 0; i < r.inputs.length; i++) { r.inputs[i].toNetwork(buf); buf.writeVarInt(r.counts[i]); }
            buf.writeItem(r.result);
            buf.writeVarInt(r.minTier); buf.writeVarInt(r.processingTime); buf.writeVarInt(r.energyPerTick);
            buf.writeFloat(r.minInstab); buf.writeFloat(r.bandLo); buf.writeFloat(r.bandHi);
        }
    }
}
