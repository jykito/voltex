package com.jykito.industrialcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Блок каркаса помнит своё ядро (его ставит ядро при сборке) и отдаёт его энергию, жидкость и выход. */
public class IndustrialMinerFrameBlockEntity extends BlockEntity {

    private @Nullable BlockPos corePos;

    public IndustrialMinerFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_MINER_FRAME_BE.get(), pos, state);
    }

    public void setCore(@Nullable BlockPos core) {
        if (java.util.Objects.equals(core, corePos)) return;
        corePos = core;
        setChanged();
    }

    public @Nullable IndustrialMinerCoreBlockEntity core() {
        if (level == null || corePos == null) return null;
        return level.getBlockEntity(corePos) instanceof IndustrialMinerCoreBlockEntity c && c.isFormed() ? c : null;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY || cap == ForgeCapabilities.FLUID_HANDLER || cap == ForgeCapabilities.ITEM_HANDLER) {
            IndustrialMinerCoreBlockEntity c = core();
            if (c != null) return c.getCapability(cap, side);
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (corePos != null) tag.putLong("core", corePos.asLong());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        corePos = tag.contains("core") ? BlockPos.of(tag.getLong("core")) : null;
    }
}
