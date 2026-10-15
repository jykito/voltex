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
 * Accelerator recipe: 1-8 DIFFERENT ingredients (shared slots 0-7) -> output. 2 inputs = "collision", 8 = "assembly".
 * minInstab is the exo gate (crafting runs only with instability ≥ the threshold). [bandLo,bandHi] is the instability band;
 * holding it at completion gives x2 (controlled with columns). minTier is the minimum coil.
 * JSON: { inputs:[{item|tag,count},...], result:{id,count}, minTier, processingTime, energyCostPerTick, minInstab, bandLo, bandHi,
 *         fluid:{fluid,amount}?, lateOnly? }
 */
public class AcceleratorRecipe implements Recipe<ContainerInput> {

    /** Recipe for the late machine only (its industrial mode); the small machine does not see it. */
    private final boolean lateOnly;
    private final Ingredient[] inputs;
    private final int[] counts;
    private final ItemStack result;
    private final int minTier;
    private final int processingTime;
    private final int energyPerTick;
    private final float minInstab;
    private final float bandLo, bandHi;
    /** Fluid input (late magnet port); the old magnet has no fluid input, so such recipes are lateOnly only. */
    private final FluidStack fluid;

    public AcceleratorRecipe(Ingredient[] inputs, int[] counts, ItemStack result,
                             int minTier, int processingTime, int energyPerTick,
                             float minInstab, float bandLo, float bandHi, boolean lateOnly, FluidStack fluid) {
        this.inputs = inputs; this.counts = counts; this.result = result;
        this.minTier = minTier; this.processingTime = processingTime; this.energyPerTick = energyPerTick;
        this.minInstab = minInstab; this.bandLo = bandLo; this.bandHi = bandHi;
        this.lateOnly = lateOnly; this.fluid = fluid;
    }

    public Ingredient[] getInputs()  { return inputs; }
    public int[] getCounts()         { return counts; }
    public ItemStack getResult()     { return result; }
    public int getMinTier()          { return minTier; }
    public int getProcessingTime()   { return processingTime; }
    public int getEnergyPerTick()    { return energyPerTick; }
    public float getMinInstab()      { return minInstab; }
    /** In the optimal instability band -> output doubled. */
    public boolean inBand(float instab) { return bandHi > bandLo && instab >= bandLo && instab <= bandHi; }
    /** The [bandLo,bandHi] band is now the range the resonance target drifts in (see ElectromagnetCoreBlockEntity). */
    public float getBandLo() { return bandLo; }
    public float getBandHi() { return bandHi; }

    /** All ingredients are in slots 0-7 (greedy matching: each gets its own slot with the required count). */
    public boolean matches(ItemStack[] slots) {
        if (lateOnly) return false;
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

    public boolean isLateOnly() { return lateOnly; }
    public FluidStack getFluid() { return fluid; }

    @Override
    public boolean matches(ContainerInput c, Level level) {
        if (lateOnly) return false;
        if (level.isClientSide()) return false;
        ItemStack[] slots = new ItemStack[c.getContainerSize()];
        for (int i = 0; i < slots.length; i++) slots[i] = c.getItem(i);
        return matches(slots);
    }

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

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ACCELERATOR_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()             { return ModRecipes.ACCELERATING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<AcceleratorRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<AcceleratorRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RecipeCodecs.COUNTED_INPUTS.fieldOf("inputs").forGetter(r -> RecipeCodecs.sized(r.inputs, r.counts)),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("minTier", 1).forGetter(r -> r.minTier),
                Codec.INT.optionalFieldOf("processingTime", 200).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("energyCostPerTick", 1000).forGetter(r -> r.energyPerTick),
                Codec.FLOAT.optionalFieldOf("minInstab", 0f).forGetter(r -> r.minInstab),
                Codec.FLOAT.optionalFieldOf("bandLo", 0f).forGetter(r -> r.bandLo),
                Codec.FLOAT.optionalFieldOf("bandHi", 0f).forGetter(r -> r.bandHi),
                Codec.BOOL.optionalFieldOf("lateOnly", false).forGetter(r -> r.lateOnly),
                RecipeCodecs.optionalFluid("fluid").forGetter(r -> r.fluid)
        ).apply(i, (in, result, minTier, time, energy, minInstab, bandLo, bandHi, lateOnly, fluid) -> new AcceleratorRecipe(
                RecipeCodecs.ingredients(in), RecipeCodecs.counts(in), result, minTier, time, energy,
                minInstab, bandLo, bandHi, lateOnly, fluid)));

        private static final StreamCodec<RegistryFriendlyByteBuf, AcceleratorRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<AcceleratorRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, AcceleratorRecipe> streamCodec() { return STREAM_CODEC; }

        private static AcceleratorRecipe read(RegistryFriendlyByteBuf buf) {
            RecipeCodecs.Counted in = RecipeCodecs.readCounted(buf);
            ItemStack result = RecipeCodecs.readItem(buf);
            int minTier = buf.readVarInt(), time = buf.readVarInt(), energy = buf.readVarInt();
            float minInstab = buf.readFloat(), bandLo = buf.readFloat(), bandHi = buf.readFloat();
            boolean lateOnly = buf.readBoolean();
            FluidStack fluid = RecipeCodecs.readFluid(buf);
            return new AcceleratorRecipe(in.inputs(), in.counts(), result, minTier, time, energy,
                    minInstab, bandLo, bandHi, lateOnly, fluid);
        }

        private static void write(RegistryFriendlyByteBuf buf, AcceleratorRecipe r) {
            RecipeCodecs.writeCounted(buf, r.inputs, r.counts);
            RecipeCodecs.writeItem(buf, r.result);
            buf.writeVarInt(r.minTier); buf.writeVarInt(r.processingTime); buf.writeVarInt(r.energyPerTick);
            buf.writeFloat(r.minInstab); buf.writeFloat(r.bandLo); buf.writeFloat(r.bandHi);
            buf.writeBoolean(r.lateOnly);
            RecipeCodecs.writeFluid(buf, r.fluid);
        }
    }
}
