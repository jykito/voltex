package com.jykito.industrialcore.entity;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, IndustrialCore.MODID);

    public static final RegistryObject<EntityType<PlasmaWaveEntity>> PLASMA_WAVE =
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
