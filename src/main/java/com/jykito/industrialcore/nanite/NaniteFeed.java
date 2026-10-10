package com.jykito.industrialcore.nanite;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Чем кормят штамм в камере эволюции и что из этого выходит: класс штамма и цель отбора.
 * <ul>
 * <li>руда или сырьё ({@code forge:ores/*}, {@code forge:raw_materials/*}) — добытчик, цель — материал, руда съедается;</li>
 * <li>пыль ({@code forge:dusts/*}) — переработчик, цель — материал, пыль съедается;</li>
 * <li>образец из тега {@code industrial_core:nanite_research/<тема>} — учёный, цель — тема, образец не расходуется
 *     (его изучают, а не едят; экзоматерия на каждое поколение была бы абсурдом).</li>
 * </ul>
 * Темы и их образцы живут в тегах данных, чтобы при проходе по рецептам менять их без кода.
 */
public record NaniteFeed(NaniteGenome.Kind kind, String target, boolean consumed) {

    public static final String RESEARCH_PREFIX = "nanite_research/";

    public static @Nullable NaniteFeed of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        String theme = theme(item);
        if (theme != null) return new NaniteFeed(NaniteGenome.Kind.SCIENTIST, theme, false);
        String ore = OreMaterial.ore(item);
        if (ore != null) return new NaniteFeed(NaniteGenome.Kind.MINER, ore, true);
        String dust = OreMaterial.dust(item);
        if (dust != null) return new NaniteFeed(NaniteGenome.Kind.PROCESSOR, dust, true);
        return null;
    }

    /** Тема исследования образца или null. */
    public static @Nullable String theme(Item item) {
        return item.builtInRegistryHolder().tags()
                .map(TagKey::location)
                .filter(l -> l.getNamespace().equals(IndustrialCore.MODID) && l.getPath().startsWith(RESEARCH_PREFIX) && l.getPath().length() > RESEARCH_PREFIX.length())
                .map(l -> l.getPath().substring(RESEARCH_PREFIX.length()))
                .findFirst().orElse(null);
    }
}
