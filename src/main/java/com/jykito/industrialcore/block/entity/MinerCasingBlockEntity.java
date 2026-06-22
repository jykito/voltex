package com.jykito.industrialcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MinerCasingBlockEntity extends BlockEntity {

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
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        MinerCoreBlockEntity core = findCore();
        if (core != null) {
            if (cap == ForgeCapabilities.ENERGY
                    || cap == ForgeCapabilities.FLUID_HANDLER
                    || cap == ForgeCapabilities.ITEM_HANDLER) {
                return core.getCapability(cap, side);
            }
        }
        return super.getCapability(cap, side);
    }
}
