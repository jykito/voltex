package com.jykito.industrialcore.block.entity;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MinerCasingBlockEntity extends BlockEntity implements Caps.Provider  {

    public MinerCasingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINER_CASING_BE.get(), pos, state);
    }

    private MinerCoreBlockEntity findCore() {
        if (level == null) return null;
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockEntity be = level.getBlockEntity(worldPosition.offset(dx, dy, dz));
                    if (be instanceof MinerCoreBlockEntity core) return core;
                }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        MinerCoreBlockEntity core = findCore();
        if (core != null) {
            if (cap == Capabilities.EnergyStorage.BLOCK
                    || cap == Capabilities.FluidHandler.BLOCK
                    || cap == Capabilities.ItemHandler.BLOCK) {
                return core instanceof Caps.Provider p ? p.getCap(cap, side) : null;
            }
        }
        return null;
    }
}
