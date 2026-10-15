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
import org.jetbrains.annotations.Nullable;

public class PhaseSplitterRecipe implements Recipe<ContainerInput> {

    private final Ingredient input;
    private final int inputCount;
    private final ItemStack output1;
    private final ItemStack output2;
    private final ItemStack output3;
    private final int processingTime;
    private final int energyCost;

    public PhaseSplitterRecipe(Ingredient input, int inputCount,
                                ItemStack output1, ItemStack output2, ItemStack output3,
                                int processingTime, int energyCost) {
        this.input = input;
        this.inputCount = inputCount;
        this.output1 = output1;
        this.output2 = output2;
        this.output3 = output3;
        this.processingTime = processingTime;
        this.energyCost = energyCost;
    }

    @Override
    public boolean matches(ContainerInput container, Level level) {
        if (level.isClientSide()) return false;
        return input.test(container.getItem(0)) && container.getItem(0).getCount() >= inputCount;
    }

    @Override
    public ItemStack assemble(ContainerInput container, HolderLookup.Provider access) { return output1.copy(); }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override public boolean isSpecial() { return true; }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider access) { return output1.copy(); }

    public ItemStack getOutput1() { return output1.copy(); }
    public ItemStack getOutput2() { return output2.copy(); }
    public ItemStack getOutput3() { return output3.copy(); }
    public int getInputCount() { return inputCount; }
    public int getProcessingTime() { return processingTime; }
    public int getEnergyCost() { return energyCost; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(input);
        return list;
    }


    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipes.PHASE_SPLITTER_SERIALIZER.get(); }

    @Override
    public RecipeType<?> getType() { return ModRecipes.PHASE_SPLITTING_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<PhaseSplitterRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<PhaseSplitterRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.input),
                Codec.INT.optionalFieldOf("inputCount", 1).forGetter(r -> r.inputCount),
                RecipeCodecs.optionalResult("result").forGetter(r -> r.output1),
                RecipeCodecs.optionalResult("result2").forGetter(r -> r.output2),
                RecipeCodecs.optionalResult("result3").forGetter(r -> r.output3),
                Codec.INT.optionalFieldOf("processingTime", 400).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("energyCost", 100).forGetter(r -> r.energyCost)
        ).apply(i, PhaseSplitterRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PhaseSplitterRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<PhaseSplitterRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, PhaseSplitterRecipe> streamCodec() { return STREAM_CODEC; }

        private static PhaseSplitterRecipe read(RegistryFriendlyByteBuf buf) {
            Ingredient input = RecipeCodecs.readIngredient(buf);
            int inputCount   = buf.readInt();
            ItemStack out1   = RecipeCodecs.readItem(buf);
            ItemStack out2   = RecipeCodecs.readItem(buf);
            ItemStack out3   = RecipeCodecs.readItem(buf);
            int processingTime = buf.readInt();
            int energyCost     = buf.readInt();
            return new PhaseSplitterRecipe(input, inputCount, out1, out2, out3, processingTime, energyCost);
        }

        private static void write(RegistryFriendlyByteBuf buf, PhaseSplitterRecipe r) {
            RecipeCodecs.writeIngredient(buf, r.input);
            buf.writeInt(r.inputCount);
            RecipeCodecs.writeItem(buf, r.output1);
            RecipeCodecs.writeItem(buf, r.output2);
            RecipeCodecs.writeItem(buf, r.output3);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCost);
        }
    }
}
