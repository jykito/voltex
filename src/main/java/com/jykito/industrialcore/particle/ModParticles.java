package com.jykito.industrialcore.particle;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, IndustrialCore.MODID);

    // energy mote: circles around the coil axis and spirals into a line (the orbit centre is passed
    // through the velocity slots of sendParticles with speed=1)
    // overrideLimiter=true -> the disintegration burst shows even with the "Minimal" particle setting
    public static final RegistryObject<SimpleParticleType> ENERGY_MOTE =
            PARTICLES.register("energy_mote", () -> new SimpleParticleType(true));

    public static void register(IEventBus bus) { PARTICLES.register(bus); }
}
