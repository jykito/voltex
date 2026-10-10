package com.jykito.industrialcore.nanite;

import java.util.ArrayList;
import java.util.List;

/**
 * Условия редких черт ({@link NaniteTrait}): камера эволюции зовёт {@link #update} после каждого поколения с тем, что вокруг.
 * Счётчики живут в геноме, черта, раз появившись, остаётся.
 */
public final class NaniteTraits {

    private NaniteTraits() {}

    /** Пироните мало потолка: нужно ещё столько поколений на тепле от {@link #HOT}, иначе хватило бы одного в конце. */
    public static final int HOT_GENERATIONS = 20;
    public static final float HOT = 0.9f, PYRONITE_STABILITY = 0.5f, PYRONITE_CAP_SHARE = 0.95f, CHIMERA_SHARE = 0.25f;
    public static final int CHIMERA_TARGETS = 3;

    /** Что было вокруг камеры в это поколение. */
    public record Env(boolean cold, boolean field, float heat) {}

    /** Обновить счётчики штамма после поколения под цель {@code target}; возвращает черты, появившиеся сейчас. */
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
