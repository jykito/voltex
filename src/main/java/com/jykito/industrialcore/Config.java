package com.jykito.industrialcore;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = IndustrialCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    // ---------------------------------------------------------------- COMMON
    // Логика мира: одинакова на сервере и в одиночке, клиент на неё не влияет.

    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue MINER_DEDUP_ORES = COMMON_BUILDER
            .comment("Virtual Miner: yield only ONE ore per material (dedup by forge:ores/<material> tags).",
                     "true — when several mods provide it, the primary mod's ore is used (see minerOrePriority).",
                     "false — lenses yield the ores of all installed mods as-is.")
            .define("minerDedupOres", true);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MINER_ORE_PRIORITY = COMMON_BUILDER
            .comment("Mod priority for Miner ore dedup: whose variant of a material counts as primary.",
                     "First in the list = highest priority. Unlisted mods rank lower (alphabetically among themselves).",
                     "Keep in sync with the AlmostUnified config if you use it. Example: [\"mekanism\", \"industrial_core\", \"minecraft\"]")
            .defineListAllowEmpty("minerOrePriority", List.of("industrial_core", "minecraft"), o -> o instanceof String);

    private static final ForgeConfigSpec.IntValue ANTIGRAV_COLUMN_LIMIT = COMMON_BUILDER
            .comment("Max antigrav columns loaded at once per level (FPS guard — each is an animated block-entity).",
                     "Placement is blocked beyond this. 0 = unlimited. Note: a magnet only uses up to 8 for cooling.")
            .defineInRange("antigravColumnLimit", 24, 0, 4096);

    private static final ForgeConfigSpec.BooleanValue DEV_TOOLS = COMMON_BUILDER
            .comment("Show mod-development tools (Recipe Exporter) in the creative tab.",
                     "These write files to the pack folder and are meant for authoring, not for playing.")
            .define("devTools", false);

    static final ForgeConfigSpec COMMON_SPEC = COMMON_BUILDER.build();

    // ---------------------------------------------------------------- CLIENT
    // Только рендер и звук. На выделенном сервере этот файл не создаётся,
    // поля ниже остаются на дефолтах и никем не читаются.

    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue SHOW_MACHINE_ITEM = CLIENT_BUILDER
            .comment("Render the in-progress item floating in the center of working Restructors and Cultivators (visual only).",
                     "Turn off to reclaim FPS in very large setups (hundreds of working machines in view).")
            .define("showMachineItem", true);

    private static final ForgeConfigSpec.DoubleValue SOUND_VOLUME = CLIENT_BUILDER
            .comment("Volume of this mod's machine sounds (electromagnet hum, column flybys, power-up).",
                     "1.0 = full, 0.0 = muted. Scales on top of your in-game Blocks volume slider.")
            .defineInRange("soundVolume", 1.0, 0.0, 1.0);

    private static final ForgeConfigSpec.BooleanValue CULL_SOLID_FACES = CLIENT_BUILDER
            .comment("Render optimization: back-face cull the closed solid models (magnet body/coils/pillars, antigrav columns).",
                     "true = ~2x less fragment work up close (faster under shaders). false = original NO_CULL (both sides drawn).",
                     "Set false if enabling this makes any face disappear — it returns to exactly the old look.")
            .define("cullSolidFaces", true);

    static final ForgeConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    // ---------------------------------------------------------------- значения

    public static boolean minerDedupOres = true;
    public static List<String> minerOrePriority = List.of("industrial_core", "minecraft");
    public static int antigravColumnLimit = 24;  // кап колонн на измерение (0 = без лимита)
    public static boolean devTools = false;      // дев-инструменты в креатив-табе

    public static boolean showMachineItem = true;
    public static double soundVolume = 1.0;
    public static boolean cullSolidFaces = true; // откат оптимизации: false → рендер ровно как был (NO_CULL)

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        // ModConfigEvent на mod-шине приходит только для конфигов этого мода, так что типа достаточно.
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            minerDedupOres     = MINER_DEDUP_ORES.get();
            minerOrePriority   = MINER_ORE_PRIORITY.get().stream().map(String::valueOf).collect(Collectors.toList());
            antigravColumnLimit = ANTIGRAV_COLUMN_LIMIT.get();
            devTools           = DEV_TOOLS.get();

            com.jykito.industrialcore.item.custom.MinerLensItem.clearCache();
        } else if (event.getConfig().getType() == ModConfig.Type.CLIENT) {
            showMachineItem = SHOW_MACHINE_ITEM.get();
            soundVolume     = SOUND_VOLUME.get();
            cullSolidFaces  = CULL_SOLID_FACES.get();
        }
    }
}
