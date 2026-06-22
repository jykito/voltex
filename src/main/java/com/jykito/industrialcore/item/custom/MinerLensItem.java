package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.Config;
import com.jykito.industrialcore.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import net.minecraftforge.registries.tags.ITagManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MinerLensItem extends Item {

    public enum LensType { OVERWORLD, NETHER, END, ALL }

    private final LensType lensType;

    public MinerLensItem(LensType lensType, Properties properties) {
        super(properties);
        this.lensType = lensType;
    }

    public LensType getLensType() { return lensType; }

    private static final TagKey<Item> TAG_ORES =
            ItemTags.create(new ResourceLocation("forge", "ores"));
    private static final TagKey<Item> TAG_ORES_NETHERRACK =
            ItemTags.create(new ResourceLocation("forge", "ores_in_ground/netherrack"));

    private static final int MODDED_ORE_WEIGHT = 4;

    private record OreEntry(Item item, int weight) {}
    private record Candidate(Item item, int weight, String material) {}

    private static final Map<LensType, List<OreEntry>> CACHE = new EnumMap<>(LensType.class);
    private static Map<Item, String> MATERIAL_CACHE = null;

    public static void clearCache() {
        synchronized (CACHE) { CACHE.clear(); MATERIAL_CACHE = null; }
    }

    private static List<OreEntry> entries(LensType type) {
        synchronized (CACHE) {
            List<OreEntry> cached = CACHE.get(type);
            if (cached != null) return cached;
            List<OreEntry> built = build(type);
            CACHE.put(type, built);
            return built;
        }
    }

    private static Map<Item, String> materialMap() {
        if (MATERIAL_CACHE != null) return MATERIAL_CACHE;
        Map<Item, String> map = new HashMap<>();
        ITagManager<Item> tm = ForgeRegistries.ITEMS.tags();
        if (tm != null) {
            tm.getTagNames().forEach(tag -> {
                ResourceLocation id = tag.location();
                if (!id.getNamespace().equals("forge")) return;
                String p = id.getPath();
                if (!p.startsWith("ores/")) return;
                String material = p.substring(5);
                if (material.isEmpty() || material.contains("/")) return;
                for (Item it : tm.getTag(tag)) map.putIfAbsent(it, material);
            });
        }
        MATERIAL_CACHE = map;
        return map;
    }

    private static List<Candidate> curated(LensType type) {
        List<Candidate> l = new ArrayList<>();
        switch (type) {
            case OVERWORLD -> {
                l.add(new Candidate(Items.IRON_ORE, 11, "iron"));        l.add(new Candidate(Items.DEEPSLATE_IRON_ORE, 9, "iron"));
                l.add(new Candidate(Items.GOLD_ORE, 4, "gold"));         l.add(new Candidate(Items.DEEPSLATE_GOLD_ORE, 3, "gold"));
                l.add(new Candidate(Items.COPPER_ORE, 9, "copper"));     l.add(new Candidate(Items.DEEPSLATE_COPPER_ORE, 7, "copper"));
                l.add(new Candidate(Items.COAL_ORE, 12, "coal"));        l.add(new Candidate(Items.DEEPSLATE_COAL_ORE, 10, "coal"));
                l.add(new Candidate(Items.DIAMOND_ORE, 2, "diamond"));   l.add(new Candidate(Items.DEEPSLATE_DIAMOND_ORE, 1, "diamond"));
                l.add(new Candidate(Items.EMERALD_ORE, 2, "emerald"));   l.add(new Candidate(Items.DEEPSLATE_EMERALD_ORE, 1, "emerald"));
                l.add(new Candidate(Items.LAPIS_ORE, 4, "lapis"));       l.add(new Candidate(Items.DEEPSLATE_LAPIS_ORE, 3, "lapis"));
                l.add(new Candidate(Items.REDSTONE_ORE, 5, "redstone")); l.add(new Candidate(Items.DEEPSLATE_REDSTONE_ORE, 4, "redstone"));
                l.add(new Candidate(ModItems.TIN_ORE_ITEM.get(), 9, "tin"));         l.add(new Candidate(ModItems.DEEPSLATE_TIN_ORE_ITEM.get(), 7, "tin"));
                l.add(new Candidate(ModItems.LEAD_ORE_ITEM.get(), 4, "lead"));       l.add(new Candidate(ModItems.DEEPSLATE_LEAD_ORE_ITEM.get(), 3, "lead"));
                l.add(new Candidate(ModItems.BAUXITE_ORE_ITEM.get(), 5, "aluminum"));l.add(new Candidate(ModItems.DEEPSLATE_BAUXITE_ORE_ITEM.get(), 2, "aluminum"));
                l.add(new Candidate(ModItems.URANIUM_ORE_ITEM.get(), 2, "uranium"));l.add(new Candidate(ModItems.DEEPSLATE_URANIUM_ORE_ITEM.get(), 1, "uranium"));
                l.add(new Candidate(Items.AMETHYST_BLOCK, 6, "amethyst"));
            }
            case NETHER -> {
                l.add(new Candidate(Items.NETHER_QUARTZ_ORE, 7, "quartz"));
                l.add(new Candidate(Items.NETHER_GOLD_ORE, 4, "nether_gold"));
                l.add(new Candidate(Items.ANCIENT_DEBRIS, 1, "ancient_debris"));
                l.add(new Candidate(Items.GLOWSTONE, 6, "glowstone"));
            }
            default -> { }
        }
        return l;
    }

    private static List<OreEntry> build(LensType type) {
        if (type == LensType.ALL) {
            List<OreEntry> all = new ArrayList<>();
            all.addAll(entries(LensType.OVERWORLD));
            all.addAll(entries(LensType.NETHER));
            all.addAll(entries(LensType.END));
            return all;
        }

        List<Candidate> candidates = new ArrayList<>(curated(type));
        Set<Item> seen = new HashSet<>();
        for (Candidate c : candidates) seen.add(c.item());

        ITagManager<Item> tm = ForgeRegistries.ITEMS.tags();
        if (tm != null) {
            ITag<Item> ores = tm.getTag(TAG_ORES);
            ITag<Item> netherOres = tm.getTag(TAG_ORES_NETHERRACK);
            Map<Item, String> matMap = materialMap();
            for (Item it : ores) {
                if (it == null || it == Items.AIR || seen.contains(it)) continue;
                if (classify(it, netherOres) != type) continue;
                String m = matMap.getOrDefault(it, "uniq:" + idOf(it));
                candidates.add(new Candidate(it, MODDED_ORE_WEIGHT, m));
                seen.add(it);
            }
        }

        List<Candidate> kept;
        if (Config.minerDedupOres) {
            Map<String, List<Candidate>> groups = new LinkedHashMap<>();
            for (Candidate c : candidates) groups.computeIfAbsent(c.material(), k -> new ArrayList<>()).add(c);
            kept = new ArrayList<>();
            for (List<Candidate> grp : groups.values()) {
                String winNs = bestNamespace(grp);
                for (Candidate c : grp) if (namespaceOf(c.item()).equals(winNs)) kept.add(c);
            }
        } else {
            kept = candidates;
        }

        List<OreEntry> out = new ArrayList<>(kept.size());
        for (Candidate c : kept) out.add(new OreEntry(c.item(), c.weight()));
        return out;
    }

    private static String bestNamespace(List<Candidate> group) {
        String best = null;
        int bestRank = Integer.MAX_VALUE;
        for (Candidate c : group) {
            String ns = namespaceOf(c.item());
            int rank = priorityRank(ns);
            if (best == null || rank < bestRank || (rank == bestRank && ns.compareTo(best) < 0)) {
                best = ns;
                bestRank = rank;
            }
        }
        return best == null ? "minecraft" : best;
    }

    private static int priorityRank(String ns) {
        int i = Config.minerOrePriority.indexOf(ns);
        return i >= 0 ? i : Integer.MAX_VALUE;
    }

    private static String namespaceOf(Item it) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(it);
        return id != null ? id.getNamespace() : "minecraft";
    }

    private static String idOf(Item it) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(it);
        return id != null ? id.toString() : "minecraft:air";
    }

    private static LensType classify(Item it, @Nullable ITag<Item> netherOres) {
        if (netherOres != null && netherOres.contains(it)) return LensType.NETHER;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(it);
        String path = id != null ? id.getPath() : "";
        if (path.contains("nether")) return LensType.NETHER;
        if (path.contains("endstone") || path.contains("end_stone")
                || path.startsWith("end_") || path.contains("_end_") || path.endsWith("_end_ore")) {
            return LensType.END;
        }
        return LensType.OVERWORLD;
    }

    public static List<Item> getOresForType(LensType type) {
        List<OreEntry> e = entries(type);
        List<Item> out = new ArrayList<>(e.size());
        for (OreEntry oe : e) out.add(oe.item());
        return out;
    }

    public static List<Integer> getWeightsForType(LensType type) {
        List<OreEntry> e = entries(type);
        List<Integer> out = new ArrayList<>(e.size());
        for (OreEntry oe : e) out.add(oe.weight());
        return out;
    }

    public static List<Item> buildWeightedPool(LensType type) {
        List<OreEntry> e = entries(type);
        List<Item> pool = new ArrayList<>();
        for (OreEntry oe : e) {
            for (int j = 0; j < oe.weight(); j++) pool.add(oe.item());
        }
        return pool;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String dimKey = switch (lensType) {
            case OVERWORLD -> "tooltip.industrial_core.lens_overworld";
            case NETHER    -> "tooltip.industrial_core.lens_nether";
            case END       -> "tooltip.industrial_core.lens_end";
            case ALL       -> "tooltip.industrial_core.lens_all";
        };
        tooltip.add(Component.translatable(dimKey).withStyle(ChatFormatting.AQUA));
    }
}
