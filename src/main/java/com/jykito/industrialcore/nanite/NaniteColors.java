package com.jykito.industrialcore.nanite;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Colour by ore material or research topic: it tints the strain shell and the data card body.
 * An unknown material gets its own stable hue.
 */
public final class NaniteColors {

    private NaniteColors() {}

    /** An empty strain is silver (the tint does not change the grey drawing). */
    public static final int NONE = 0xFFFFFFFF;

    private static final Map<String, Integer> KNOWN = new HashMap<>(Map.ofEntries(
            Map.entry("iron", 0xE6C7B0), Map.entry("copper", 0xF29A5C), Map.entry("gold", 0xFFD84A),
            Map.entry("tin", 0xB9E3EE), Map.entry("lead", 0x9A9ED0), Map.entry("silver", 0xE4ECF4),
            Map.entry("nickel", 0xDCD9A6), Map.entry("aluminum", 0xD9B8A0), Map.entry("zinc", 0xA9CDBB),
            Map.entry("osmium", 0x9EC4E6), Map.entry("uranium", 0x9BE57A), Map.entry("platinum", 0xCDEBFA),
            Map.entry("diamond", 0x7FF0EC), Map.entry("emerald", 0x55E38E), Map.entry("lapis", 0x5579E6),
            Map.entry("redstone", 0xFF5545), Map.entry("coal", 0x6A6A6A), Map.entry("quartz", 0xF2EEE8),
            Map.entry("netherite", 0x9A7464), Map.entry("cobalt", 0x5A86F2), Map.entry("certus_quartz", 0xBADAFA),
            // scientist research topics
            Map.entry("crystallography", 0xB46BFF), Map.entry("cryophysics", 0x4FD8FF), Map.entry("matter", 0xFF4FC3), Map.entry("stellar", 0xFFCF33)));

    /** Colour of an add-on material or research topic (API); otherwise it gets a stable hue from its name. */
    public static synchronized void register(String key, int rgb) { KNOWN.put(key, rgb & 0xFFFFFF); }

    /** Blank data card. */
    public static final int BLANK = 0xFFB8C0C6;

    /** Data card: topic colour, a blank one is grey. */
    public static int theme(@Nullable String theme) { return theme == null ? BLANK : of(theme); }

    public static int of(@Nullable String material) {
        if (material == null) return NONE;
        Integer c = KNOWN.get(material);
        if (c != null) return 0xFF000000 | c;
        float hue = (material.hashCode() & 0xFFFF) / 65536f;
        return 0xFF000000 | Mth.hsvToRgb(hue, 0.45f, 0.95f);
    }
}
