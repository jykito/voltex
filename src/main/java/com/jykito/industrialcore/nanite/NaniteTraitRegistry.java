package com.jykito.industrialcore.nanite;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rare traits and chamber surroundings added by other mods (API, see {@code api.VoltexNanites}).
 * <p>
 * The five built-in traits stay in {@link NaniteTrait}. An add-on trait is identified by its id ({@code modid:name}),
 * keeps its own counters in the strain and appears when its {@link Condition} says so after a generation in the
 * evolution chamber. Its effect lives in the add-on's own machines. Names come from the lang keys
 * {@code nanite_trait.<modid>.<name>} with {@code .how}, {@code .hint} and {@code .effect} for the atlas page.
 * <p>
 * An {@link EnvProbe} reports one thing around the chamber (for example "a reactor runs nearby"); its id lands in
 * {@link NaniteTraits.Env#flags()} for every generation it holds, so conditions can count such generations.
 */
public final class NaniteTraitRegistry {

    private NaniteTraitRegistry() {}

    /** Whether the trait appears after this generation. {@code counters} is the trait's own storage in the strain. */
    @FunctionalInterface
    public interface Condition {
        boolean test(NaniteGenome genome, @Nullable NaniteGenome.Kind kind, String target, NaniteTraits.Env env, CompoundTag counters);
    }

    /** One fact about the chamber's surroundings, checked once per generation on the server. */
    @FunctionalInterface
    public interface EnvProbe {
        boolean test(Level level, BlockPos chamber);
    }

    public record CustomTrait(ResourceLocation id, int color, Condition condition) {
        public String key() { return id.toString(); }
    }

    /** A trait as the atlas, toasts and tooltips see it: built-in or add-on. */
    public record TraitInfo(String key, int color, String langKey) {}

    private static final Map<String, CustomTrait> TRAITS = new LinkedHashMap<>();
    private static final Map<String, EnvProbe> PROBES = new LinkedHashMap<>();

    public static synchronized void registerTrait(ResourceLocation id, int argbColor, Condition condition) {
        TRAITS.put(id.toString(), new CustomTrait(id, argbColor, condition));
    }

    public static synchronized void registerEnvironment(ResourceLocation id, EnvProbe probe) {
        PROBES.put(id.toString(), probe);
    }

    public static List<CustomTrait> customTraits() { return List.copyOf(TRAITS.values()); }

    /** Ids of the probes that hold around the chamber right now. */
    public static Set<String> probe(Level level, BlockPos chamber) {
        if (PROBES.isEmpty()) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        PROBES.forEach((id, p) -> { if (p.test(level, chamber)) out.add(id); });
        return out;
    }

    /** Built-in traits first, then add-on ones, in registration order. */
    public static List<TraitInfo> all() {
        List<TraitInfo> out = new ArrayList<>();
        for (NaniteTrait t : NaniteTrait.values()) out.add(new TraitInfo(t.key(), t.color(), "nanite_trait.industrial_core." + t.key()));
        for (CustomTrait t : TRAITS.values()) out.add(new TraitInfo(t.key(), t.color(), langKey(t.key())));
        return out;
    }

    /** Lang key of a trait name by its key: built-in {@code nanite_trait.industrial_core.<key>}, add-on {@code nanite_trait.<modid>.<name>}. */
    public static String langKey(String key) {
        ResourceLocation id = key.contains(":") ? ResourceLocation.tryParse(key) : null;
        return id == null ? "nanite_trait.industrial_core." + key : "nanite_trait." + id.getNamespace() + "." + id.getPath();
    }
}
