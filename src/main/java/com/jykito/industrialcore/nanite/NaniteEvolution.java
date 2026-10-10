package com.jykito.industrialcore.nanite;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

/**
 * Одно поколение отбора в камере эволюции. Из штамма вырастает {@link #CANDIDATES} потомков со случайными сдвигами,
 * выживает лучший под целевую руду; родитель тоже в отборе, поэтому сродство к цели не падает.
 * <p>
 * Рост замедляется у потолка (шаг пропорционален запасу до потолка класса {@link NaniteGenome#cap}), отсюда плато на графике.
 * У переработчика шаг мельче: около +3–4% за поколение до потолка 30% (у остальных около +7% до +300%).
 * Тепло крупнее шаги и быстрее поколения (это считает камера), но каждое горячее поколение снижает стабильность;
 * без тепла стабильность понемногу восстанавливается.
 */
public final class NaniteEvolution {

    private NaniteEvolution() {}

    public static final int CANDIDATES = 8;
    private static final float STEP = 0.05f, STEP_HEAT = 0.07f;
    /** Переработчик: лучший из 8 потомков даёт около 1.4σ, то есть +3.5% за поколение в начале. */
    private static final float PROCESSOR_STEP = 0.025f, PROCESSOR_STEP_HEAT = 0.035f;
    /** Учёный: около +4% изученности за поколение в начале; до 90% (звёздная физика) — порядка 55 поколений без тепла. */
    private static final float SCIENTIST_STEP = 0.03f, SCIENTIST_STEP_HEAT = 0.04f;
    private static final float DRIFT = 0.02f, FLUID_STEP = 0.03f, FLUID_WEIGHT = 0.25f;
    private static final float STABILITY_LOSS = 0.03f, STABILITY_GAIN = 0.02f, COLD = 0.05f;

    /**
     * Новое поколение штамма под цель {@code target} (материал или тема); {@code heat} — доля тепла камеры 0..1.
     * Пустой штамм получает класс корма; штамм другого класса камера сюда не пускает.
     */
    public static NaniteGenome step(NaniteGenome parent, NaniteGenome.Kind kind, String target, Item feed, float heat, RandomSource rnd) {
        NaniteGenome best = parent;
        boolean miner = kind == NaniteGenome.Kind.MINER;
        float bestFit = fitness(parent, target, miner);
        boolean processor = kind == NaniteGenome.Kind.PROCESSOR;
        float sigma = processor ? PROCESSOR_STEP + PROCESSOR_STEP_HEAT * heat
                : kind == NaniteGenome.Kind.SCIENTIST ? SCIENTIST_STEP + SCIENTIST_STEP_HEAT * heat
                : STEP + STEP_HEAT * heat;
        float cap = NaniteGenome.cap(kind);
        for (int i = 0; i < CANDIDATES; i++) {
            NaniteGenome child = mutate(parent, target, sigma, cap, miner, rnd);
            float fit = fitness(child, target, miner);
            if (fit > bestFit) { best = child; bestFit = fit; }
        }
        NaniteGenome out = best == parent ? parent.copy() : best;
        if (out.kind() == null) out.setKind(kind);
        out.rememberIcon(target, feed);
        float loss = out.has(NaniteTrait.CRYOPHILE) ? 0f : STABILITY_LOSS * heat;   // криофилу тепло не страшно
        out.setStability(out.stability() + (heat > COLD ? -loss : STABILITY_GAIN));
        out.nextGeneration();
        return out;
    }

    /** Сродство к цели; у добытчика в зачёт идёт и экономия буровой жижи (у остальных жижи нет). */
    public static float fitness(NaniteGenome g, String target, boolean miner) {
        return g.bonus(target) + (miner ? FLUID_WEIGHT * (1f - g.fluid()) : 0f);
    }

    private static NaniteGenome mutate(NaniteGenome parent, String target, float sigma, float cap, boolean miner, RandomSource rnd) {
        NaniteGenome c = parent.copy();
        float own = c.bonus(target), headroom = Math.max(0.05f, 1f - own / cap);
        c.setAffinity(target, own + (float) rnd.nextGaussian() * sigma * headroom);
        for (String m : c.affinity().keySet().toArray(String[]::new))
            if (!m.equals(target)) c.setAffinity(m, c.bonus(m) + (float) rnd.nextGaussian() * DRIFT * cap / NaniteGenome.BUDGET);
        if (miner) c.setFluid(c.fluid() + (float) rnd.nextGaussian() * FLUID_STEP);
        c.fitBudget(target, cap);
        return c;
    }
}
