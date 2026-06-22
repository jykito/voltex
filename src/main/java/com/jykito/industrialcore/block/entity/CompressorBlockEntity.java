package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.menu.CompressorMenu;
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

public class CompressorBlockEntity extends BaseMachineBlockEntity {

    public CompressorBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.COMPRESSOR_BE.get(), pPos, pBlockState);
    }

    @Override
    protected RecipeType<MachineRecipe> getRecipeType() {
        return ModRecipes.COMPRESSING_TYPE.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.compressor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new CompressorMenu(pContainerId, pPlayerInventory, this, this.baseData);
    }
}
