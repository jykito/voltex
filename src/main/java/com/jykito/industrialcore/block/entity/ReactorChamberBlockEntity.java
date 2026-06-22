package com.jykito.industrialcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReactorChamberBlockEntity extends BlockEntity {

    public ReactorChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_CHAMBER_BE.get(), pos, state);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (level != null) {

            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockEntity be = level.getBlockEntity(worldPosition.offset(dx, dy, dz));
                        if (be instanceof NuclearReactorBlockEntity reactor) {
                            return reactor.getCapability(cap, side);
                        }
                    }
        }
        return super.getCapability(cap, side);
    }
}
