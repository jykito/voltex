package com.jykito.industrialcore;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = IndustrialCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    private static final ForgeConfigSpec.BooleanValue MINER_DEDUP_ORES = BUILDER
            .comment("Virtual Miner: yield only ONE ore per material (dedup by forge:ores/<material> tags).",
                     "true — when several mods provide it, the primary mod's ore is used (see minerOrePriority).",
                     "false — lenses yield the ores of all installed mods as-is.")
            .define("minerDedupOres", true);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MINER_ORE_PRIORITY = BUILDER
            .comment("Mod priority for Miner ore dedup: whose variant of a material counts as primary.",
                     "First in the list = highest priority. Unlisted mods rank lower (alphabetically among themselves).",
                     "Keep in sync with the AlmostUnified config if you use it. Example: [\"mekanism\", \"industrial_core\", \"minecraft\"]")
            .defineListAllowEmpty("minerOrePriority", List.of("industrial_core", "minecraft"), o -> o instanceof String);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    public static boolean minerDedupOres = true;
    public static List<String> minerOrePriority = List.of("industrial_core", "minecraft");

    private static boolean validateItemName(final Object obj)
    {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        items = ITEM_STRINGS.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)))
                .collect(Collectors.toSet());

        minerDedupOres = MINER_DEDUP_ORES.get();
        minerOrePriority = MINER_ORE_PRIORITY.get().stream().map(String::valueOf).collect(Collectors.toList());

        com.jykito.industrialcore.item.custom.MinerLensItem.clearCache();
    }
}
