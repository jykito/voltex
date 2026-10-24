package com.jykito.industrialcore.compat.kubejs;

import com.jykito.industrialcore.block.custom.wind.WindRotors;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Wind turbine rotors from a pack's startup script:
 * <pre>
 * ICStartupEvents.windRotor(event => {
 *   event.register('kubejs:steel_rotor', { output: 1.5, radius: 3, tint: 0xB0C4DE })
 * })
 * </pre>
 * output: multiplier of the output and its cap (basic rotor 1); radius: blade length in blocks (basic 2.5);
 * tint: RGB over the blades (optional); texture: full path of a texture with the layout of
 * industrial_core:textures/block/wind_turbine.png (optional). Values are clamped, see {@link WindRotors}.
 */
public class WindRotorEventJS extends EventJS {

    public void register(String id, Object params) {
        if (!(params instanceof Map<?, ?> map))
            throw new IllegalArgumentException("windRotor.register: second argument must be an object, got " + params);
        Object tex = map.get("texture");
        WindRotors.register(new ResourceLocation(id), new WindRotors.Spec(
                number(map, "output", 1f), number(map, "radius", WindRotors.BASE_RADIUS), (int) number(map, "tint", 0xFFFFFF),
                tex == null ? null : new ResourceLocation(tex.toString())));
    }

    private static float number(Map<?, ?> map, String key, float fallback) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.floatValue();
        if (v != null) throw new IllegalArgumentException("Expected number for '" + key + "', got " + v);
        return fallback;
    }
}
