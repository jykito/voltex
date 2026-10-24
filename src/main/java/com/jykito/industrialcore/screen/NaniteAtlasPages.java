package com.jykito.industrialcore.screen;

import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.nanite.NaniteAtlas;
import com.jykito.industrialcore.nanite.NaniteAtlasClient;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.industrialcore.nanite.NaniteLab;
import com.jykito.industrialcore.nanite.NaniteTrait;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * The nanite species atlas as one more chapter of the panel (author, 2026-10-07): an overview, then species by class and rare species.
 * The species list is built from what exists in the game: miner materials from the {@code forge:ores/*} tags, processor ones from
 * materials that have both ore and dust, scientist ones from the lab topics. Unlocks and records come from {@link NaniteAtlasClient}.
 * A species not yet unlocked shows "???" with a hint on how to get it; rare ones get a vague hint.
 */
final class NaniteAtlasPages {

    private NaniteAtlasPages() {}

    private static final String[] MEDAL_COLOR = {"#8b949e", "#d08a4f", "#c3ccd1", "#ffcf33"};

    static GuideContent.Category build() {
        List<GuideContent.Entry> entries = new ArrayList<>();
        int open = 0, total = 0, golds = 0;
        List<GuideContent.Entry> species = new ArrayList<>();
        for (NaniteGenome.Kind kind : NaniteGenome.Kind.values())
            for (String target : targets(kind)) {
                String key = NaniteAtlas.speciesKey(kind, target);
                int best = NaniteAtlasClient.best(key);
                total++;
                if (best >= 0) { open++; if (NaniteAtlas.medal(kind, best) == 3) golds++; }
                species.add(speciesPage(kind, target, key, best));
            }
        List<GuideContent.Entry> rare = new ArrayList<>();
        for (com.jykito.industrialcore.nanite.NaniteTraitRegistry.TraitInfo t : com.jykito.industrialcore.nanite.NaniteTraitRegistry.all()) {
            boolean got = NaniteAtlasClient.trait(t.key());
            total++; if (got) open++;
            rare.add(traitPage(t, got));
        }
        entries.add(overview(open, total, golds));
        entries.addAll(species);
        entries.addAll(rare);
        return new GuideContent.Category(tr("gui.industrial_core.atlas.title"), entries, new ItemStack(ModItems.NANITE_STRAIN.get()));
    }

    // ── what exists ──

    /** Class targets in this game: materials by tags or lab topics. */
    private static List<String> targets(NaniteGenome.Kind kind) {
        if (!kind.builtIn()) return new ArrayList<>(kind.atlasTargets());
        if (kind == NaniteGenome.Kind.SCIENTIST) return new ArrayList<>(NaniteLab.THRESHOLDS.keySet());
        TreeSet<String> ores = materials("ores/"), out = new TreeSet<>();
        if (kind == NaniteGenome.Kind.MINER) return new ArrayList<>(ores);
        for (String m : materials("dusts/")) if (ores.contains(m)) out.add(m);
        return new ArrayList<>(out);
    }

    private static TreeSet<String> materials(String prefix) {
        TreeSet<String> out = new TreeSet<>();
        BuiltInRegistries.ITEM.getTags().forEach(p -> {
            ResourceLocation l = p.getFirst().location();
            if (l.getNamespace().equals("forge") && l.getPath().startsWith(prefix) && l.getPath().length() > prefix.length()
                    && p.getSecond().size() > 0) out.add(l.getPath().substring(prefix.length()));
        });
        return out;
    }

    /** The item the material is labelled with: the one it was unlocked on, otherwise the first from the ore/dust tag. */
    private static @Nullable Item sample(NaniteGenome.Kind kind, String target, String key) {
        String id = NaniteAtlasClient.icon(key);
        if (!id.isEmpty()) { Item it = BuiltInRegistries.ITEM.get(new ResourceLocation(id)); if (it != null) return it; }
        if (!kind.builtIn()) return kind.atlasSample(target);
        String prefix = kind == NaniteGenome.Kind.PROCESSOR ? "dusts/" : "ores/";
        TagKey<Item> tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, new ResourceLocation("forge", prefix + target));
        return BuiltInRegistries.ITEM.getTag(tag).map(HolderSet.Named::stream).flatMap(s -> s.findFirst()).map(h -> h.value()).orElse(null);
    }

    // ── pages ──

    private static GuideContent.Entry overview(int open, int total, int golds) {
        List<GuideContent.Element> els = new ArrayList<>();
        els.add(new GuideContent.Card(new ItemStack(ModItems.NANITE_STRAIN.get()), tr("gui.industrial_core.atlas.title"),
                tr("gui.industrial_core.atlas.progress", open, total), List.<String[]>of()));
        els.add(new GuideContent.Text(tr("gui.industrial_core.atlas.intro")));
        els.add(new GuideContent.Stats(List.of(
                new String[]{tr("gui.industrial_core.atlas.medal.1"), tr("gui.industrial_core.atlas.medal_from", pct(NaniteAtlas.BRONZE))},
                new String[]{tr("gui.industrial_core.atlas.medal.2"), tr("gui.industrial_core.atlas.medal_from", pct(NaniteAtlas.SILVER))},
                new String[]{tr("gui.industrial_core.atlas.medal.3"), tr("gui.industrial_core.atlas.medal_from", pct(NaniteAtlas.GOLD))},
                new String[]{tr("gui.industrial_core.atlas.golds"), String.valueOf(golds)})));
        els.add(new GuideContent.Tip(tr("gui.industrial_core.atlas.rare_tip")));
        return new GuideContent.Entry("atlas", tr("gui.industrial_core.atlas.overview"), els, new ItemStack(ModItems.NANITE_STRAIN.get()));
    }

    private static GuideContent.Entry speciesPage(NaniteGenome.Kind kind, String target, String key, int best) {
        boolean got = best >= 0;
        Item sample = sample(kind, target, key);
        String name = got ? NaniteAtlas.speciesName(kind, target, sample).getString() : "???";
        String kindName = tr(kind.lang("tooltip.industrial_core.nanite_strain.kind"));
        List<GuideContent.Element> els = new ArrayList<>();
        int medal = got ? NaniteAtlas.medal(kind, best) : 0;
        List<String[]> tags = new ArrayList<>();
        tags.add(new String[]{got ? tr("gui.industrial_core.atlas.medal." + medal) : tr("gui.industrial_core.atlas.closed"), MEDAL_COLOR[medal]});
        els.add(new GuideContent.Card(got ? NaniteAtlas.icon(kind, target) : NaniteAtlas.painted(NaniteAtlas.UNKNOWN_COLOR), name, kindName, tags));
        net.minecraft.network.chat.Component named = kind.targetName(target);
        String what = named != null ? named.getString() : sample != null ? sample.getDescription().getString() : target;
        els.add(new GuideContent.Text(tr(kind.lang("gui.industrial_core.atlas.how"), what)));
        float cap = NaniteGenome.cap(kind);
        List<String[]> rows = new ArrayList<>();
        if (got) rows.add(new String[]{tr("gui.industrial_core.atlas.record"), value(kind, best / 1000f)});
        for (int m = 1; m <= 3; m++) {
            float share = m == 1 ? NaniteAtlas.BRONZE : m == 2 ? NaniteAtlas.SILVER : NaniteAtlas.GOLD;
            rows.add(new String[]{tr("gui.industrial_core.atlas.medal." + m), tr("gui.industrial_core.atlas.medal_from", value(kind, cap * share))});
        }
        if (kind == NaniteGenome.Kind.SCIENTIST)
            rows.add(new String[]{tr("gui.industrial_core.atlas.threshold"), pct(NaniteLab.threshold(target))});
        els.add(new GuideContent.Stats(rows));
        String id = kind.key().replace(':', '_') + "_" + target;
        return new GuideContent.Entry("atlas_" + id, name, els, got ? NaniteAtlas.icon(kind, target) : NaniteAtlas.painted(NaniteAtlas.UNKNOWN_COLOR));
    }

    private static GuideContent.Entry traitPage(com.jykito.industrialcore.nanite.NaniteTraitRegistry.TraitInfo t, boolean got) {
        String name = got ? tr(t.langKey()) : "???";
        List<GuideContent.Element> els = new ArrayList<>();
        List<String[]> tags = new ArrayList<>();
        tags.add(new String[]{tr(got ? "gui.industrial_core.atlas.rare" : "gui.industrial_core.atlas.closed"), got ? "#c98bff" : MEDAL_COLOR[0]});
        ItemStack icon = NaniteAtlas.painted(got ? t.color() : NaniteAtlas.UNKNOWN_COLOR);
        els.add(new GuideContent.Card(icon, name, tr("gui.industrial_core.atlas.rare_kind"), tags));
        els.add(new GuideContent.Text(tr(t.langKey() + (got ? ".how" : ".hint"))));
        if (got) els.add(new GuideContent.Tip(tr(t.langKey() + ".effect")));
        return new GuideContent.Entry("atlas_trait_" + t.key().replace(':', '_'), name, els, icon);
    }

    private static String value(NaniteGenome.Kind kind, float v) {
        return kind.knowledge() ? pct(v) : "+" + pct(v);
    }

    private static String pct(float v) { return String.format(Locale.ROOT, "%d%%", Math.round(v * 100)); }

    private static String tr(String key, Object... args) { return Component.translatable(key, args).getString(); }
}
