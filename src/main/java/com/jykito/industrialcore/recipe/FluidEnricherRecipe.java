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

public class FluidEnricherRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final Ingredient itemIngredient;
    private final int itemCount;
    private final Ingredient itemIngredient2;
    private final int itemCount2;
    private final ResourceLocation fluidInputId;
    private final int fluidInputAmount;
    private final ResourceLocation fluidOutputId;
    private final int fluidOutputAmount;
    private final int processingTime;
    private final int energyCostPerTick;

    public FluidEnricherRecipe(ResourceLocation id,
                                Ingredient itemIngredient, int itemCount,
                                Ingredient itemIngredient2, int itemCount2,
                                ResourceLocation fluidInputId, int fluidInputAmount,
                                ResourceLocation fluidOutputId, int fluidOutputAmount,
                                int processingTime, int energyCostPerTick) {
        this.id               = id;
        this.itemIngredient   = itemIngredient;
        this.itemCount        = itemCount;
        this.itemIngredient2  = itemIngredient2;
        this.itemCount2       = itemCount2;
        this.fluidInputId     = fluidInputId;
        this.fluidInputAmount = fluidInputAmount;
        this.fluidOutputId    = fluidOutputId;
        this.fluidOutputAmount = fluidOutputAmount;
        this.processingTime   = processingTime;
        this.energyCostPerTick = energyCostPerTick;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (level.isClientSide()) return false;
        return itemIngredient.test(container.getItem(0)) && container.getItem(0).getCount() >= itemCount;
    }

    @Override public ItemStack assemble(SimpleContainer c, RegistryAccess a) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(RegistryAccess a) { return ItemStack.EMPTY; }

    public Ingredient getItemIngredient()   { return itemIngredient; }
    public int        getItemCount()        { return itemCount; }
    public Ingredient getItemIngredient2()  { return itemIngredient2; }
    public int        getItemCount2()       { return itemCount2; }
    public boolean    hasSecondReagent()    { return !itemIngredient2.isEmpty(); }

    public boolean reagentsMatch(ItemStack s2, ItemStack s3) {
        if (!hasSecondReagent()) {
            return (itemIngredient.test(s2) && s2.getCount() >= itemCount)
                || (itemIngredient.test(s3) && s3.getCount() >= itemCount);
        }
        return reagentPair(s2, s3) || reagentPair(s3, s2);
    }

    public boolean reagentPair(ItemStack slotA, ItemStack slotB) {
        return itemIngredient.test(slotA)  && slotA.getCount() >= itemCount
            && itemIngredient2.test(slotB) && slotB.getCount() >= itemCount2;
    }
    public int        getProcessingTime()   { return processingTime; }
    public int        getEnergyCostPerTick(){ return energyCostPerTick; }
    public int        getFluidInputAmount() { return fluidInputAmount; }
    public int        getFluidOutputAmount(){ return fluidOutputAmount; }
    public ResourceLocation getFluidInputId()  { return fluidInputId; }
    public ResourceLocation getFluidOutputId() { return fluidOutputId; }

    public FluidStack makeFluidInput() {
        var fluid = ForgeRegistries.FLUIDS.getValue(fluidInputId);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, fluidInputAmount);
    }

    public FluidStack makeFluidOutput() {
        var fluid = ForgeRegistries.FLUIDS.getValue(fluidOutputId);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, fluidOutputAmount);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(itemIngredient);
        if (hasSecondReagent()) list.add(itemIngredient2);
        return list;
    }

    @Override public ResourceLocation getId()           { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.FLUID_ENRICHER_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()            { return ModRecipes.FLUID_ENRICHER_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<FluidEnricherRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public FluidEnricherRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient itemIngredient   = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            int        itemCount        = GsonHelper.getAsInt(json, "itemCount", 1);

            Ingredient itemIngredient2  = Ingredient.EMPTY;
            int        itemCount2       = 0;
            if (json.has("ingredient2")) {
                itemIngredient2 = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient2"));
                itemCount2      = GsonHelper.getAsInt(json, "itemCount2", 1);
            }
            ResourceLocation fluidIn   = new ResourceLocation(GsonHelper.getAsString(json, "fluidInput"));
            int        fluidInAmt      = GsonHelper.getAsInt(json, "fluidInputAmount", 1000);
            ResourceLocation fluidOut  = new ResourceLocation(GsonHelper.getAsString(json, "fluidOutput"));
            int        fluidOutAmt     = GsonHelper.getAsInt(json, "fluidOutputAmount", 1000);
            int        processingTime  = GsonHelper.getAsInt(json, "processingTime", 100);
            int        energyCostPerTick = GsonHelper.getAsInt(json, "energyCostPerTick", 50);
            return new FluidEnricherRecipe(id, itemIngredient, itemCount, itemIngredient2, itemCount2,
                    fluidIn, fluidInAmt, fluidOut, fluidOutAmt, processingTime, energyCostPerTick);
        }

        @Override
        public @Nullable FluidEnricherRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient itemIngredient   = Ingredient.fromNetwork(buf);
            int        itemCount        = buf.readInt();
            Ingredient itemIngredient2  = Ingredient.fromNetwork(buf);
            int        itemCount2       = buf.readInt();
            ResourceLocation fluidIn    = buf.readResourceLocation();
            int        fluidInAmt       = buf.readInt();
            ResourceLocation fluidOut   = buf.readResourceLocation();
            int        fluidOutAmt      = buf.readInt();
            int        processingTime   = buf.readInt();
            int        energyCostPerTick = buf.readInt();
            return new FluidEnricherRecipe(id, itemIngredient, itemCount, itemIngredient2, itemCount2,
                    fluidIn, fluidInAmt, fluidOut, fluidOutAmt, processingTime, energyCostPerTick);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, FluidEnricherRecipe r) {
            r.itemIngredient.toNetwork(buf);
            buf.writeInt(r.itemCount);
            r.itemIngredient2.toNetwork(buf);
            buf.writeInt(r.itemCount2);
            buf.writeResourceLocation(r.fluidInputId);
            buf.writeInt(r.fluidInputAmount);
            buf.writeResourceLocation(r.fluidOutputId);
            buf.writeInt(r.fluidOutputAmount);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
        }
    }
}
