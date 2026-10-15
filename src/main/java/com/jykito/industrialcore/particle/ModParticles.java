package com.jykito.industrialcore.particle;


import net.minecraft.core.registries.Registries;
import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, IndustrialCore.MODID);

    // energy mote: circles around the coil axis and spirals into a line (the orbit centre is passed
    // through the velocity slots of sendParticles with speed=1)
    // overrideLimiter=true -> the disintegration burst shows even with the "Minimal" particle setting
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ENERGY_MOTE =
            PARTICLES.register("energy_mote", () -> new SimpleParticleType(true));

    public static void register(IEventBus bus) { PARTICLES.register(bus); }
}
