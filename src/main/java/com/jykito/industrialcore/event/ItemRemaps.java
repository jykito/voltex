package com.jykito.industrialcore.event;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.item.Item;

/**
 * Forwards old registry IDs to new ones on rename. A lookup of an alias resolves to its target, so a renamed item
 * in an existing save turns into the new one in place instead of disappearing.
 *
 * ⚠ On EVERY item rename (registry ID change) add a line here. Keep the entries FOREVER: old saves keep turning up
 *   for a long time, and an alias is cheap.
 * ⚠ Aliases must be added before the registries fire, so {@link #register()} runs from the mod constructor.
 */
public final class ItemRemaps {
    private ItemRemaps() {}

    public static void register() {
        item("energy_crystal",   ModItems.FLUX_CORE);   // 2026-07: crystal -> core rebrand
        item("lapotron_crystal", ModItems.PLASMA_CORE);

        // 2026-08: connectors removed, upgrades now go into the hub
        item("connector_upgrade_t1", ModItems.HUB_UPGRADE_T1);
        item("connector_upgrade_t2", ModItems.HUB_UPGRADE_T2);
        item("connector_upgrade_t3", ModItems.HUB_UPGRADE_T3);

        // 2026-09: the nuclear branch was cut (2.1.0). Uranium ore already sits in generated worlds,
        // so it turns into stone, otherwise the veins would become voids underground. The ore item goes the same way.
        // The rest of the branch was removed with no replacement: unknown entries are dropped on load.
        vanilla("uranium_ore", "stone");
        vanilla("deepslate_uranium_ore", "deepslate");
    }

    private static void item(String oldPath, DeferredHolder<Item, ? extends Item> target) {
        ModItems.ITEMS.addAlias(id(oldPath), target.getId());
    }

    /** Block and its item both go to the vanilla block of the same name. */
    private static void vanilla(String oldPath, String vanillaPath) {
        ResourceLocation to = ResourceLocation.withDefaultNamespace(vanillaPath);
        ModBlocks.BLOCKS.addAlias(id(oldPath), to);
        ModItems.ITEMS.addAlias(id(oldPath), to);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, path);
    }
}
