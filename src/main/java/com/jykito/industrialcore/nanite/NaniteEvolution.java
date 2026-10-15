package com.jykito.industrialcore.nanite;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

/**
 * One selection generation in the evolution chamber. The strain produces {@link #CANDIDATES} offspring with random shifts,
 * the best one for the target ore survives; the parent is in the selection too, so the affinity to the target does not drop.
 * <p>
 * Growth slows near the cap (the step is proportional to the room left to the class cap {@link NaniteGenome#cap}), hence the plateau on the graph.
 * A processor has a finer step: about +3-4% per generation up to the 30% cap (the others about +7% up to +300%).
 * Heat gives larger steps and faster generations (the chamber computes that), but every hot generation lowers stability;
 * without heat stability slowly recovers.
 */
public final class NaniteEvolution {

    private NaniteEvolution() {}

    public static final int CANDIDATES = 8;
    private static final float STEP = 0.05f, STEP_HEAT = 0.07f;
    /** Processor: the best of 8 offspring gives about 1.4σ, that is +3.5% per generation at the start. */
    private static final float PROCESSOR_STEP = 0.025f, PROCESSOR_STEP_HEAT = 0.035f;
    /** Scientist: about +4% knowledge per generation at the start; up to 90% (stellar physics) takes around 55 generations without heat. */
    private static final float SCIENTIST_STEP = 0.03f, SCIENTIST_STEP_HEAT = 0.04f;
    private static final float DRIFT = 0.02f, FLUID_STEP = 0.03f, FLUID_WEIGHT = 0.25f;
    private static final float STABILITY_LOSS = 0.03f, STABILITY_GAIN = 0.02f, COLD = 0.05f;

    /**
     * A new strain generation for target {@code target} (material or topic); {@code heat} is the chamber heat fraction 0..1.
     * An empty strain gets the feed's class; the chamber does not let a strain of another class in.
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
        float loss = out.has(NaniteTrait.CRYOPHILE) ? 0f : STABILITY_LOSS * heat;   // heat does not bother a cryophile
        out.setStability(out.stability() + (heat > COLD ? -loss : STABILITY_GAIN));
        out.nextGeneration();
        return out;
    }

    /** Affinity to the target; for a miner, drill fluid savings count too (the others use no fluid). */
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
