package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.wire.BaseWireBlock;
import com.jykito.industrialcore.block.custom.wire.WireTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class BaseWireBlockEntity extends BlockEntity {
    private WireTier tier = WireTier.COPPER;
    private final LazyOptional<IEnergyStorage> energyProxy;

    public BaseWireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRE_BE.get(), pos, state);

        if (state.getBlock() instanceof BaseWireBlock wire) {
            this.tier = wire.getTier();
        }

        this.energyProxy = LazyOptional.of(TransitEnergyStorage::new);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyProxy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyProxy.invalidate();
    }

    private class TransitEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (level == null || level.isClientSide) return 0;

            int actualReceive = Math.min(maxReceive, tier.getMaxTransferRate());
            if (actualReceive <= 0) return 0;

            return distributeEnergy(actualReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override
        public int getEnergyStored() { return 0; }
        @Override
        public int getMaxEnergyStored() { return 0; }
        @Override
        public boolean canExtract() { return false; }
        @Override
        public boolean canReceive() { return true; }
    }

    private int distributeEnergy(int maxAmount, boolean simulate) {

        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        List<IEnergyStorage> receivers = new ArrayList<>();
        queue.add(this.worldPosition);
        visited.add(this.worldPosition);

        int scanLimit = 512;
        while (!queue.isEmpty() && visited.size() < scanLimit) {
            BlockPos currentPos = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(dir);

                if (visited.contains(neighborPos)) continue;
                if (!level.isLoaded(neighborPos)) continue;
                BlockEntity be = level.getBlockEntity(neighborPos);
                if (be == null) continue;

                if (be instanceof BaseWireBlockEntity) {
                    visited.add(neighborPos);
                    queue.add(neighborPos);
                } else {
                    var cap = be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
                    if (cap.isPresent()) {
                        IEnergyStorage storage = cap.resolve().get();
                        if (storage.canReceive()) {
                            receivers.add(storage);
                            visited.add(neighborPos);
                        }
                    }
                }
            }
        }

        int receiverCount = receivers.size();
        if (receiverCount == 0) return 0;

        int amountLeft = maxAmount;
        int remaining  = receiverCount;

        for (IEnergyStorage storage : receivers) {
            if (amountLeft <= 0) break;
            int share = amountLeft / remaining;
            if (share <= 0) share = amountLeft;
            int accepted = storage.receiveEnergy(Math.min(share, amountLeft), simulate);
            amountLeft -= accepted;
            remaining--;
        }

        if (amountLeft > 0) {
            for (IEnergyStorage storage : receivers) {
                if (amountLeft <= 0) break;
                int accepted = storage.receiveEnergy(amountLeft, simulate);
                amountLeft -= accepted;
            }
        }

        return maxAmount - amountLeft;
    }
}
