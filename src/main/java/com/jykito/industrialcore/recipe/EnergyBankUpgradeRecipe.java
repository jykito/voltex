package com.jykito.industrialcore.recipe;

import com.google.gson.JsonObject;
import com.jykito.industrialcore.block.custom.EnergyBankBlock;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

/**
 * A shaped crafting recipe that carries the charge and reserve of the energy bank in the grid over to the result.
 * Everything else (pattern, JEI display, recipe book) is the vanilla shaped recipe.
 */
public class EnergyBankUpgradeRecipe extends ShapedRecipe {

    public EnergyBankUpgradeRecipe(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
                base.getIngredients(), base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        ItemStack out = super.assemble(grid, access);
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack in = grid.getItem(i);
            if (!(in.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof EnergyBankBlock)) continue;
            CompoundTag old = BlockItem.getBlockEntityData(in);
            if (old == null || !(out.getItem() instanceof BlockItem outItem) || !(outItem.getBlock() instanceof EnergyBankBlock target)) break;
            CompoundTag tag = new CompoundTag();
            tag.putInt("energy", Math.min(old.getInt("energy"), target.tier.capacity));
            tag.putInt("reserve", old.getInt("reserve"));
            out.getOrCreateTag().put("BlockEntityTag", tag);
            break;
        }
        return out;
    }

    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    /** Reads and writes exactly like the vanilla shaped serializer and wraps the result. */
    public static class Serializer implements RecipeSerializer<EnergyBankUpgradeRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public EnergyBankUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new EnergyBankUpgradeRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public @Nullable EnergyBankUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
            return base == null ? null : new EnergyBankUpgradeRecipe(base);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, EnergyBankUpgradeRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
    }
}
