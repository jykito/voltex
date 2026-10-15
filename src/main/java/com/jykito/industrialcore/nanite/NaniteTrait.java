package com.jykito.industrialcore.nanite;

import org.jetbrains.annotations.Nullable;

/**
 * Rare strain traits are the rare species of the atlas (author, 2026-10-07: "a species atlas in the information panel"). They appear only
 * under special selection conditions in the evolution chamber ({@link NaniteTraits}), stay forever and give a special effect at work.
 * Numbers are preliminary, balance comes later.
 */
public enum NaniteTrait {
    /** 20 generations in the cold: the chamber right next to a cryo branch block. Heat no longer lowers stability. */
    CRYOPHILE(0xFF8FE6FF),
    /** Affinity cap reached in heat from 90% with stability from 50%. Effect strength x1.2. */
    PYRONITE(0xFFFF7A3A),
    /** 20 generations in a field: a magnet-accelerator core within 8 blocks of the chamber. 15% chance the effect triggers twice. */
    RESONATOR(0xFFB46BFF),
    /** Affinity from a quarter of the cap to three targets at once. On a foreign target it works at half its best affinity. */
    CHIMERA(0xFF7DFF9A),
    /** 100 generations in a row at full stability. Does not wear out. */
    SELF_ASSEMBLER(0xFFF2F6F8);

    private final int color;
    NaniteTrait(int color) { this.color = color; }
    /** Spider colour on a rare species icon (atlas, toast). */
    public int color() { return color; }

    public static final int COLD_GENERATIONS = 20, FIELD_GENERATIONS = 20, STABLE_STREAK = 100;
    public static final float PYRONITE_POWER = 1.2f, RESONATOR_CHANCE = 0.15f, CHIMERA_SHARE = 0.5f;

    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }

    public static @Nullable NaniteTrait byKey(String k) {
        for (NaniteTrait t : values()) if (t.key().equals(k)) return t;
        return null;
    }
}
