package com.jykito.industrialcore.nanite;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Processor in the ore washer (small and multiblock): for each operation, with chance {@link NaniteGenome#secondOutputChance} (up to 30%)
 * the main product comes out once more. The material comes from the recipe input (raw material, ore). It does not touch time, that is the accelerators' job.
 * Wear like the miner's, but rarer: the ore washer does far more operations than the miner does cycles.
 */
public final class NaniteProcessing {

    private NaniteProcessing() {}

    public static final float WEAR_CHANCE = 0.01f, WEAR = 0.97f;

    /** How many extra products {@code ops} operations gave (a resonator sometimes gives two per operation). */
    public static int extra(Level level, ItemStack strain, Item input, int ops) {
        if (strain.isEmpty() || ops <= 0) return 0;
        NaniteGenome g = NaniteGenome.read(strain);
        if (!g.is(NaniteGenome.Kind.PROCESSOR)) return 0;
        float chance = g.secondOutputChance(OreMaterial.any(input));
        if (chance <= 0) return 0;
        int n = 0;
        boolean resonator = g.has(NaniteTrait.RESONATOR);
        for (int i = 0; i < ops; i++) if (level.random.nextFloat() < chance) {
            n++;
            if (resonator && level.random.nextFloat() < NaniteTrait.RESONATOR_CHANCE) n++;   // resonance: one more
        }
        return n;
    }

    /** Strain after {@code ops} operations: the same item if no wear happened, otherwise a worn copy. */
    public static ItemStack wear(Level level, ItemStack strain, int ops) {
        if (strain.isEmpty() || ops <= 0) return strain;
        NaniteGenome g = NaniteGenome.read(strain);
        if (!g.is(NaniteGenome.Kind.PROCESSOR) || !g.wears()) return strain;
        float chance = (1f - g.stability()) * WEAR_CHANCE;
        boolean worn = false;
        for (int i = 0; i < ops; i++) if (level.random.nextFloat() < chance) { g.decay(WEAR); worn = true; }
        if (!worn) return strain;
        ItemStack out = strain.copy();
        g.write(out);
        return out;
    }
}
