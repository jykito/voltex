package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A frame block remembers its core (the core sets it when forming) and exposes the core's energy, fluid and output. */
public class IndustrialMinerFrameBlockEntity extends BlockEntity implements Caps.Provider  {

    private @Nullable BlockPos corePos;

    public IndustrialMinerFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_MINER_FRAME_BE.get(), pos, state);
    }

    public void setCore(@Nullable BlockPos core) {
        if (java.util.Objects.equals(core, corePos)) return;
        corePos = core;
        invalidateCapabilities();
        setChanged();
    }

    public @Nullable IndustrialMinerCoreBlockEntity core() {
        if (level == null || corePos == null) return null;
        return level.getBlockEntity(corePos) instanceof IndustrialMinerCoreBlockEntity c && c.isFormed() ? c : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK || cap == Capabilities.FluidHandler.BLOCK || cap == Capabilities.ItemHandler.BLOCK) {
            IndustrialMinerCoreBlockEntity c = core();
            if (c != null) return c.getCap(cap, side);
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (corePos != null) tag.putLong("core", corePos.asLong());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        corePos = tag.contains("core") ? BlockPos.of(tag.getLong("core")) : null;
    }
}
