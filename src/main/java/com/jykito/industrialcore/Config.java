package com.jykito.industrialcore;


import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

import java.util.List;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = IndustrialCore.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    // ---------------------------------------------------------------- COMMON
    // World logic: the same on a server and in singleplayer, the client does not affect it.

    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue MINER_DEDUP_ORES = COMMON_BUILDER
            .comment("Virtual Miner: yield only ONE ore per material (dedup by forge:ores/<material> tags).",
                     "true — when several mods provide it, the primary mod's ore is used (see minerOrePriority).",
                     "false — lenses yield the ores of all installed mods as-is.")
            .define("minerDedupOres", true);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> MINER_ORE_PRIORITY = COMMON_BUILDER
            .comment("Mod priority for Miner ore dedup: whose variant of a material counts as primary.",
                     "First in the list = highest priority. Unlisted mods rank lower (alphabetically among themselves).",
                     "Keep in sync with the AlmostUnified config if you use it. Example: [\"mekanism\", \"industrial_core\", \"minecraft\"]")
            .defineListAllowEmpty("minerOrePriority", List.of("industrial_core", "minecraft"), o -> o instanceof String);

    private static final ModConfigSpec.IntValue ANTIGRAV_COLUMN_LIMIT = COMMON_BUILDER
            .comment("Max antigrav columns loaded at once per level (FPS guard — each is an animated block-entity).",
                     "Placement is blocked beyond this. 0 = unlimited. Note: a magnet only uses up to 8 for cooling.")
            .defineInRange("antigravColumnLimit", 24, 0, 4096);

    private static final ModConfigSpec.BooleanValue DEV_TOOLS = COMMON_BUILDER
            .comment("Show mod-development tools (Recipe Exporter) in the creative tab.",
                     "These write files to the pack folder and are meant for authoring, not for playing.")
            .define("devTools", false);

    static final ModConfigSpec COMMON_SPEC = COMMON_BUILDER.build();

    // ---------------------------------------------------------------- CLIENT
    // Rendering and sound only. On a dedicated server this file is not created,
    // the fields below keep their defaults and nobody reads them.

    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue SOUND_VOLUME = CLIENT_BUILDER
            .comment("Volume of this mod's machine sounds (electromagnet hum, column flybys, power-up).",
                     "1.0 = full, 0.0 = muted. Scales on top of your in-game Blocks volume slider.")
            .defineInRange("soundVolume", 1.0, 0.0, 1.0);

    private static final ModConfigSpec.BooleanValue CULL_SOLID_FACES = CLIENT_BUILDER
            .comment("Render optimization: back-face cull the closed solid models (magnet body/coils/pillars, antigrav columns).",
                     "true = ~2x less fragment work up close (faster under shaders). false = original NO_CULL (both sides drawn).",
                     "Set false if enabling this makes any face disappear — it returns to exactly the old look.")
            .define("cullSolidFaces", true);

    static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    // ---------------------------------------------------------------- values

    public static boolean minerDedupOres = true;
    public static List<String> minerOrePriority = List.of("industrial_core", "minecraft");
    public static int antigravColumnLimit = 24;  // column cap per dimension (0 = no limit)
    public static boolean devTools = false;      // dev tools in the creative tab

    public static double soundVolume = 1.0;
    public static boolean cullSolidFaces = true; // optimization rollback: false -> render exactly as before (NO_CULL)

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        // ModConfigEvent on the mod bus only arrives for this mod's configs, so the type is enough.
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            minerDedupOres     = MINER_DEDUP_ORES.get();
            minerOrePriority   = MINER_ORE_PRIORITY.get().stream().map(String::valueOf).collect(Collectors.toList());
            antigravColumnLimit = ANTIGRAV_COLUMN_LIMIT.get();
            devTools           = DEV_TOOLS.get();

            com.jykito.industrialcore.item.custom.MinerLensItem.clearCache();
        } else if (event.getConfig().getType() == ModConfig.Type.CLIENT) {
            soundVolume     = SOUND_VOLUME.get();
            cullSolidFaces  = CULL_SOLID_FACES.get();
        }
    }
}
