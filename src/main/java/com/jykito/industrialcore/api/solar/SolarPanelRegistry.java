package com.jykito.industrialcore.api.solar;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SolarPanelRegistry {

    private static final Map<ResourceLocation, SolarPanelDefinition> PANELS = new LinkedHashMap<>();

    private SolarPanelRegistry() {}

    public static void register(ResourceLocation id, SolarPanelDefinition def) {
        if (PANELS.containsKey(id))
            throw new IllegalArgumentException("Solar panel already registered: " + id);
        PANELS.put(id, def);
    }

    @Nullable
    public static SolarPanelDefinition get(ResourceLocation id) {
        return PANELS.get(id);
    }

    public static Map<ResourceLocation, SolarPanelDefinition> getAll() {
        return Collections.unmodifiableMap(PANELS);
    }

    public record SolarPanelDefinition(
            int dayOutput,
            int nightOutput,
            int bufferSize,
            boolean requiresSky
    ) {}
}
