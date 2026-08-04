package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.wire.BaseWireBlock;
import com.jykito.industrialcore.block.custom.wire.WireTier;
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

public class BaseWireBlockEntity extends BlockEntity {
    private WireTier tier = WireTier.COPPER;
    private final LazyOptional<IEnergyStorage> energyProxy;

    /**
     * Снимок приёмников сети, живущий ОДИН тик.
     * <p>
     * Раньше обход сети шёл на каждую вставку энергии, поэтому цена за тик была
     * «число толкающих × размер сети» — на большой базе это квадратичный рост.
     * Теперь первый же толчок за тик обходит сеть один раз и раскладывает этот
     * снимок на ВСЕ пройденные кабели, а остальные генераторы находят свежую
     * метку и берут готовое.
     * <p>
     * Снимок протухает сам со сменой тика, поэтому инвалидация не нужна вообще:
     * ни на постановку кабеля, ни на слом машины, ни на выгрузку чанка. Это снимает
     * весь класс багов «кэш устарел, энергия течёт в снесённую машину» — максимум
     * один тик расхождения, и он самовосстанавливается.
     */
    /** @param receivers держим именно LazyOptional: если машину снесли внутри тика, капабилити протухнет и мы это увидим. */
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

    /** Снимок за текущий тик: берём готовый или считаем один раз на всю сеть. */
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
        for (BaseWireBlockEntity w : wires) w.snapshot = snap;  // вся сеть разом — вот здесь и уходит множитель
        return receivers;
    }

    private int distributeEnergy(int maxAmount, boolean simulate) {

        List<LazyOptional<IEnergyStorage>> cached = receiversThisTick();

        // разворачиваем снимок: приёмник мог исчезнуть внутри тика — тогда капабилити уже протухла
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
