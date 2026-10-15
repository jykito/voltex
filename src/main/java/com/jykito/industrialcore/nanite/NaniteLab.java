package com.jykito.industrialcore.nanite;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scientist in the nanite lab: studies a topic sample and writes data cards. A scientist's affinity to a topic is
 * knowledge 0..100% ({@link NaniteGenome#SCIENTIST_CAP}). Each topic is its own step (author, 2026-10-07: data levels
 * removed, "the player can hop between them too easily"): the scientist takes on a topic only with knowledge from its threshold
 * ({@link #THRESHOLDS}), above the threshold knowledge only speeds up writing ({@link #ticks}).
 * A double gate: a late topic sample and a well-bred scientist. The cards will go into late recipes (decided during the recipe
 * pass). Numbers are preliminary, balance comes later.
 */
public final class NaniteLab {

    private NaniteLab() {}

    /** Topic -> knowledge threshold. Samples of each are in the tag {@code industrial_core:nanite_research/<topic>}. */
    public static final Map<String, Float> THRESHOLDS = new LinkedHashMap<>();
    static {
        THRESHOLDS.put("crystallography", 0.30f);
        THRESHOLDS.put("cryophysics", 0.50f);
        THRESHOLDS.put("matter", 0.70f);
        THRESHOLDS.put("stellar", 0.90f);
    }

    public static final int SLOW_TICKS = 1200, FAST_TICKS = 400;

    /** Topic threshold; unreachable for an unknown topic. */
    public static float threshold(String theme) { return THRESHOLDS.getOrDefault(theme, 2f); }

    /** Ticks per card: 60 s at zero knowledge, 20 s at full. */
    public static int ticks(float knowledge) {
        float k = Math.max(0f, Math.min(1f, knowledge));
        return Math.round(SLOW_TICKS - (SLOW_TICKS - FAST_TICKS) * k);
    }

    public static ResourceLocation cardId(String theme) {
        return ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "data_card_" + theme);
    }

    public static @Nullable Item card(String theme) {
        Item item = BuiltInRegistries.ITEM.get(cardId(theme));
        return item == null || item == Items.AIR ? null : item;
    }
}
