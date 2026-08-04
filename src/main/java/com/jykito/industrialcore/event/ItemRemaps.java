package com.jykito.industrialcore.event;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

/**
 * Пересылка старых registry-ID предметов на новые при переименовании. Без этого Форж
 * удаляет отсутствующие в реестре записи из существующих сейвов — предмет пропадает из
 * инвентарей/сундуков. Ремап конвертирует ссылку на месте, СОХРАНЯЯ NBT/заряд.
 *
 * ⚠ При КАЖДОМ переименовании предмета (смене registry-ID) добавлять строку в {@link #REMAP}.
 *   Записи держать НАВСЕГДА — старые сейвы всплывают долго, ремап дешёвый.
 *   Для блоков, если понадобится, — отдельная таблица + свой цикл по ForgeRegistries.Keys.BLOCKS.
 */
// ВАЖНО: MissingMappingsEvent в Forge 1.20.1 — FORGE-шина (НЕ mod-bus, как было в старом Forge)
@Mod.EventBusSubscriber(modid = IndustrialCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemRemaps {

    /** старый путь ID  ->  новый предмет */
    private static final Map<String, RegistryObject<Item>> REMAP = Map.ofEntries(
            Map.entry("energy_crystal",   ModItems.FLUX_CORE),   // 2026-07: crystal -> core ребренд
            Map.entry("lapotron_crystal", ModItems.PLASMA_CORE),

            // 2026-08: коннекторы снесены, апгрейды теперь ставятся в хаб
            Map.entry("connector_upgrade_t1", ModItems.HUB_UPGRADE_T1),
            Map.entry("connector_upgrade_t2", ModItems.HUB_UPGRADE_T2),
            Map.entry("connector_upgrade_t3", ModItems.HUB_UPGRADE_T3)
    );

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        for (var mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, IndustrialCore.MODID)) {
            RegistryObject<Item> replacement = REMAP.get(mapping.getKey().getPath());
            if (replacement != null) {
                mapping.remap(replacement.get());
            }
        }
    }
}
