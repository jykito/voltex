package com.jykito.industrialcore.api;

import com.jykito.industrialcore.block.custom.wind.WindRotors;
import net.minecraft.resources.ResourceLocation;

/**
 * Wind turbine rotors for add-ons. Voltex has one, the basic rotor ({@code industrial_core:wind_turbine_rotor}); any item
 * registered here goes into the nacelle the same way (right click with it), gets its own tooltip and is drawn on the
 * turbine with the Voltex blade model. Call during mod setup. Packs do the same from KubeJS with the startup event
 * {@code ICStartupEvents.windRotor}.
 * <p>
 * Values are clamped: output 0.01..1000, radius 1..6 blocks.
 */
public final class VoltexWind {

    private VoltexWind() {}

    /**
     * @param item   the rotor item
     * @param output multiplier of the turbine output and of its cap (the basic rotor 1)
     * @param radius blade length in blocks (the basic rotor 2.5); the free room around the nacelle and the drawn blades follow it
     * @param tint   RGB multiplied over the blades, 0xFFFFFF keeps the texture as it is
     */
    public static void rotor(ResourceLocation item, float output, float radius, int tint) {
        WindRotors.register(item, new WindRotors.Spec(output, radius, tint, null));
    }

    /**
     * The same with own blade colours: {@code texture} is a full texture path (for example
     * {@code mymod:textures/block/steel_rotor.png}) with the layout of {@code industrial_core:textures/block/wind_turbine.png};
     * recolour the blade tiles of a copy of it.
     */
    public static void rotor(ResourceLocation item, float output, float radius, int tint, ResourceLocation texture) {
        WindRotors.register(item, new WindRotors.Spec(output, radius, tint, texture));
    }
}
