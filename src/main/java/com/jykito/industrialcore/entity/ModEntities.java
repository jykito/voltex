package com.jykito.industrialcore.entity;


import net.minecraft.core.registries.Registries;
import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, IndustrialCore.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<PlasmaWaveEntity>> PLASMA_WAVE =
            ENTITY_TYPES.register("plasma_wave", () ->
                    EntityType.Builder.<PlasmaWaveEntity>of(PlasmaWaveEntity::new, MobCategory.MISC)
                            .sized(16.0f, 16.0f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .noSave()
                            .build("plasma_wave"));

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
