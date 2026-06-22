package com.jykito.industrialcore.compat.kubejs;

import com.jykito.industrialcore.api.solar.SolarPanelRegistry;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class SolarPanelEventJS extends EventJS {

    public void register(String id, Object params) {
        if (!(params instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(
                    "SolarPanel.register: second argument must be an object, got " + params);
        }

        int dayOutput   = toInt(map, "dayOutput",   0);
        int nightOutput = toInt(map, "nightOutput",  0);
        int bufferSize  = toInt(map, "buffer",       10_000);
        boolean sky     = toBool(map, "requiresSky", true);

        SolarPanelRegistry.register(
                new ResourceLocation(id),
                new SolarPanelRegistry.SolarPanelDefinition(dayOutput, nightOutput, bufferSize, sky)
        );
    }

    private static int toInt(Map<?, ?> map, String key, int fallback) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v != null) throw new IllegalArgumentException("Expected number for '" + key + "', got " + v);
        return fallback;
    }

    private static boolean toBool(Map<?, ?> map, String key, boolean fallback) {
        Object v = map.get(key);
        if (v instanceof Boolean b) return b;
        if (v != null) throw new IllegalArgumentException("Expected boolean for '" + key + "', got " + v);
        return fallback;
    }
}
