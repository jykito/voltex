package com.jykito.industrialcore.nanite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nanite strain genome, stored in the item NBT ({@link #TAG}).
 * <ul>
 * <li>class ({@link Kind}): fixed from the first generation by what it is bred on ({@link NaniteFeed});</li>
 * <li>affinity to targets: for miners and processors to ore materials, for scientists to research topics.
 *     A miner in the miner gives an ore bonus (0.84 = +84%), a processor in the ore washer a second output chance
 *     ({@link #secondOutputChance}), a scientist in the nanite lab topic knowledge (topic admission and writing speed);
 *     the sum of affinities is not above the class cap ({@link #cap}: {@link #BUDGET} for miner and scientist, {@link #PROCESSOR_CAP}
 *     for the processor, {@link #SCIENTIST_CAP} for the scientist), so a strain is either a narrow specialist or a bit of everything;</li>
 * <li>drill fluid usage: multiplier {@link #FLUID_MIN}..{@link #FLUID_MAX};</li>
 * <li>stability 0..1: an unstable strain in the miner slowly loses affinity;</li>
 * <li>rare traits ({@link NaniteTrait}) and the counters of the conditions they appear under ({@link NaniteTraits}).</li>
 * </ul>
 * Numbers are preliminary, balance comes later.
 */
public final class NaniteGenome {

    public static final String TAG = "Genome";
    public static final float BUDGET = 3.0f;
    /** Processor cap: affinity is the second output chance, at most 30% (author, 2026-10-07: "+300% is too much"). */
    public static final float PROCESSOR_CAP = 0.30f;
    /** Scientist cap: affinity to a topic is knowledge 0..100%. */
    public static final float SCIENTIST_CAP = 1.0f;
    public static final float FLUID_MIN = 0.5f, FLUID_MAX = 1.5f;
    public static final float STABILITY_MIN = 0.1f;

    /**
     * Strain class: where it goes and what the selection aims for. The three built-in classes are constants; other mods
     * add their own with {@link #builder} (API: {@code api.VoltexNanites}), the way add-ons bring new bee species.
     * <p>
     * A class says what feed makes a strain of it and what target that feed selects for, how far the selection goes
     * ({@link #cap()}), how fast ({@link #step()}, plus {@link #stepHeat()} per unit of chamber heat), whether the feed is
     * eaten, and how its targets are named. Voltex breeds any class in the evolution chamber and shows it in the atlas;
     * an add-on class works only in the add-on's own machines. Lang keys of an add-on class mirror the built-in ones in its
     * own namespace, see {@link #lang}.
     */
    public static final class Kind {
        private static final Map<String, Kind> BY_KEY = new LinkedHashMap<>();

        public static final Kind MINER = new Kind("miner", BUDGET, NaniteEvolution.STEP, NaniteEvolution.STEP_HEAT,
                true, false, true, OreMaterial::ore, null, null, null);
        public static final Kind PROCESSOR = new Kind("processor", PROCESSOR_CAP, NaniteEvolution.PROCESSOR_STEP, NaniteEvolution.PROCESSOR_STEP_HEAT,
                true, false, false, OreMaterial::dust, null, null, null);
        public static final Kind SCIENTIST = new Kind("scientist", SCIENTIST_CAP, NaniteEvolution.SCIENTIST_STEP, NaniteEvolution.SCIENTIST_STEP_HEAT,
                false, true, false, NaniteFeed::theme, t -> net.minecraft.network.chat.Component.translatable("nanite_theme.industrial_core." + t), null, null);
        static { BY_KEY.put(MINER.key, MINER); BY_KEY.put(PROCESSOR.key, PROCESSOR); BY_KEY.put(SCIENTIST.key, SCIENTIST); }

        private final String key;
        private final float cap, step, stepHeat;
        private final boolean consumesFeed, knowledge, usesFluid;
        private final java.util.function.Function<net.minecraft.world.item.Item, String> feed;
        private final @Nullable java.util.function.Function<String, net.minecraft.network.chat.Component> targetName;
        private final @Nullable java.util.function.Supplier<java.util.List<String>> atlasTargets;
        private final @Nullable java.util.function.Function<String, net.minecraft.world.item.Item> atlasSample;

        private Kind(String key, float cap, float step, float stepHeat, boolean consumesFeed, boolean knowledge, boolean usesFluid,
                     java.util.function.Function<net.minecraft.world.item.Item, String> feed,
                     @Nullable java.util.function.Function<String, net.minecraft.network.chat.Component> targetName,
                     @Nullable java.util.function.Supplier<java.util.List<String>> atlasTargets,
                     @Nullable java.util.function.Function<String, net.minecraft.world.item.Item> atlasSample) {
            this.key = key; this.cap = cap; this.step = step; this.stepHeat = stepHeat;
            this.consumesFeed = consumesFeed; this.knowledge = knowledge; this.usesFluid = usesFluid;
            this.feed = feed; this.targetName = targetName; this.atlasTargets = atlasTargets; this.atlasSample = atlasSample;
        }

        /** {@code miner}, {@code processor}, {@code scientist}, or {@code modid:name} for an add-on class. */
        public String key() { return key; }
        public boolean builtIn() { return key.indexOf(':') < 0; }
        /** Cap on the sum of affinities. */
        public float cap() { return cap; }
        public float step() { return step; }
        public float stepHeat() { return stepHeat; }
        public boolean consumesFeed() { return consumesFeed; }
        /** Affinity reads as knowledge (0..100%) rather than a bonus (+%). */
        public boolean knowledge() { return knowledge; }
        /** Selection also counts drill fluid savings (the miner). */
        public boolean usesFluid() { return usesFluid; }
        /** The target this item selects for when fed to a strain of this class, or null. */
        public @Nullable String feedTarget(net.minecraft.world.item.Item item) { return feed.apply(item); }
        /** Name of a target when the class names them itself (topics); null means "the item it was bred on". */
        public @Nullable net.minecraft.network.chat.Component targetName(String target) { return targetName == null ? null : targetName.apply(target); }
        /** Atlas: all targets of an add-on class in this game (built-in classes are listed by the atlas itself). */
        public java.util.List<String> atlasTargets() { return atlasTargets == null ? java.util.List.of() : atlasTargets.get(); }
        /** Atlas: the item a target is shown with for an add-on class, or null. */
        public @Nullable net.minecraft.world.item.Item atlasSample(String target) { return atlasSample == null ? null : atlasSample.apply(target); }

        /**
         * Lang key for this class from a built-in prefix: {@code lang("nanite_species.industrial_core")} is
         * {@code nanite_species.industrial_core.miner} for a built-in class and {@code nanite_species.<modid>.<name>} for an add-on one.
         */
        public String lang(String prefix) {
            if (builtIn()) return prefix + "." + key;
            ResourceLocation id = ResourceLocation.parse(key);
            return prefix.replace(com.jykito.industrialcore.IndustrialCore.MODID, id.getNamespace()) + "." + id.getPath();
        }

        public static @Nullable Kind byKey(String k) { return BY_KEY.get(k); }
        /** Built-in classes first, then add-on ones in registration order. */
        public static java.util.List<Kind> values() { return java.util.List.copyOf(BY_KEY.values()); }
        static java.util.List<Kind> addOns() { return BY_KEY.values().stream().filter(k -> !k.builtIn()).toList(); }

        @Override public String toString() { return key; }

        public static Builder builder(ResourceLocation id) { return new Builder(id); }

        /** Add-on class. Defaults: cap and step of the miner, feed eaten, affinity read as a bonus. */
        public static final class Builder {
            private final ResourceLocation id;
            private float cap = BUDGET, step = NaniteEvolution.STEP, stepHeat = NaniteEvolution.STEP_HEAT;
            private boolean consumesFeed = true, knowledge;
            private java.util.function.Function<net.minecraft.world.item.Item, String> feed = i -> null;
            private @Nullable java.util.function.Function<String, net.minecraft.network.chat.Component> targetName;
            private @Nullable java.util.function.Supplier<java.util.List<String>> atlasTargets;
            private @Nullable java.util.function.Function<String, net.minecraft.world.item.Item> atlasSample;

            private Builder(ResourceLocation id) { this.id = id; }
            public Builder cap(float cap) { this.cap = cap; return this; }
            public Builder step(float step, float perHeat) { this.step = step; this.stepHeat = perHeat; return this; }
            /** The feed is a sample that stays, like a scientist's topic sample. */
            public Builder keepsFeed() { this.consumesFeed = false; return this; }
            /** Affinity reads as knowledge 0..100% instead of a +% bonus. */
            public Builder knowledge() { this.knowledge = true; return this; }
            /** Which items make a strain of this class and which target each selects for (null = not a feed). */
            public Builder feed(java.util.function.Function<net.minecraft.world.item.Item, String> feed) { this.feed = feed; return this; }
            public Builder targetName(java.util.function.Function<String, net.minecraft.network.chat.Component> name) { this.targetName = name; return this; }
            public Builder atlas(java.util.function.Supplier<java.util.List<String>> targets, java.util.function.Function<String, net.minecraft.world.item.Item> sample) {
                this.atlasTargets = targets; this.atlasSample = sample; return this;
            }
            /** Registers the class. Call during mod setup. */
            public Kind register() {
                Kind k = new Kind(id.toString(), cap, step, stepHeat, consumesFeed, knowledge, false, feed, targetName, atlasTargets, atlasSample);
                synchronized (BY_KEY) { BY_KEY.put(k.key, k); }
                return k;
            }
        }
    }

    /** Material -> affinity. Order is the order the materials appeared in the selection. */
    private final Map<String, Float> affinity = new LinkedHashMap<>();
    /** Material -> the item it was bred on: the material is labelled with it in tooltips and windows. */
    private final Map<String, ResourceLocation> icon = new LinkedHashMap<>();
    private float fluid = 1f, stability = 1f;
    private int generation;
    private @Nullable Kind kind;
    private final java.util.EnumSet<NaniteTrait> traits = java.util.EnumSet.noneOf(NaniteTrait.class);
    /** Generations in the cold, in a magnet field, in heat and in a row at full stability, towards rare traits. */
    int coldGens, fieldGens, hotGens, stableStreak;
    /** Add-on traits ({@link NaniteTraitRegistry}) by id, and the counters of each. */
    private final java.util.LinkedHashSet<String> extraTraits = new java.util.LinkedHashSet<>();
    private final CompoundTag extraCounters = new CompoundTag();

    public static NaniteGenome read(ItemStack stack) {
        NaniteGenome g = new NaniteGenome();
        net.minecraft.world.item.component.CustomData data = stack.get(com.jykito.industrialcore.item.ModDataComponents.NANITE_GENOME.get());
        if (data == null) return g;
        CompoundTag t = data.copyTag();
        CompoundTag aff = t.getCompound("Aff"), ic = t.getCompound("Icon");
        for (String k : aff.getAllKeys()) g.affinity.put(k, Math.max(0f, aff.getFloat(k)));
        for (String k : ic.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(ic.getString(k));
            if (id != null) g.icon.put(k, id);
        }
        g.fluid = t.contains("Fluid") ? Mth.clamp(t.getFloat("Fluid"), FLUID_MIN, FLUID_MAX) : 1f;
        g.stability = t.contains("Stab") ? Mth.clamp(t.getFloat("Stab"), STABILITY_MIN, 1f) : 1f;
        g.generation = t.getInt("Gen");
        g.kind = Kind.byKey(t.getString("Kind"));
        for (net.minecraft.nbt.Tag e : t.getList("Traits", net.minecraft.nbt.Tag.TAG_STRING)) { NaniteTrait tr = NaniteTrait.byKey(e.getAsString()); if (tr != null) g.traits.add(tr); }
        g.coldGens = t.getInt("Cold"); g.fieldGens = t.getInt("Field"); g.hotGens = t.getInt("Hot"); g.stableStreak = t.getInt("Stable");
        for (net.minecraft.nbt.Tag e : t.getList("XTraits", net.minecraft.nbt.Tag.TAG_STRING)) g.extraTraits.add(e.getAsString());
        g.extraCounters.merge(t.getCompound("XCount"));
        if (g.kind == null && !g.affinity.isEmpty()) g.kind = Kind.MINER;   // first version strains, before classes
        return g;
    }

    public void write(ItemStack stack) {
        CompoundTag t = new CompoundTag(), aff = new CompoundTag(), ic = new CompoundTag();
        affinity.forEach(aff::putFloat);
        icon.forEach((k, v) -> ic.putString(k, v.toString()));
        t.put("Aff", aff);
        t.put("Icon", ic);
        t.putFloat("Fluid", fluid);
        t.putFloat("Stab", stability);
        t.putInt("Gen", generation);
        if (kind != null) t.putString("Kind", kind.key());
        if (!traits.isEmpty()) { net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag(); for (NaniteTrait tr : traits) l.add(net.minecraft.nbt.StringTag.valueOf(tr.key())); t.put("Traits", l); }
        if (coldGens > 0) t.putInt("Cold", coldGens);
        if (fieldGens > 0) t.putInt("Field", fieldGens);
        if (hotGens > 0) t.putInt("Hot", hotGens);
        if (stableStreak > 0) t.putInt("Stable", stableStreak);
        if (!extraTraits.isEmpty()) { net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag(); for (String k : extraTraits) l.add(net.minecraft.nbt.StringTag.valueOf(k)); t.put("XTraits", l); }
        if (!extraCounters.isEmpty()) t.put("XCount", extraCounters.copy());
        stack.set(com.jykito.industrialcore.item.ModDataComponents.NANITE_GENOME.get(), net.minecraft.world.item.component.CustomData.of(t));
    }

    public NaniteGenome copy() {
        NaniteGenome g = new NaniteGenome();
        g.affinity.putAll(affinity);
        g.icon.putAll(icon);
        g.fluid = fluid; g.stability = stability; g.generation = generation; g.kind = kind;
        g.traits.addAll(traits); g.coldGens = coldGens; g.fieldGens = fieldGens; g.hotGens = hotGens; g.stableStreak = stableStreak;
        g.extraTraits.addAll(extraTraits); g.extraCounters.merge(extraCounters.copy());
        return g;
    }

    public boolean isEmpty() { return generation == 0 && affinity.isEmpty(); }
    public @Nullable Kind kind() { return kind; }
    public boolean is(Kind k) { return kind == k; }
    public boolean has(NaniteTrait t) { return traits.contains(t); }
    public java.util.Set<NaniteTrait> traits() { return java.util.Collections.unmodifiableSet(traits); }
    void addTrait(NaniteTrait t) { traits.add(t); }

    /** Any trait by key: a built-in one ({@link NaniteTrait#key()}) or an add-on one ({@code modid:name}). */
    public boolean hasTrait(String key) {
        NaniteTrait t = NaniteTrait.byKey(key);
        return t != null ? traits.contains(t) : extraTraits.contains(key);
    }

    /** Keys of all traits of the strain, built-in first. */
    public java.util.List<String> allTraitKeys() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (NaniteTrait t : traits) out.add(t.key());
        out.addAll(extraTraits);
        return out;
    }

    /** Counter storage of an add-on trait in this strain (created on first use, saved with the strain). */
    public CompoundTag counters(String traitKey) {
        if (!extraCounters.contains(traitKey, net.minecraft.nbt.Tag.TAG_COMPOUND)) extraCounters.put(traitKey, new CompoundTag());
        return extraCounters.getCompound(traitKey);
    }

    void addExtraTrait(String key) { extraTraits.add(key); }

    /**
     * Affinity to the target at work (miner, washer, lab): with chimera not below half of the best affinity,
     * with pyronite x1.2. Selection in the chamber looks at the plain {@link #bonus}.
     */
    public float effect(@Nullable String target) {
        float b = bonus(target);
        if (has(NaniteTrait.CHIMERA)) {
            float best = 0; for (float v : affinity.values()) best = Math.max(best, v);
            b = Math.max(b, best * NaniteTrait.CHIMERA_SHARE);
        }
        return has(NaniteTrait.PYRONITE) ? b * NaniteTrait.PYRONITE_POWER : b;
    }

    /** Whether the strain wears out at work: a self-assembler does not. */
    public boolean wears() { return !has(NaniteTrait.SELF_ASSEMBLER) && !affinity.isEmpty(); }

    /** Strain class in the item without parsing the whole genome: for slot checks. */
    public static @Nullable Kind kindOf(ItemStack stack) { return read(stack).kind; }

    /** Cap on the sum of affinities for a class. */
    public static float cap(@Nullable Kind k) { return k == null ? BUDGET : k.cap(); }

    /** Processor second output chance: affinity at work, up to {@link #PROCESSOR_CAP} (pyronite has a x1.2 cap). */
    public float secondOutputChance(@Nullable String material) {
        return Math.min(PROCESSOR_CAP * (has(NaniteTrait.PYRONITE) ? NaniteTrait.PYRONITE_POWER : 1f), effect(material));
    }
    public int generation() { return generation; }
    public float fluid() { return fluid; }
    public float stability() { return stability; }
    public Map<String, Float> affinity() { return affinity; }

    public float bonus(@Nullable String material) {
        return material == null ? 0f : affinity.getOrDefault(material, 0f);
    }

    public float total() {
        float s = 0;
        for (float v : affinity.values()) s += v;
        return s;
    }

    /** The material with the highest affinity (the item is tinted with it), or null for an empty strain. */
    public @Nullable String dominant() {
        String best = null;
        float max = 0;
        for (Map.Entry<String, Float> e : affinity.entrySet()) if (e.getValue() > max) { max = e.getValue(); best = e.getKey(); }
        return best;
    }

    /** The item the material is labelled with; null if unknown. */
    public @Nullable Item icon(String material) {
        ResourceLocation id = icon.get(material);
        Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
        return item == null || item == net.minecraft.world.item.Items.AIR ? null : item;
    }

    // ───────── edits (only from selection and the miner) ─────────

    void setAffinity(String material, float v) { affinity.put(material, Math.max(0f, v)); }
    void rememberIcon(String material, Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id != null) icon.putIfAbsent(material, id);
    }
    void setFluid(float v) { fluid = Mth.clamp(v, FLUID_MIN, FLUID_MAX); }
    void setStability(float v) { stability = Mth.clamp(v, STABILITY_MIN, 1f); }
    void nextGeneration() { generation++; }
    void setKind(Kind k) { kind = k; }

    /** The sum of affinities stays within the budget: other materials shrink first, then, if needed, the target too. */
    void fitBudget(String keep, float budget) {
        float own = bonus(keep), others = total() - own;
        if (own + others <= budget) return;
        float room = Math.max(0f, budget - own);
        float k = others > 0 ? room / others : 0f;
        affinity.replaceAll((m, v) -> m.equals(keep) ? Math.min(v, budget) : v * k);
        affinity.values().removeIf(v -> v < 0.005f);
        if (!affinity.containsKey(keep) && own > 0) affinity.put(keep, Math.min(own, budget));
    }

    /** Wear in the miner: all affinities lose a share. */
    public void decay(float factor) {
        affinity.replaceAll((m, v) -> v * factor);
        affinity.values().removeIf(v -> v < 0.005f);
    }
}
