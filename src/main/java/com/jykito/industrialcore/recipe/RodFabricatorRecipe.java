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

public class RodFabricatorRecipe implements Recipe<ContainerInput> {

    private final Ingredient ingredient1;
    private final int count1;
    private final Ingredient ingredient2;
    private final int count2;
    private final ItemStack result;
    private final int processingTime;
    private final int energyCostPerTick;

    public RodFabricatorRecipe(Ingredient ingredient1, int count1,
                         Ingredient ingredient2, int count2,
                         ItemStack result, int processingTime, int energyCostPerTick) {
        this.ingredient1      = ingredient1;
        this.count1           = count1;
        this.ingredient2      = ingredient2;
        this.count2           = count2;
        this.result           = result;
        this.processingTime   = processingTime;
        this.energyCostPerTick = energyCostPerTick;
    }

    @Override
    public boolean matches(ContainerInput container, Level level) {
        if (level.isClientSide()) return false;
        return ingredient1.test(container.getItem(0)) && container.getItem(0).getCount() >= count1
            && ingredient2.test(container.getItem(1)) && container.getItem(1).getCount() >= count2;
    }

    @Override public ItemStack assemble(ContainerInput c, HolderLookup.Provider a) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider a) { return result.copy(); }

    public Ingredient getIngredient1()    { return ingredient1; }
    public int        getCount1()         { return count1; }
    public Ingredient getIngredient2()    { return ingredient2; }
    public int        getCount2()         { return count2; }
    public int        getProcessingTime() { return processingTime; }
    public int        getEnergyCostPerTick() { return energyCostPerTick; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient1);
        list.add(ingredient2);
        return list;
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ROD_FABRICATION_SERIALIZER.get(); }
    @Override public RecipeType<?> getType()            { return ModRecipes.ROD_FABRICATION_TYPE.get(); }

    public static class Serializer implements RecipeSerializer<RodFabricatorRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<RodFabricatorRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient1").forGetter(r -> r.ingredient1),
                Codec.INT.optionalFieldOf("count1", 1).forGetter(r -> r.count1),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient2").forGetter(r -> r.ingredient2),
                Codec.INT.optionalFieldOf("count2", 1).forGetter(r -> r.count2),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("processingTime", 200).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("energyCostPerTick", 500).forGetter(r -> r.energyCostPerTick)
        ).apply(i, RodFabricatorRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, RodFabricatorRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<RodFabricatorRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, RodFabricatorRecipe> streamCodec() { return STREAM_CODEC; }

        private static RodFabricatorRecipe read(RegistryFriendlyByteBuf buf) {
            Ingredient ingredient1    = RecipeCodecs.readIngredient(buf);
            int        count1         = buf.readInt();
            Ingredient ingredient2    = RecipeCodecs.readIngredient(buf);
            int        count2         = buf.readInt();
            ItemStack  result         = RecipeCodecs.readItem(buf);
            int        processingTime = buf.readInt();
            int        energyCostPerTick = buf.readInt();
            return new RodFabricatorRecipe(ingredient1, count1, ingredient2, count2, result, processingTime, energyCostPerTick);
        }

        private static void write(RegistryFriendlyByteBuf buf, RodFabricatorRecipe r) {
            RecipeCodecs.writeIngredient(buf, r.ingredient1);
            buf.writeInt(r.count1);
            RecipeCodecs.writeIngredient(buf, r.ingredient2);
            buf.writeInt(r.count2);
            RecipeCodecs.writeItem(buf, r.result);
            buf.writeInt(r.processingTime);
            buf.writeInt(r.energyCostPerTick);
        }
    }
}
