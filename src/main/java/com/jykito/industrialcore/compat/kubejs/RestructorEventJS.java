package com.jykito.industrialcore.compat.kubejs;

import com.jykito.industrialcore.api.RestructorRecipeRegistry;
import dev.latvian.mods.kubejs.event.EventJS;

import java.util.Map;

public class RestructorEventJS extends EventJS {

    public void register(Object params) {
        if (!(params instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(
                    "RestructorEventJS.register: argument must be an object, got " + params);
        }

        String input     = str(map, "input");
        String output    = str(map, "output");
        String catalyst  = str(map, "catalyst");
        int energy       = toInt(map, "energy",      1_000_000);
        int inputCount   = toInt(map, "inputCount",  1);
        int outputCount  = toInt(map, "outputCount", 1);

        RestructorRecipeRegistry.register(
                new RestructorRecipeRegistry.KubeRecipe(input, inputCount, output, outputCount, catalyst, energy));
    }

    private static String str(Map<?, ?> map, String key) {
        Object v = map.get(key);
        if (v instanceof String s) return s;
        throw new IllegalArgumentException("Expected string for '" + key + "', got " + v);
    }

    private static int toInt(Map<?, ?> map, String key, int fallback) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v != null) throw new IllegalArgumentException("Expected number for '" + key + "', got " + v);
        return fallback;
    }
}
