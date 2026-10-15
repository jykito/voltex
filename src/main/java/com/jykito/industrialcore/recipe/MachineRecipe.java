package com.jykito.industrialcore.recipe;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;

import net.minecraft.core.HolderLookup;
import com.google.gson.JsonObject;
import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

public class MachineRecipe implements Recipe<ContainerInput> {
    /** Recipe for the late machine only (its industrial mode); the small machine does not see it. */
    private boolean lateOnly;
    private final Ingredient input;
    private final int inputCount;
    private final ItemStack output;
    private final int processingTime;
    private final int energyCost;
    @Nullable private final String catalystId;
    private final RecipeType<?> type;

    public MachineRecipe(Ingredient input, int inputCount, ItemStack output,
                         int processingTime, int energyCost, @Nullable String catalystId, RecipeType<?> type) {
        this.input = input;
        this.inputCount = inputCount;
        this.output = output;
        this.processingTime = processingTime;
        this.energyCost = energyCost;
        this.catalystId = catalystId;
        this.type = type;
    }

    public boolean isLateOnly() { return lateOnly; }

    /** For the typed serializers (crushing, rolling, ...; KubeJS goes through them): the "late only" marker. */
    MachineRecipe lateOnly(boolean v) { this.lateOnly = v; return this; }

    @Override
    public boolean matches(ContainerInput pContainer, Level pLevel) {
        if (lateOnly) return false;
        if (pLevel.isClientSide()) return false;
        if (!input.test(pContainer.getItem(0)) || pContainer.getItem(0).getCount() < inputCount) return false;
        if (catalystId == null) return true;
        ItemStack cat = pContainer.getContainerSize() > 1 ? pContainer.getItem(1) : ItemStack.EMPTY;
        if (cat.isEmpty()) return false;
        ResourceLocation catKey = BuiltInRegistries.ITEM.getKey(cat.getItem());
        return catKey != null && catKey.toString().equals(catalystId);
    }

    @Nullable public String getCatalystId() { return catalystId; }

    @Override
    public ItemStack assemble(ContainerInput pContainer, HolderLookup.Provider pRegistryAccess) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override public boolean isSpecial() { return true; }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistryAccess) {
        return output.copy();
    }

    public int getProcessingTime() { return processingTime; }
    public int getEnergyCost() { return energyCost; }
    public int getInputCount() { return inputCount; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.input);
        return list;
    }


    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipes.MACHINE_SERIALIZER.get(); }

    @Override
    public RecipeType<?> getType() { return type; }

    /** "machine_type" of the generic serializer: only the mod's own recipe types, as before. */
    static final Codec<RecipeType<?>> MACHINE_TYPE = ResourceLocation.CODEC.comapFlatMap(id -> {
        for (var entry : ModRecipes.RECIPE_TYPES.getEntries())
            if (entry.getId().equals(id)) return com.mojang.serialization.DataResult.success(entry.get());
        return com.mojang.serialization.DataResult.error(() -> "Unknown machine_type: " + id);
    }, type -> BuiltInRegistries.RECIPE_TYPE.getKey(type));

    /** Network part shared by the generic and the typed serializers (the type is written by the generic one only). */
    static void writeCommon(RegistryFriendlyByteBuf buf, MachineRecipe r) {
        RecipeCodecs.writeIngredient(buf, r.input);
        buf.writeInt(r.inputCount);
        RecipeCodecs.writeItem(buf, r.output);
        buf.writeInt(r.processingTime);
        buf.writeInt(r.energyCost);
        buf.writeBoolean(r.catalystId != null);
        if (r.catalystId != null) buf.writeUtf(r.catalystId);
    }

    static MachineRecipe readCommon(RegistryFriendlyByteBuf buf, java.util.function.Supplier<RecipeType<?>> type) {
        Ingredient input   = RecipeCodecs.readIngredient(buf);
        int inputCount     = buf.readInt();
        ItemStack output   = RecipeCodecs.readItem(buf);
        int processingTime = buf.readInt();
        int energyCost     = buf.readInt();
        String catalystId  = buf.readBoolean() ? buf.readUtf() : null;
        return new MachineRecipe(input, inputCount, output, processingTime, energyCost, catalystId, type.get());
    }

    public static class Serializer implements RecipeSerializer<MachineRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<MachineRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                MACHINE_TYPE.fieldOf("machine_type").forGetter(r -> r.type),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.input),
                Codec.INT.optionalFieldOf("inputCount", 1).forGetter(r -> r.inputCount),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.output),
                Codec.INT.optionalFieldOf("processingTime", 200).forGetter(r -> r.processingTime),
                Codec.INT.optionalFieldOf("energyCost", 50).forGetter(r -> r.energyCost),
                Codec.STRING.optionalFieldOf("catalyst").forGetter(r -> java.util.Optional.ofNullable(r.catalystId)),
                Codec.BOOL.optionalFieldOf("lateOnly", false).forGetter(r -> r.lateOnly)
        ).apply(i, (type, input, inputCount, output, time, energy, catalyst, lateOnly) ->
                new MachineRecipe(input, inputCount, output, time, energy, catalyst.orElse(null), type).lateOnly(lateOnly)));

        private static final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::write, Serializer::read);

        @Override public MapCodec<MachineRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() { return STREAM_CODEC; }

        private static MachineRecipe read(RegistryFriendlyByteBuf buf) {
            MachineRecipe partial = readCommon(buf, ModRecipes.CRUSHING_TYPE::get);
            ResourceLocation typeId = buf.readResourceLocation();
            RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.get(typeId);
            MachineRecipe recipe = new MachineRecipe(partial.input, partial.inputCount, partial.output,
                    partial.processingTime, partial.energyCost, partial.catalystId, type != null ? type : ModRecipes.CRUSHING_TYPE.get());
            return recipe.lateOnly(buf.readBoolean());
        }

        private static void write(RegistryFriendlyByteBuf buf, MachineRecipe r) {
            writeCommon(buf, r);
            buf.writeResourceLocation(BuiltInRegistries.RECIPE_TYPE.getKey(r.getType()));
            buf.writeBoolean(r.lateOnly);
        }
    }
}
