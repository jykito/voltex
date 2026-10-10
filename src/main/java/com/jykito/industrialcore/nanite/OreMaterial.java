package com.jykito.industrialcore.nanite;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Материал по тегам Forge: {@code forge:ores/copper}, {@code forge:raw_materials/copper}, {@code forge:dusts/copper} дают «copper».
 * Так медная и глубинная медная руда, сырая медь и медная пыль из любого мода — один материал для нанитов.
 */
public final class OreMaterial {

    private OreMaterial() {}

    private static final String[] ORE = {"ores/", "raw_materials/"};
    private static final String[] DUST = {"dusts/"};
    private static final String[] ANY = {"ores/", "raw_materials/", "dusts/", "ingots/", "gems/"};

    /** Руда или сырьё: на чём выводят добытчика и что сырьём идёт в шахтёр и промывку. */
    public static @Nullable String ore(Item item) { return find(item, ORE); }

    /** Пыль: на чём выводят переработчика. */
    public static @Nullable String dust(Item item) { return find(item, DUST); }

    /** Материал любой формы (руда, сырьё, пыль, слиток, самоцвет). */
    public static @Nullable String any(Item item) { return find(item, ANY); }

    private static @Nullable String find(Item item, String[] prefixes) {
        for (String prefix : prefixes) {
            String found = item.builtInRegistryHolder().tags()
                    .map(TagKey::location)
                    .filter(l -> l.getNamespace().equals("forge") && l.getPath().startsWith(prefix) && l.getPath().length() > prefix.length())
                    .map(l -> l.getPath().substring(prefix.length()))
                    .findFirst().orElse(null);
            if (found != null) return found;
        }
        return null;
    }
}
