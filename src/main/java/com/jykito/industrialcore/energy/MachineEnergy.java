package com.jykito.industrialcore.energy;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Machine energy buffer. From outside (cables, neighbours) it works as a regular Forge {@link EnergyStorage}
 * with intake and output limits, and the machine itself changes the charge directly, bypassing those limits.
 * <p>
 * Before, every machine had its own subclass with the same three methods under different names.
 * The direction is set by the limit: a consumer has output 0, a generator has intake 0, and Forge
 * answers {@code canExtract()/canReceive() = false} by itself for a zero limit.
 */
public class MachineEnergy extends EnergyStorage {

    /** Consumer: accepts up to {@code maxReceive} per call, gives nothing outside. */
    public static MachineEnergy consumer(int capacity, int maxReceive) {
        return new MachineEnergy(capacity, maxReceive, 0);
    }

    /** Generator: accepts nothing from outside, gives up to {@code maxExtract} per call. */
    public static MachineEnergy generator(int capacity, int maxExtract) {
        return new MachineEnergy(capacity, 0, maxExtract);
    }

    protected MachineEnergy(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    /** Spend for work. Does not go below zero. */
    public void consumeEnergy(int amount) {
        energy = Math.max(0, energy - amount);
    }

    /** Generation. Surplus above capacity is lost. */
    public void addEnergy(int amount) {
        energy = (int) Math.min((long) capacity, (long) energy + amount);
    }

    /** Load from NBT. The value is clamped to 0..capacity. */
    public void setEnergy(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }
}
