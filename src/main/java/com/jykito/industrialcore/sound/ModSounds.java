package com.jykito.industrialcore.sound;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, IndustrialCore.MODID);

    public static final RegistryObject<SoundEvent> PLASMA_BURST = SOUNDS.register("plasma_burst",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(IndustrialCore.MODID, "plasma_burst")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
