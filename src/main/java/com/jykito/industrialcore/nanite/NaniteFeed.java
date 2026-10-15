package com.jykito.industrialcore.nanite;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What the strain is fed in the evolution chamber and what comes of it: the strain class and the selection target.
 * <ul>
 * <li>ore or raw material ({@code forge:ores/*}, {@code forge:raw_materials/*}) makes a miner, the target is the material, the ore is eaten;</li>
 * <li>dust ({@code forge:dusts/*}) makes a processor, the target is the material, the dust is eaten;</li>
 * <li>a sample from the {@code industrial_core:nanite_research/<topic>} tag makes a scientist, the target is the topic, the sample is not consumed
 *     (it is studied, not eaten; spending exotic matter every generation would be absurd).</li>
 * </ul>
 * Topics and their samples live in data tags, so they can change during recipe passes without code.
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

    /** Research topic of the sample, or null. */
    public static @Nullable String theme(Item item) {
        return item.builtInRegistryHolder().tags()
                .map(TagKey::location)
                .filter(l -> l.getNamespace().equals(IndustrialCore.MODID) && l.getPath().startsWith(RESEARCH_PREFIX) && l.getPath().length() > RESEARCH_PREFIX.length())
                .map(l -> l.getPath().substring(RESEARCH_PREFIX.length()))
                .findFirst().orElse(null);
    }
}
