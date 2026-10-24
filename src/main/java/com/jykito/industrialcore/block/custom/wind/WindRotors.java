package com.jykito.industrialcore.block.custom.wind;

import com.jykito.industrialcore.IndustrialCore;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wind turbine rotors: the items that go into the nacelle and what each one gives. Voltex has one, the basic rotor;
 * other mods and packs add theirs through {@code api.VoltexWind} or the KubeJS startup event {@code ICStartupEvents.windRotor}.
 * Any item can be a rotor, it is looked up by its id. Values from outside are clamped to sane limits, so a pack cannot
 * make the turbine scan half the world or overflow the numbers.
 */
public final class WindRotors {

    /**
     * What a rotor gives.
     * output: multiplier of the turbine output and of its cap (the basic rotor 1);
     * radius: blade length in blocks (the basic rotor {@link #BASE_RADIUS}), the free room around the nacelle and the drawn
     * blades grow with it;
     * tint: RGB multiplied over the blades (0xFFFFFF keeps the texture as it is);
     * texture: a texture with the layout of textures/block/wind_turbine.png for the blades, or null for the Voltex one.
     */
    public record Spec(float output, float radius, int tint, @Nullable ResourceLocation texture) {}

    public static final float BASE_RADIUS = 2.5f;
    public static final float MIN_OUTPUT = 0.01f, MAX_OUTPUT = 1000f, MIN_RADIUS = 1f, MAX_RADIUS = 6f;
    public static final ResourceLocation BASIC_ID = new ResourceLocation(IndustrialCore.MODID, "wind_turbine_rotor");
    public static final Spec BASIC = new Spec(1f, BASE_RADIUS, 0xFFFFFF, null);

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, Spec> ROTORS = new ConcurrentHashMap<>();
    static { ROTORS.put(BASIC_ID, BASIC); }

    private WindRotors() {}

    /** Registers (or replaces) a rotor item. Call during startup; out-of-range values are clamped with a warning. */
    public static void register(ResourceLocation item, Spec spec) {
        float output = clamp(item, "output", spec.output(), MIN_OUTPUT, MAX_OUTPUT);
        float radius = clamp(item, "radius", spec.radius(), MIN_RADIUS, MAX_RADIUS);
        ROTORS.put(item, new Spec(output, radius, spec.tint() & 0xFFFFFF, spec.texture()));
    }

    private static float clamp(ResourceLocation item, String what, float v, float min, float max) {
        float c = Float.isFinite(v) ? Mth.clamp(v, min, max) : min;
        if (c != v) LOGGER.warn("[Voltex] wind rotor {}: {} {} is out of {}..{}, using {}", item, what, v, min, max, c);
        return c;
    }

    /** The rotor this stack is, or null if it is not a rotor. */
    @Nullable
    public static Spec get(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? null : ROTORS.get(id);
    }
}
