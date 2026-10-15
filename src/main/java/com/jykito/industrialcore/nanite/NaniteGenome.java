package com.jykito.industrialcore.nanite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
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

    /** Strain class: where it goes and what the selection aims for. */
    public enum Kind {
        MINER, PROCESSOR, SCIENTIST;
        public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
        public static @Nullable Kind byKey(String k) { for (Kind v : values()) if (v.key().equals(k)) return v; return null; }
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

    public static NaniteGenome read(ItemStack stack) {
        NaniteGenome g = new NaniteGenome();
        CompoundTag t = stack.getTagElement(TAG);
        if (t == null) return g;
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
        stack.addTagElement(TAG, t);
    }

    public NaniteGenome copy() {
        NaniteGenome g = new NaniteGenome();
        g.affinity.putAll(affinity);
        g.icon.putAll(icon);
        g.fluid = fluid; g.stability = stability; g.generation = generation; g.kind = kind;
        g.traits.addAll(traits); g.coldGens = coldGens; g.fieldGens = fieldGens; g.hotGens = hotGens; g.stableStreak = stableStreak;
        return g;
    }

    public boolean isEmpty() { return generation == 0 && affinity.isEmpty(); }
    public @Nullable Kind kind() { return kind; }
    public boolean is(Kind k) { return kind == k; }
    public boolean has(NaniteTrait t) { return traits.contains(t); }
    public java.util.Set<NaniteTrait> traits() { return java.util.Collections.unmodifiableSet(traits); }
    void addTrait(NaniteTrait t) { traits.add(t); }

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
    public static float cap(@Nullable Kind k) { return k == Kind.PROCESSOR ? PROCESSOR_CAP : k == Kind.SCIENTIST ? SCIENTIST_CAP : BUDGET; }

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
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item == null || item == net.minecraft.world.item.Items.AIR ? null : item;
    }

    // ───────── edits (only from selection and the miner) ─────────

    void setAffinity(String material, float v) { affinity.put(material, Math.max(0f, v)); }
    void rememberIcon(String material, Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
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
