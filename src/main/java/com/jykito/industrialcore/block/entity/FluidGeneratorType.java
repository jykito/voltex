package com.jykito.industrialcore.block.entity;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public enum FluidGeneratorType {

    LAVA (Fluids.LAVA,    500,  160,   2000,  128_000,  256_000,  20_000, "block.industrial_core.lava_generator"),
    WATER(Fluids.WATER,   200,   80,   8000,  128_000,  256_000,  20_000, "block.industrial_core.water_generator");

    public final Fluid fluid;
    public final int fePerTick;
    public final int processTicks;
    public final int mbPerCycle;
    public final int tankCapacity;
    public final int feBuffer;
    public final int feMaxReceive;
    public final String nameKey;

    FluidGeneratorType(Fluid fluid, int fePerTick, int processTicks, int mbPerCycle,
                       int tankCapacity, int feBuffer, int feMaxReceive, String nameKey) {
        this.fluid = fluid;
        this.fePerTick = fePerTick;
        this.processTicks = processTicks;
        this.mbPerCycle = mbPerCycle;
        this.tankCapacity = tankCapacity;
        this.feBuffer = feBuffer;
        this.feMaxReceive = feMaxReceive;
        this.nameKey = nameKey;
    }
}
