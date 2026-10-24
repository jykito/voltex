package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.wire.BaseWireBlock;
import com.jykito.industrialcore.block.custom.wire.WireTier;
import com.jykito.industrialcore.block.custom.cover.CoverHolder;
import com.jykito.industrialcore.block.custom.cover.Covers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class BaseWireBlockEntity extends BlockEntity implements CoverHolder {
    private WireTier tier = WireTier.COPPER;
    /** The block this cable hides in, or null. See {@link Covers}. */
    private @Nullable BlockState cover;
    private final LazyOptional<IEnergyStorage> energyProxy;

    /**
     * A snapshot of the network's receivers that lives for ONE tick.
     * <p>
     * Before, the network was walked on every energy insert, so the cost per tick was
     * "number of pushers x network size", which grows quadratically on a large base.
     * Now the first push in a tick walks the network once and hands this
     * snapshot to ALL cables it passed, and the other generators find a fresh
     * stamp and take the ready result.
     * <p>
     * The snapshot expires by itself when the tick changes, so no invalidation is needed at all:
     * not when a cable is placed, not when a machine is broken, not when a chunk unloads. This removes
     * the whole class of "stale cache, energy flows into a removed machine" bugs: at most
     * one tick of mismatch, and it heals itself.
     */
    /** @param receivers we keep LazyOptional on purpose: if the machine is removed within the tick, the capability expires and we see it. */
    private record NetworkSnapshot(long tick, List<LazyOptional<IEnergyStorage>> receivers) {}

    private NetworkSnapshot snapshot;

    private static final int SCAN_LIMIT = 512;

    public BaseWireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRE_BE.get(), pos, state);

        if (state.getBlock() instanceof BaseWireBlock wire) {
            this.tier = wire.getTier();
        }

        this.energyProxy = LazyOptional.of(TransitEnergyStorage::new);
    }

    @Override public @Nullable BlockState getCover() { return cover; }
    @Override public void setCover(@Nullable BlockState cover) { this.cover = cover; setChanged(); }

    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); Covers.save(tag, cover); }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        BlockState oldCover = cover;
        cover = Covers.load(tag);
        // the client rebuilds the chunk so the model picks the new cover up
        if (level != null && level.isClientSide() && oldCover != cover) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = new CompoundTag(); saveAdditional(t); return t; }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

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

    /** Snapshot for the current tick: take the ready one or compute it once for the whole network. */
    private List<LazyOptional<IEnergyStorage>> receiversThisTick() {
        long now = level.getGameTime();
        if (snapshot != null && snapshot.tick() == now) return snapshot.receivers();

        List<LazyOptional<IEnergyStorage>> receivers = new ArrayList<>();
        List<BaseWireBlockEntity> wires = new ArrayList<>();

        Deque<BlockPos> queue = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet();
        BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

        queue.add(this.worldPosition);
        visited.add(this.worldPosition.asLong());

        while (!queue.isEmpty() && visited.size() < SCAN_LIMIT) {
            BlockPos currentPos = queue.poll();

            for (Direction dir : Direction.values()) {
                scratch.setWithOffset(currentPos, dir);
                long key = scratch.asLong();

                if (visited.contains(key)) continue;
                if (!level.isLoaded(scratch)) continue;
                BlockEntity be = level.getBlockEntity(scratch);
                if (be == null) continue;

                if (be instanceof BaseWireBlockEntity wire) {
                    visited.add(key);
                    queue.add(scratch.immutable());
                    wires.add(wire);
                } else {
                    var cap = be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
                    if (cap.isPresent()) {
                        IEnergyStorage storage = cap.resolve().get();
                        if (storage.canReceive()) {
                            receivers.add(cap);
                            visited.add(key);
                        }
                    }
                }
            }
        }

        NetworkSnapshot snap = new NetworkSnapshot(now, receivers);
        this.snapshot = snap;
        for (BaseWireBlockEntity w : wires) w.snapshot = snap;  // the whole network at once: this is where the multiplier goes away
        return receivers;
    }

    private int distributeEnergy(int maxAmount, boolean simulate) {

        List<LazyOptional<IEnergyStorage>> cached = receiversThisTick();

        // unpack the snapshot: a receiver may have vanished within the tick, then its capability has already expired
        List<IEnergyStorage> receivers = new ArrayList<>(cached.size());
        for (LazyOptional<IEnergyStorage> cap : cached) {
            IEnergyStorage storage = cap.resolve().orElse(null);
            if (storage != null) receivers.add(storage);
        }

        int amountLeft = maxAmount;
        int remaining  = receivers.size();
        if (remaining == 0) return 0;

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
