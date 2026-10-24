package com.jykito.industrialcore.recipe;

import com.jykito.industrialcore.item.EnergyBankItem;
import com.jykito.industrialcore.item.ModDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped recipe that upgrades an energy bank: the result takes the charge and the reserve of the bank in the grid.
 * The json is the vanilla shaped format with this type, so JEI shows it as a normal 3×3 recipe.
 */
public class EnergyBankUpgradeRecipe extends ShapedRecipe {

    private final ShapedRecipePattern pattern;
    private final ItemStack result;

    public EnergyBankUpgradeRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public ItemStack assemble(CraftingInput grid, HolderLookup.Provider registries) {
        ItemStack out = super.assemble(grid, registries);
        for (int i = 0; i < grid.size(); i++) {
            ItemStack in = grid.getItem(i);
            if (!(in.getItem() instanceof EnergyBankItem)) continue;
            Integer energy = in.get(ModDataComponents.ENERGY.get()), reserve = in.get(ModDataComponents.BANK_RESERVE.get());
            if (energy != null) out.set(ModDataComponents.ENERGY.get(), energy);
            if (reserve != null) out.set(ModDataComponents.BANK_RESERVE.get(), reserve);
            break;
        }
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ENERGY_BANK_UPGRADE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<EnergyBankUpgradeRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<EnergyBankUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification)
        ).apply(i, EnergyBankUpgradeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, EnergyBankUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ShapedRecipe::getGroup,
                CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
                ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
                ItemStack.STREAM_CODEC, r -> r.result,
                ByteBufCodecs.BOOL, ShapedRecipe::showNotification,
                EnergyBankUpgradeRecipe::new);

        @Override
        public MapCodec<EnergyBankUpgradeRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EnergyBankUpgradeRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
