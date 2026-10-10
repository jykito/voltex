package com.jykito.industrialcore.nanite;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Учёный в нанолаборатории: изучает образец темы и записывает карты данных. У учёного сродство к теме — это
 * изученность 0..100% ({@link NaniteGenome#SCIENTIST_CAP}). Каждая тема — своя ступень (юзер 2026-10-07: уровни данных
 * убраны, «игрок очень просто может скакать между ними»): учёный берётся за тему только с изученностью от её порога
 * ({@link #THRESHOLDS}), выше порога изученность только ускоряет запись ({@link #ticks}).
 * Гейт двойной: поздний образец темы и хорошо выведенный учёный. Карты пойдут в поздние рецепты (решаем при проходе по
 * рецептам). Числа предварительные, баланс позже.
 */
public final class NaniteLab {

    private NaniteLab() {}

    /** Тема → порог изученности. Образцы каждой — в теге {@code industrial_core:nanite_research/<тема>}. */
    public static final Map<String, Float> THRESHOLDS = new LinkedHashMap<>();
    static {
        THRESHOLDS.put("crystallography", 0.30f);
        THRESHOLDS.put("cryophysics", 0.50f);
        THRESHOLDS.put("matter", 0.70f);
        THRESHOLDS.put("stellar", 0.90f);
    }

    public static final int SLOW_TICKS = 1200, FAST_TICKS = 400;

    /** Порог темы; у незнакомой темы — недостижимый. */
    public static float threshold(String theme) { return THRESHOLDS.getOrDefault(theme, 2f); }

    /** Тиков на одну карту: 60 с при нулевой изученности, 20 с при полной. */
    public static int ticks(float knowledge) {
        float k = Math.max(0f, Math.min(1f, knowledge));
        return Math.round(SLOW_TICKS - (SLOW_TICKS - FAST_TICKS) * k);
    }

    public static ResourceLocation cardId(String theme) {
        return new ResourceLocation(IndustrialCore.MODID, "data_card_" + theme);
    }

    public static @Nullable Item card(String theme) {
        Item item = ForgeRegistries.ITEMS.getValue(cardId(theme));
        return item == null || item == Items.AIR ? null : item;
    }
}
