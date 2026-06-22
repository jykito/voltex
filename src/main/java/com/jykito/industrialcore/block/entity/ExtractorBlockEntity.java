package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.menu.ExtractorMenu;
import com.jykito.industrialcore.recipe.MachineRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ExtractorBlockEntity extends BaseMachineBlockEntity {

    public ExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTRACTOR_BE.get(), pos, state);
    }

    @Override
    protected RecipeType<MachineRecipe> getRecipeType() {
        return ModRecipes.EXTRACTING_TYPE.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.extractor");
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ExtractorMenu(id, inventory, this, this.baseData);
    }
}
