package com.jykito.industrialcore.nanite;

import java.util.ArrayList;
import java.util.List;

/**
 * Conditions for rare traits ({@link NaniteTrait}): the evolution chamber calls {@link #update} after every generation with what is around it.
 * The counters live in the genome; once a trait appears, it stays.
 */
public final class NaniteTraits {

    private NaniteTraits() {}

    /** Pyronite needs more than the cap: this many more generations in heat from {@link #HOT}, otherwise one at the end would be enough. */
    public static final int HOT_GENERATIONS = 20;
    public static final float HOT = 0.9f, PYRONITE_STABILITY = 0.5f, PYRONITE_CAP_SHARE = 0.95f, CHIMERA_SHARE = 0.25f;
    public static final int CHIMERA_TARGETS = 3;

    /** What was around the chamber during this generation. */
    public record Env(boolean cold, boolean field, float heat) {}

    /** Update the strain counters after a generation for target {@code target}; returns the traits that appeared now. */
    public static List<NaniteTrait> update(NaniteGenome g, NaniteGenome.Kind kind, String target, Env env) {
        List<NaniteTrait> got = new ArrayList<>();
        if (env.cold()) g.coldGens++;
        if (env.field()) g.fieldGens++;
        if (env.heat() >= HOT) g.hotGens++;
        g.stableStreak = g.stability() >= 0.999f ? g.stableStreak + 1 : 0;
        float cap = NaniteGenome.cap(kind);
        give(g, got, NaniteTrait.CRYOPHILE, g.coldGens >= NaniteTrait.COLD_GENERATIONS);
        give(g, got, NaniteTrait.RESONATOR, g.fieldGens >= NaniteTrait.FIELD_GENERATIONS);
        give(g, got, NaniteTrait.SELF_ASSEMBLER, g.stableStreak >= NaniteTrait.STABLE_STREAK);
        give(g, got, NaniteTrait.PYRONITE, env.heat() >= HOT && g.hotGens >= HOT_GENERATIONS
                && g.stability() >= PYRONITE_STABILITY && g.bonus(target) >= PYRONITE_CAP_SHARE * cap);
        int strong = 0;
        for (float v : g.affinity().values()) if (v >= cap * CHIMERA_SHARE) strong++;
        give(g, got, NaniteTrait.CHIMERA, strong >= CHIMERA_TARGETS);
        return got;
    }

    private static void give(NaniteGenome g, List<NaniteTrait> got, NaniteTrait t, boolean ok) {
        if (ok && !g.has(t)) { g.addTrait(t); got.add(t); }
    }
}
