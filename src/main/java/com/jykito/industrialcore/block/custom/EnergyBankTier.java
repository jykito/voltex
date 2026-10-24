package com.jykito.industrialcore.block.custom;

/**
 * Energy bank tiers. T1 keeps the original numbers; T3 sits just under the Forge energy limit (int), which cables
 * cannot carry past anyway.
 */
public enum EnergyBankTier {
    T1(10_000_000, 100_000),
    T2(100_000_000, 1_000_000),
    T3(2_000_000_000, 100_000_000);

    public final int capacity;
    /** Per-tick limit for both intake and output. */
    public final int ioRate;

    EnergyBankTier(int capacity, int ioRate) {
        this.capacity = capacity;
        this.ioRate = ioRate;
    }
}
