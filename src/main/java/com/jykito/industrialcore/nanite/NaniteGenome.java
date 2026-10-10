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
 * Геном штамма нанитов, хранится в NBT предмета ({@link #TAG}).
 * <ul>
 * <li>класс ({@link Kind}): закрепляется с первого поколения по тому, на чём выводят ({@link NaniteFeed});</li>
 * <li>сродство к целям: у добытчика и переработчика — к материалам руды, у учёного — к темам исследований.
 *     Добытчик в шахтёре даёт прибавку руды (0.84 = +84%), переработчик в промывщике — шанс второго выхода
 *     ({@link #secondOutputChance}), учёный в нанолаборатории — изученность темы (допуск к теме и скорость записи);
 *     сумма сродств не больше потолка класса ({@link #cap}: {@link #BUDGET} у добытчика и учёного, {@link #PROCESSOR_CAP}
 *     у переработчика, {@link #SCIENTIST_CAP} у учёного), поэтому штамм либо узкий специалист, либо понемногу ко всему;</li>
 * <li>расход буровой жижи: множитель {@link #FLUID_MIN}..{@link #FLUID_MAX};</li>
 * <li>стабильность 0..1: нестабильный штамм в шахтёре понемногу теряет сродство;</li>
 * <li>редкие черты ({@link NaniteTrait}) и счётчики условий, по которым они появляются ({@link NaniteTraits}).</li>
 * </ul>
 * Числа предварительные, баланс позже.
 */
public final class NaniteGenome {

    public static final String TAG = "Genome";
    public static final float BUDGET = 3.0f;
    /** Потолок переработчика: сродство и есть шанс второго выхода, не больше 30% (юзер 2026-10-07: «+300% слишком много»). */
    public static final float PROCESSOR_CAP = 0.30f;
    /** Потолок учёного: сродство к теме — это изученность 0..100%. */
    public static final float SCIENTIST_CAP = 1.0f;
    public static final float FLUID_MIN = 0.5f, FLUID_MAX = 1.5f;
    public static final float STABILITY_MIN = 0.1f;

    /** Класс штамма: куда он ставится и под что идёт отбор. */
    public enum Kind {
        MINER, PROCESSOR, SCIENTIST;
        public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
        public static @Nullable Kind byKey(String k) { for (Kind v : values()) if (v.key().equals(k)) return v; return null; }
    }

    /** Материал → сродство. Порядок — как материалы появлялись в отборе. */
    private final Map<String, Float> affinity = new LinkedHashMap<>();
    /** Материал → предмет, на котором его выводили: им материал подписан в подсказках и окнах. */
    private final Map<String, ResourceLocation> icon = new LinkedHashMap<>();
    private float fluid = 1f, stability = 1f;
    private int generation;
    private @Nullable Kind kind;
    private final java.util.EnumSet<NaniteTrait> traits = java.util.EnumSet.noneOf(NaniteTrait.class);
    /** Поколений в холоде, в поле магнита, на тепле и подряд на полной стабильности — к редким чертам. */
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
        if (g.kind == null && !g.affinity.isEmpty()) g.kind = Kind.MINER;   // штаммы первой версии, до классов
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
     * Сродство к цели в работе (шахтёр, промыватель, лаборатория): с химерой не ниже половины лучшего сродства,
     * с пиронитом ×1.2. Отбор в камере смотрит на чистое {@link #bonus}.
     */
    public float effect(@Nullable String target) {
        float b = bonus(target);
        if (has(NaniteTrait.CHIMERA)) {
            float best = 0; for (float v : affinity.values()) best = Math.max(best, v);
            b = Math.max(b, best * NaniteTrait.CHIMERA_SHARE);
        }
        return has(NaniteTrait.PYRONITE) ? b * NaniteTrait.PYRONITE_POWER : b;
    }

    /** Изнашивается ли штамм в работе: самосборщик — нет. */
    public boolean wears() { return !has(NaniteTrait.SELF_ASSEMBLER) && !affinity.isEmpty(); }

    /** Класс штамма в предмете без разбора всего генома: для проверок слотов. */
    public static @Nullable Kind kindOf(ItemStack stack) { return read(stack).kind; }

    /** Потолок суммы сродств для класса. */
    public static float cap(@Nullable Kind k) { return k == Kind.PROCESSOR ? PROCESSOR_CAP : k == Kind.SCIENTIST ? SCIENTIST_CAP : BUDGET; }

    /** Шанс второго выхода у переработчика: сродство в работе, до {@link #PROCESSOR_CAP} (у пиронита потолок ×1.2). */
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

    /** Материал с наибольшим сродством (им красится предмет) или null у пустого штамма. */
    public @Nullable String dominant() {
        String best = null;
        float max = 0;
        for (Map.Entry<String, Float> e : affinity.entrySet()) if (e.getValue() > max) { max = e.getValue(); best = e.getKey(); }
        return best;
    }

    /** Предмет, которым подписан материал; если неизвестен — null. */
    public @Nullable Item icon(String material) {
        ResourceLocation id = icon.get(material);
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item == null || item == net.minecraft.world.item.Items.AIR ? null : item;
    }

    // ───────── правка (только из отбора и шахтёра) ─────────

    void setAffinity(String material, float v) { affinity.put(material, Math.max(0f, v)); }
    void rememberIcon(String material, Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id != null) icon.putIfAbsent(material, id);
    }
    void setFluid(float v) { fluid = Mth.clamp(v, FLUID_MIN, FLUID_MAX); }
    void setStability(float v) { stability = Mth.clamp(v, STABILITY_MIN, 1f); }
    void nextGeneration() { generation++; }
    void setKind(Kind k) { kind = k; }

    /** Сумма сродств не выше бюджета: сначала сжимаются чужие материалы, потом, если нужно, и целевой. */
    void fitBudget(String keep, float budget) {
        float own = bonus(keep), others = total() - own;
        if (own + others <= budget) return;
        float room = Math.max(0f, budget - own);
        float k = others > 0 ? room / others : 0f;
        affinity.replaceAll((m, v) -> m.equals(keep) ? Math.min(v, budget) : v * k);
        affinity.values().removeIf(v -> v < 0.005f);
        if (!affinity.containsKey(keep) && own > 0) affinity.put(keep, Math.min(own, budget));
    }

    /** Износ в шахтёре: все сродства теряют долю. */
    public void decay(float factor) {
        affinity.replaceAll((m, v) -> v * factor);
        affinity.values().removeIf(v -> v < 0.005f);
    }
}
