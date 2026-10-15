package com.jykito.industrialcore.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Industrial assembler recipe: several ingredients with counts, optional fluid,
 * result: item and/or fluid. The core takes the input from the structure's input ports, fluid from ports
 * in fluid mode; a fluid result goes into output ports with a tank.
 * JSON: { inputs:[{item|tag,count},...], fluid:{fluid,amount}?, result:{id,count}?, fluidResult:{fluid,amount}?,
 *         processingTime, energyCostPerTick }
 */
public class AssemblingRecipe implements Recipe<ContainerInput> {

    private final Ingredient[] inputs;
    private final int[] counts;
    private final FluidStack fluid;
    private final ItemStack result;
    private final FluidStack fluidResult;
    private final int processingTime, energyPerTick;

    public AssemblingRecipe(Ingredient[] inputs, int[] counts, FluidStack fluid, ItemStack result,
                            FluidStack fluidResult, int processingTime, int energyPerTick) {
        this.inputs = inputs; this.counts = counts; this.fluid = fluid; this.result = result;
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

    /** The assembler counts the stock across ports itself; it does not need the vanilla container lookup. */
    @Override public boolean matches(ContainerInput c, Level level) { return false; }
    @Override public ItemStack assemble(ContainerInput c, HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (Ingredient in : inputs) list.add(in);
        return list;
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ASSEMBLING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.ASSEMBLING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<AssemblingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<AssemblingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RecipeCodecs.COUNTED_INPUTS.fieldOf("inputs").forGetter(r -> RecipeCodecs.sized(r.inputs, r.counts)),
                RecipeCodecs.optionalFluid("fluid").forGetter(r -> r.fluid),
                RecipeCodecs.optionalResult("result").forGetter(r -> r.result),
                RecipeCodecs.optionalFluid("fluidResult").forGetter(r -> r.fluidResult),
                Codec.INT.optionalFieldOf("processingTime", 200).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("energyCostPerTick", 1000).forGetter(r -> r.energyPerTick)
        ).apply(i, (in, fluid, result, fluidResult, time, energy) -> new AssemblingRecipe(
                RecipeCodecs.ingredients(in), RecipeCodecs.counts(in), fluid, result, fluidResult, time, energy)));

        private static final StreamCodec<RegistryFriendlyByteBuf, AssemblingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<AssemblingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, AssemblingRecipe> streamCodec() { return STREAM_CODEC; }

        private static AssemblingRecipe read(RegistryFriendlyByteBuf buf) {
            RecipeCodecs.Counted in = RecipeCodecs.readCounted(buf);
            FluidStack fluid = RecipeCodecs.readFluid(buf);
            ItemStack result = RecipeCodecs.readItem(buf);
            FluidStack fluidResult = RecipeCodecs.readFluid(buf);
            int time = buf.readVarInt(), energy = buf.readVarInt();
            return new AssemblingRecipe(in.inputs(), in.counts(), fluid, result, fluidResult, time, energy);
        }

        private static void write(RegistryFriendlyByteBuf buf, AssemblingRecipe r) {
            RecipeCodecs.writeCounted(buf, r.inputs, r.counts);
            RecipeCodecs.writeFluid(buf, r.fluid);
            RecipeCodecs.writeItem(buf, r.result);
            RecipeCodecs.writeFluid(buf, r.fluidResult);
            buf.writeVarInt(r.processingTime); buf.writeVarInt(r.energyPerTick);
        }
    }
}
