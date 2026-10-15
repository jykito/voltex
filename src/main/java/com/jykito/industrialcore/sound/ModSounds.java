package com.jykito.industrialcore.sound;


import net.minecraft.core.registries.Registries;
import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, IndustrialCore.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> PLASMA_BURST = SOUNDS.register("plasma_burst",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "plasma_burst")));

    // Electromagnet-accelerator: working hum loop, column pass, spin-up
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGNET_HUM = SOUNDS.register("magnet_hum",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "magnet_hum")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MAGNET_WHOOSH = SOUNDS.register("magnet_whoosh",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "magnet_whoosh")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MAGNET_POWERUP = SOUNDS.register("magnet_powerup",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "magnet_powerup")));

    // Drill: a loop while the player digs with a drill (DrillSound; the sound comes from tools/sounds/drill_sound.js)
    public static final DeferredHolder<SoundEvent, SoundEvent> DRILL_RUN = SOUNDS.register("drill_run",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "drill_run")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
