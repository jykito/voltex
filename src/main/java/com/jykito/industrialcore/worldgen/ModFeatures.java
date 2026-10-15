package com.jykito.industrialcore.worldgen;


import net.minecraft.core.registries.Registries;
import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, IndustrialCore.MODID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> RESIN_TREE =
            FEATURES.register("resin_tree", () -> new RubberTreeFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
