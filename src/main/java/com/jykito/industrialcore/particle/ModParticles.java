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

    // энергомот: кружит вокруг оси катушки, спиралью стягивается в линию (центр орбиты передаётся
    // через velocity-слоты sendParticles при speed=1)
    // overrideLimiter=true → вспышка дезинтеграции пробивается даже при настройке партиклов «Минимум»
    public static final RegistryObject<SimpleParticleType> ENERGY_MOTE =
            PARTICLES.register("energy_mote", () -> new SimpleParticleType(true));

    public static void register(IEventBus bus) { PARTICLES.register(bus); }
}
