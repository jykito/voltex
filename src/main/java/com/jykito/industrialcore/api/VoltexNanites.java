package com.jykito.industrialcore.api;

import com.jykito.industrialcore.nanite.NaniteColors;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.industrialcore.nanite.NaniteLab;
import com.jykito.industrialcore.nanite.NaniteTraitRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Nanites for add-ons. Call everything during mod setup (common side), before worlds load.
 * <p>
 * Strains are read with {@link NaniteGenome#read}; a strain's class is {@link NaniteGenome#kind()}, its affinity to a
 * target {@link NaniteGenome#effect}, its traits {@link NaniteGenome#hasTrait}.
 */
public final class VoltexNanites {

    private VoltexNanites() {}

    /**
     * A new nanite class, like a new bee species: what feed makes it, what it selects for, how fast. Voltex breeds it in
     * the evolution chamber and lists it in the atlas; it works in the add-on's machines. Finish with {@code register()}.
     * Lang keys mirror the built-in ones in the add-on namespace, for example {@code nanite_species.<modid>.<name>}
     * and {@code tooltip.<modid>.nanite_strain.kind.<name>} (see {@link NaniteGenome.Kind#lang}).
     */
    public static NaniteGenome.Kind.Builder kind(ResourceLocation id) {
        return NaniteGenome.Kind.builder(id);
    }

    /**
     * A scientist research topic. Samples go into the item tag {@code industrial_core:nanite_research/<topic>}, the name
     * into {@code nanite_theme.industrial_core.<topic>}; the lab writes {@code cardItem} (an add-on {@code DataCardItem})
     * once a scientist's knowledge reaches {@code threshold}.
     */
    public static void scientistTopic(String topic, float threshold, ResourceLocation cardItem, int rgb) {
        NaniteLab.registerTopic(topic, threshold, cardItem);
        NaniteColors.register(topic, rgb);
    }

    /** Colour of an add-on material or target on strains and in the atlas. */
    public static void color(String target, int rgb) {
        NaniteColors.register(target, rgb);
    }

    /** A rare trait that appears in the evolution chamber when {@code condition} holds after a generation. */
    public static void trait(ResourceLocation id, int argbColor, NaniteTraitRegistry.Condition condition) {
        NaniteTraitRegistry.registerTrait(id, argbColor, condition);
    }

    /** Something around the evolution chamber that trait conditions can count, for example "a reactor runs nearby". */
    public static void environment(ResourceLocation id, NaniteTraitRegistry.EnvProbe probe) {
        NaniteTraitRegistry.registerEnvironment(id, probe);
    }
}
