package com.jykito.industrialcore.nanite;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Material by common tags: {@code c:ores/copper}, {@code c:raw_materials/copper}, {@code c:dusts/copper} give "copper".
 * This way copper and deepslate copper ore, raw copper and copper dust from any mod are one material for nanites.
 */
public final class OreMaterial {

    private OreMaterial() {}

    private static final String[] ORE = {"ores/", "raw_materials/"};
    private static final String[] DUST = {"dusts/"};
    private static final String[] ANY = {"ores/", "raw_materials/", "dusts/", "ingots/", "gems/"};

    /** Ore or raw material: what a miner is bred on and what goes into the miner and washing as input. */
    public static @Nullable String ore(Item item) { return find(item, ORE); }

    /** Dust: what a processor is bred on. */
    public static @Nullable String dust(Item item) { return find(item, DUST); }

    /** Material of any form (ore, raw material, dust, ingot, gem). */
    public static @Nullable String any(Item item) { return find(item, ANY); }

    private static @Nullable String find(Item item, String[] prefixes) {
        for (String prefix : prefixes) {
            String found = item.builtInRegistryHolder().tags()
                    .map(TagKey::location)
                    .filter(l -> l.getNamespace().equals("c") && l.getPath().startsWith(prefix) && l.getPath().length() > prefix.length())
                    .map(l -> l.getPath().substring(prefix.length()))
                    .findFirst().orElse(null);
            if (found != null) return found;
        }
        return null;
    }
}
