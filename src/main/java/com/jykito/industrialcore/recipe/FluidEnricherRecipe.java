package com.jykito.industrialcore.recipe;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;

import net.minecraft.core.HolderLookup;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

public class FluidEnricherRecipe implements Recipe<ContainerInput> {

    /** Recipe for the late machine only (its industrial mode); the small machine does not see it. */
    private boolean lateOnly;
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

    public FluidEnricherRecipe(Ingredient itemIngredient, int itemCount,
                                Ingredient itemIngredient2, int itemCount2,
                                ResourceLocation fluidInputId, int fluidInputAmount,
                                ResourceLocation fluidOutputId, int fluidOutputAmount,
                                int processingTime, int energyCostPerTick) {
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

    public boolean isLateOnly() { return lateOnly; }

    @Override
    public boolean matches(ContainerInput container, Level level) {
        if (lateOnly) return false;
        if (level.isClientSide()) return false;
        return itemIngredient.test(container.getItem(0)) && container.getItem(0).getCount() >= itemCount;
    }

    @Override public ItemStack assemble(ContainerInput c, HolderLookup.Provider a) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider a) { return ItemStack.EMPTY; }

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
        var fluid = BuiltInRegistries.FLUID.get(fluidInputId);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, fluidInputAmount);
    }

    public FluidStack makeFluidOutput() {
        var fluid = BuiltInRegistries.FLUID.get(fluidOutputId);
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, fluidOutputAmount);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(itemIngredient);
        if (hasSecondReagent()) list.add(itemIngredient2);
        return list;
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.FLUID_ENRICHER_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()            { return ModRecipes.FLUID_ENRICHER_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<FluidEnricherRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        /** No "ingredient2" means no second reagent, and then its count is 0 whatever "itemCount2" says. */
        private static final MapCodec<FluidEnricherRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.itemIngredient),
                Codec.INT.optionalFieldOf("itemCount", 1).forGetter(r -> r.itemCount),
                Ingredient.CODEC_NONEMPTY.optionalFieldOf("ingredient2", Ingredient.EMPTY).forGetter(r -> r.itemIngredient2),
                Codec.INT.optionalFieldOf("itemCount2", 1).forGetter(r -> r.itemCount2),
                ResourceLocation.CODEC.fieldOf("fluidInput").forGetter(r -> r.fluidInputId),
                Codec.INT.optionalFieldOf("fluidInputAmount", 1000).forGetter(r -> r.fluidInputAmount),
                ResourceLocation.CODEC.fieldOf("fluidOutput").forGetter(r -> r.fluidOutputId),
                Codec.INT.optionalFieldOf("fluidOutputAmount", 1000).forGetter(r -> r.fluidOutputAmount),
                Codec.INT.optionalFieldOf("processingTime", 100).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("energyCostPerTick", 50).forGetter(r -> r.energyCostPerTick),
                Codec.BOOL.optionalFieldOf("lateOnly", false).forGetter(r -> r.lateOnly)
        ).apply(i, (ing, count, ing2, count2, fluidIn, fluidInAmt, fluidOut, fluidOutAmt, time, energy, lateOnly) -> {
            FluidEnricherRecipe r = new FluidEnricherRecipe(ing, count, ing2, ing2.isEmpty() ? 0 : count2,
                    fluidIn, fluidInAmt, fluidOut, fluidOutAmt, time, energy);
            r.lateOnly = lateOnly;
            return r;
        }));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidEnricherRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<FluidEnricherRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, FluidEnricherRecipe> streamCodec() { return STREAM_CODEC; }

        private static FluidEnricherRecipe read(RegistryFriendlyByteBuf buf) {
            Ingredient itemIngredient   = RecipeCodecs.readIngredient(buf);
            int        itemCount        = buf.readInt();
            Ingredient itemIngredient2  = RecipeCodecs.readIngredient(buf);
            int        itemCount2       = buf.readInt();
            ResourceLocation fluidIn    = buf.readResourceLocation();
            int        fluidInAmt       = buf.readInt();
            ResourceLocation fluidOut   = buf.readResourceLocation();
            int        fluidOutAmt      = buf.readInt();
            int        processingTime   = buf.readInt();
            int        energyCostPerTick = buf.readInt();
            FluidEnricherRecipe recipe = new FluidEnricherRecipe(itemIngredient, itemCount, itemIngredient2, itemCount2,
                    fluidIn, fluidInAmt, fluidOut, fluidOutAmt, processingTime, energyCostPerTick);
            recipe.lateOnly = buf.readBoolean();
            return recipe;
        }

        private static void write(RegistryFriendlyByteBuf buf, FluidEnricherRecipe r) {
            RecipeCodecs.writeIngredient(buf, r.itemIngredient);
            buf.writeInt(r.itemCount);
            RecipeCodecs.writeIngredient(buf, r.itemIngredient2);
            buf.writeInt(r.itemCount2);
            buf.writeResourceLocation(r.fluidInputId);
            buf.writeInt(r.fluidInputAmount);
            buf.writeResourceLocation(r.fluidOutputId);
            buf.writeInt(r.fluidOutputAmount);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
            buf.writeBoolean(r.lateOnly);
        }
    }
}
