package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.menu.MetalFormerMenu;
import com.jykito.industrialcore.recipe.MachineRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MetalFormerBlockEntity extends BaseMachineBlockEntity {
    protected final ContainerData data;

    private int currentMode = 0;

    public MetalFormerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.METAL_FORMER_BE.get(), pPos, pBlockState);

        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> MetalFormerBlockEntity.this.energyStorage.getEnergyStored() & 0xFFFF;
                    case 1 -> (MetalFormerBlockEntity.this.energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 2 -> MetalFormerBlockEntity.this.energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 3 -> (MetalFormerBlockEntity.this.energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> MetalFormerBlockEntity.this.progress;
                    case 5 -> MetalFormerBlockEntity.this.maxProgress;
                    case 6 -> MetalFormerBlockEntity.this.currentMode;
                    default -> 0;
                };
            }
            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 4 -> MetalFormerBlockEntity.this.progress = pValue;
                    case 5 -> MetalFormerBlockEntity.this.maxProgress = pValue;
                    case 6 -> MetalFormerBlockEntity.this.currentMode = pValue;
                }
            }
            @Override
            public int getCount() { return 7; }
        };
    }

    @Override
    protected RecipeType<MachineRecipe> getRecipeType() {
        return switch (this.currentMode) {
            case 1 -> ModRecipes.EXTRUDING_TYPE.get();
            default -> ModRecipes.ROLLING_TYPE.get();
        };
    }

    public void cycleMode() {

        this.currentMode = (this.currentMode + 1) % 2;
        this.progress = 0;
        setChanged();
    }

    public int getCurrentMode() {
        return currentMode;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putInt("metal_former_mode", currentMode);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.currentMode = tag.getInt("metal_former_mode") == 1 ? 1 : 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.metal_former");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new MetalFormerMenu(pContainerId, pPlayerInventory, this, this.data);
    }
}
