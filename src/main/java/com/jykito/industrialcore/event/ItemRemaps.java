package com.jykito.industrialcore.event;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.item.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Forwards old item registry IDs to new ones on rename. Without this Forge
 * removes entries missing from the registry from existing saves, and the item disappears from
 * inventories and chests. The remap converts the reference in place, KEEPING NBT/charge.
 *
 * ⚠ On EVERY item rename (registry ID change) add a line to {@link #REMAP}.
 *   Keep the entries FOREVER: old saves keep turning up for a long time, and a remap is cheap.
 */
// IMPORTANT: MissingMappingsEvent in Forge 1.20.1 is on the FORGE bus (NOT the mod bus, as in old Forge)
@Mod.EventBusSubscriber(modid = IndustrialCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemRemaps {

    /** old ID path  ->  new item */
    private static final Map<String, RegistryObject<Item>> REMAP = Map.ofEntries(
            Map.entry("energy_crystal",   ModItems.FLUX_CORE),   // 2026-07: crystal -> core rebrand
            Map.entry("lapotron_crystal", ModItems.PLASMA_CORE),

            // 2026-08: connectors removed, upgrades now go into the hub
            Map.entry("connector_upgrade_t1", ModItems.HUB_UPGRADE_T1),
            Map.entry("connector_upgrade_t2", ModItems.HUB_UPGRADE_T2),
            Map.entry("connector_upgrade_t3", ModItems.HUB_UPGRADE_T3)
    );

    // 2026-09: the nuclear branch was cut (2.1.0). Uranium ore already sits in generated worlds,
    // so we turn it into stone, otherwise the veins would become voids underground. The ore item goes the same way.
    private static final Map<String, Supplier<Block>> BLOCK_REMAP = Map.of(
            "uranium_ore",           () -> Blocks.STONE,
            "deepslate_uranium_ore", () -> Blocks.DEEPSLATE
    );
    private static final Map<String, Supplier<Item>> VANILLA_ITEM_REMAP = Map.of(
            "uranium_ore",           () -> Items.STONE,
            "deepslate_uranium_ore", () -> Items.DEEPSLATE
    );

    /**
     * Removed on purpose with no replacement (2026-09, nuclear branch). Marked as ignore so Forge does not
     * show the missing entries screen; the player was warned by the 2.1.0 changelog.
     */
    private static final Set<String> REMOVED = Set.of(
            // blocks (and their items)
            "nuclear_reactor", "reactor_chamber", "raw_uranium_block",
            // items
            "raw_uranium", "uranium_dust", "uranium_ingot", "plutonium", "mox_fuel",
            "empty_reinforced_fuel_rod", "uranium_fuel_rod", "plutonium_fuel_rod", "mox_fuel_rod",
            "depleted_uranium_fuel_rod", "depleted_plutonium_fuel_rod", "depleted_mox_fuel_rod",
            "reactor_blueprint",
            "reactor_scheme_u1", "reactor_scheme_u2", "reactor_scheme_u3",
            "reactor_scheme_p1", "reactor_scheme_p2", "reactor_scheme_p3", "reactor_scheme_p4",
            "reactor_scheme_m1", "reactor_scheme_m2", "reactor_scheme_m3", "reactor_scheme_m4",
            // reactor block entities and menus
            "nuclear_reactor_be", "reactor_chamber_be", "nuclear_reactor_menu"
    );

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        for (var mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, IndustrialCore.MODID)) {
            String path = mapping.getKey().getPath();
            RegistryObject<Item> replacement = REMAP.get(path);
            if (replacement != null) { mapping.remap(replacement.get()); continue; }
            Supplier<Item> vanilla = VANILLA_ITEM_REMAP.get(path);
            if (vanilla != null) { mapping.remap(vanilla.get()); continue; }
            if (REMOVED.contains(path)) mapping.ignore();
        }
        for (var mapping : event.getMappings(ForgeRegistries.Keys.BLOCKS, IndustrialCore.MODID)) {
            String path = mapping.getKey().getPath();
            Supplier<Block> block = BLOCK_REMAP.get(path);
            if (block != null) { mapping.remap(block.get()); continue; }
            if (REMOVED.contains(path)) mapping.ignore();
        }
        ignoreRemoved(event, ForgeRegistries.Keys.BLOCK_ENTITY_TYPES);
        ignoreRemoved(event, ForgeRegistries.Keys.MENU_TYPES);
    }

    private static <T> void ignoreRemoved(MissingMappingsEvent event, ResourceKey<? extends Registry<T>> key) {
        for (var mapping : event.getMappings(key, IndustrialCore.MODID)) {
            if (REMOVED.contains(mapping.getKey().getPath())) mapping.ignore();
        }
    }
}
