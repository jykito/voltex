package com.jykito.industrialcore.energy;

import net.minecraftforge.energy.EnergyStorage;

/**
 * Энергобуфер машины. Снаружи (кабели, соседи) работает как обычный Forge {@link EnergyStorage}
 * с лимитами приёма и отдачи, а сама машина меняет заряд напрямую, в обход этих лимитов.
 * <p>
 * Раньше у каждой машины был свой подкласс с теми же тремя методами под разными именами.
 * Направление задаётся лимитом: у потребителя отдача 0, у генератора приём 0, и Forge по
 * нулевому лимиту сам отвечает {@code canExtract()/canReceive() = false}.
 */
public class MachineEnergy extends EnergyStorage {

    /** Потребитель: принимает до {@code maxReceive} за вызов, наружу не отдаёт. */
    public static MachineEnergy consumer(int capacity, int maxReceive) {
        return new MachineEnergy(capacity, maxReceive, 0);
    }

    /** Генератор: снаружи не принимает, отдаёт до {@code maxExtract} за вызов. */
    public static MachineEnergy generator(int capacity, int maxExtract) {
        return new MachineEnergy(capacity, 0, maxExtract);
    }

    protected MachineEnergy(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    /** Расход на работу. Ниже нуля не уходит. */
    public void consumeEnergy(int amount) {
        energy = Math.max(0, energy - amount);
    }

    /** Выработка. Излишек сверх ёмкости теряется. */
    public void addEnergy(int amount) {
        energy = (int) Math.min((long) capacity, (long) energy + amount);
    }

    /** Загрузка из NBT. Значение прижимается к 0..ёмкость. */
    public void setEnergy(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }
}
