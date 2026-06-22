package com.jykito.industrialcore.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricDrillItem extends EnergyDrillItem {

    public static final int CAPACITY       = 10_000;
    public static final int COST_PER_BLOCK = 200;

    public ElectricDrillItem(Properties properties) {
        super(Tiers.DIAMOND, CAPACITY, 6.0f, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                             BlockPos pos, LivingEntity entity) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0f) {
            drainEnergy(stack, COST_PER_BLOCK);
        }
        return true;
    }
}
