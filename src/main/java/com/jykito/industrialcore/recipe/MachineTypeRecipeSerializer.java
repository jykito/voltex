package com.jykito.industrialcore.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Typed serializer for {@link MachineRecipe} (crushing, rolling, ...): the same json as the generic
 * {@code machine_recipe}, but the recipe type is fixed by the serializer instead of a "machine_type" field.
 * KubeJS writes machine recipes through these.
 */
public class MachineTypeRecipeSerializer implements RecipeSerializer<MachineRecipe> {

    private final MapCodec<MachineRecipe> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec;

    public MachineTypeRecipeSerializer(Supplier<RecipeType<?>> type) {
        this.codec = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.getIngredients().get(0)),
                Codec.INT.optionalFieldOf("inputCount", 1).forGetter(MachineRecipe::getInputCount),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.getResultItem(null)),
                Codec.INT.optionalFieldOf("processingTime", 200).forGetter(MachineRecipe::getProcessingTime),
                Codec.INT.optionalFieldOf("energyCost", 50).forGetter(MachineRecipe::getEnergyCost),
                Codec.STRING.optionalFieldOf("catalyst").forGetter(r -> Optional.ofNullable(r.getCatalystId())),
                Codec.BOOL.optionalFieldOf("lateOnly", false).forGetter(MachineRecipe::isLateOnly)
        ).apply(i, (input, inputCount, output, time, energy, catalyst, lateOnly) ->
                new MachineRecipe(input, inputCount, output, time, energy, catalyst.orElse(null), type.get()).lateOnly(lateOnly)));
        this.streamCodec = StreamCodec.of(
                (buf, r) -> { MachineRecipe.writeCommon(buf, r); buf.writeBoolean(r.isLateOnly()); },
                buf -> MachineRecipe.readCommon(buf, type).lateOnly(buf.readBoolean()));
    }

    @Override public MapCodec<MachineRecipe> codec() { return codec; }
    @Override public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() { return streamCodec; }
}
