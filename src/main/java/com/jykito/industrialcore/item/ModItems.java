   package com.jykito.industrialcore.item;

    import com.jykito.industrialcore.IndustrialCore;
    import com.jykito.industrialcore.block.ModBlocks;
    import com.jykito.industrialcore.item.custom.EnergyBackpackItem;
    import com.jykito.industrialcore.item.upgrade.*;
    import net.minecraft.network.chat.Component;
    import net.minecraft.world.item.BlockItem;
    import net.minecraft.world.item.Item;
    import net.minecraft.world.item.ItemStack;
    import net.minecraft.world.item.TooltipFlag;
    import net.minecraft.world.level.Level;
    import net.minecraftforge.eventbus.api.IEventBus;
    import org.jetbrains.annotations.Nullable;
    import net.minecraftforge.registries.DeferredRegister;
    import net.minecraftforge.registries.ForgeRegistries;
    import net.minecraftforge.registries.RegistryObject;

    public class ModItems {
        public static final DeferredRegister<Item> ITEMS =
                DeferredRegister.create(ForgeRegistries.ITEMS, IndustrialCore.MODID);

        public static final RegistryObject<Item> MACHINE_CASING_ITEM = ITEMS.register("machine_casing",
                () -> new BlockItem(ModBlocks.MACHINE_CASING.get(), new Item.Properties()));

        public static final RegistryObject<Item> MANUAL = ITEMS.register("manual",
                () -> new com.jykito.industrialcore.item.custom.ManualItem(new Item.Properties().stacksTo(1)));

        // --- Accelerator/magnet components (2026-07-10) ---
        public static final RegistryObject<Item> COIL_WINDING = ITEMS.register("coil_winding", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> SIGNAL_RECEIVER = ITEMS.register("signal_receiver", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> FLUID_VALVE = ITEMS.register("fluid_valve", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> THERMAL_INSULATION = ITEMS.register("thermal_insulation", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> MANIPULATOR = ITEMS.register("manipulator", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> HARDENED_STEEL_ROD = ITEMS.register("hardened_steel_rod", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> MATERIAL_CONTAINER = ITEMS.register("material_container", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> STAR_SHARD = ITEMS.register("star_shard", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> CARBON_COMPOSITE_INGOT = ITEMS.register("carbon_composite_ingot", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> PIEZO_ELEMENT = ITEMS.register("piezo_element", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> HEAT_RESISTANT_PLATING = ITEMS.register("heat_resistant_plating", () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> COAL_GENERATOR_ITEM = ITEMS.register("coal_generator",
                () -> new BlockItem(ModBlocks.COAL_GENERATOR.get(), new Item.Properties()));

        public static final RegistryObject<Item> CRUSHER_ITEM = ITEMS.register("crusher",
                () -> new net.minecraft.world.item.BlockItem(ModBlocks.CRUSHER.get(), new Item.Properties()));

        public static final RegistryObject<Item> ELECTRIC_FURNACE_ITEM = ITEMS.register("electric_furnace",
                () -> new BlockItem(ModBlocks.ELECTRIC_FURNACE.get(), new Item.Properties()));

        public static final RegistryObject<Item> COMPRESSOR_ITEM = ITEMS.register("compressor",
                () -> new net.minecraft.world.item.BlockItem(ModBlocks.COMPRESSOR.get(), new Item.Properties()));

        public static final RegistryObject<Item> METAL_FORMER_ITEM = ITEMS.register("metal_former",
                () -> new net.minecraft.world.item.BlockItem(ModBlocks.METAL_FORMER.get(), new Item.Properties()));

        public static final RegistryObject<Item> THERMOGENERATOR_ITEM = ITEMS.register("thermogenerator",
                () -> new BlockItem(ModBlocks.THERMOGENERATOR.get(), new Item.Properties()));

        public static final RegistryObject<Item> MOD_BLAST_FURNACE_ITEM = ITEMS.register("blast_furnace",
                () -> new BlockItem(ModBlocks.BLAST_FURNACE.get(), new Item.Properties()));

        public static final RegistryObject<Item> FlUID_ENRICHER = ITEMS.register("fluid_enricher",
                () -> new BlockItem(ModBlocks.FLUID_ENRICHER.get(), new Item.Properties()));

        public static final RegistryObject<Item> LIQUEFIER_ITEM = ITEMS.register("liquefier",
                () -> new BlockItem(ModBlocks.LIQUEFIER.get(), new Item.Properties()));

        public static final RegistryObject<Item> CRYSTALLIZER_ITEM = ITEMS.register("crystallizer",
                () -> new BlockItem(ModBlocks.CRYSTALLIZER.get(), new Item.Properties()));
        public static final RegistryObject<Item> EVOLUTION_CHAMBER_ITEM = ITEMS.register("evolution_chamber",
                () -> new BlockItem(ModBlocks.EVOLUTION_CHAMBER.get(), new Item.Properties()));
        public static final RegistryObject<Item> NANITE_LAB_ITEM = ITEMS.register("nanite_lab",
                () -> new BlockItem(ModBlocks.NANITE_LAB.get(), new Item.Properties()));

        public static final RegistryObject<Item> ORE_WASHER_ITEM = ITEMS.register("ore_washer",
                () -> new BlockItem(ModBlocks.ORE_WASHER.get(), new Item.Properties()));

        public static final RegistryObject<Item> BIOREACTOR_ITEM = ITEMS.register("bioreactor",
                () -> new BlockItem(ModBlocks.BIOREACTOR.get(), new Item.Properties()));

        public static final RegistryObject<Item> LAVA_GENERATOR_ITEM = ITEMS.register("lava_generator",
                () -> new BlockItem(ModBlocks.LAVA_GENERATOR.get(), new Item.Properties()));
        public static final RegistryObject<Item> WATER_GENERATOR_ITEM = ITEMS.register("water_generator",
                () -> new BlockItem(ModBlocks.WATER_GENERATOR.get(), new Item.Properties()));
        public static final RegistryObject<Item> STONE_GENERATOR_ITEM = ITEMS.register("stone_generator",
                () -> new BlockItem(ModBlocks.STONE_GENERATOR.get(), new Item.Properties()));
        public static final RegistryObject<Item> CAPSULE_FILLER_ITEM = ITEMS.register("capsule_filler",
                () -> new BlockItem(ModBlocks.CAPSULE_FILLER.get(), new Item.Properties()));
        public static final RegistryObject<Item> CULTIVATOR_ITEM = ITEMS.register("cultivator",
                () -> new BlockItem(ModBlocks.CULTIVATOR.get(), new Item.Properties()));
        public static final RegistryObject<Item> CULTIVATOR_T2_ITEM = ITEMS.register("cultivator_t2",
                () -> new BlockItem(ModBlocks.CULTIVATOR_T2.get(), new Item.Properties()));

        public static final RegistryObject<Item> CULTIVATOR_BIOSTIMULATOR = ITEMS.register("cultivator_biostimulator",
                () -> new CultivatorUpgradeItem(CultivatorUpgradeItem.Mode.BIOSTIMULATOR, new Item.Properties()));
        public static final RegistryObject<Item> CULTIVATOR_RESONATOR = ITEMS.register("cultivator_resonator",
                () -> new CultivatorUpgradeItem(CultivatorUpgradeItem.Mode.CRYSTAL_RESONATOR, new Item.Properties()));

        public static final RegistryObject<Item> RECIPE_EXPORTER_ITEM = ITEMS.register("recipe_exporter",
                () -> new BlockItem(ModBlocks.RECIPE_EXPORTER.get(), new Item.Properties()));

        public static final RegistryObject<Item> MATTER_FABRICATOR_ITEM = ITEMS.register("matter_fabricator",
                () -> new BlockItem(ModBlocks.MATTER_FABRICATOR.get(), new Item.Properties()));

        public static final RegistryObject<Item> SILICON = ITEMS.register("silicon",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> SILICON_DUST = ITEMS.register("silicon_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> MINERAL_DUST = ITEMS.register("mineral_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> NICKEL_DUST = ITEMS.register("nickel_dust",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> NICKEL_INGOT = ITEMS.register("nickel_ingot",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> SILVER_DUST = ITEMS.register("silver_dust",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> SILVER_INGOT = ITEMS.register("silver_ingot",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> INVAR_INGOT = ITEMS.register("invar_ingot",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> INVAR_PLATE = ITEMS.register("invar_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> LITHIUM_DUST = ITEMS.register("lithium_dust",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> LITHIUM = ITEMS.register("lithium",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> BAUXITE_ORE_ITEM = ITEMS.register("bauxite_ore",
                () -> new BlockItem(ModBlocks.BAUXITE_ORE.get(), new Item.Properties()));
        public static final RegistryObject<Item> DEEPSLATE_BAUXITE_ORE_ITEM = ITEMS.register("deepslate_bauxite_ore",
                () -> new BlockItem(ModBlocks.DEEPSLATE_BAUXITE_ORE.get(), new Item.Properties()));
        public static final RegistryObject<Item> RAW_ALUMINUM = ITEMS.register("raw_aluminum",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> ALUMINUM_INGOT = ITEMS.register("aluminum_ingot",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> ALUMINUM_DUST = ITEMS.register("aluminum_dust",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> ALUMINUM_PLATE = ITEMS.register("aluminum_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> SOLAR_PANEL_T6_ITEM = ITEMS.register("solar_panel_t6",
                () -> new BlockItem(ModBlocks.SOLAR_PANEL_T6.get(), new Item.Properties()));
        public static final RegistryObject<Item> SOLAR_PANEL_T7_ITEM = ITEMS.register("solar_panel_t7",
                () -> new BlockItem(ModBlocks.SOLAR_PANEL_T7.get(), new Item.Properties()));
        public static final RegistryObject<Item> SOLAR_PANEL_T8_ITEM = ITEMS.register("solar_panel_t8",
                () -> new BlockItem(ModBlocks.SOLAR_PANEL_T8.get(), new Item.Properties()));
        public static final RegistryObject<Item> SOLAR_PANEL_T9_ITEM = ITEMS.register("solar_panel_t9",
                () -> new BlockItem(ModBlocks.SOLAR_PANEL_T9.get(), new Item.Properties()));
        public static final RegistryObject<Item> SOLAR_PANEL_T10_ITEM = ITEMS.register("solar_panel_t10",
                () -> new BlockItem(ModBlocks.SOLAR_PANEL_T10.get(), new Item.Properties()));

        public static final RegistryObject<Item> EXOTIC_MATTER = ITEMS.register("exotic_matter",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> PHOTONIC_LENS = ITEMS.register("photonic_lens",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> SUPERCONDUCTOR_COIL = ITEMS.register("superconductor_coil",
                () -> new Item(new Item.Properties()));
        // Electromagnet-accelerator coils (tiers): inserted into the controller, they set the level
        public static final RegistryObject<Item> MAGNET_COPPER_COIL = ITEMS.register("magnet_copper_coil",
                () -> new com.jykito.industrialcore.item.custom.MagnetCoilItem(1, 1.0f, 0.06f, "tooltip.industrial_core.res_energy", new Item.Properties()));
        public static final RegistryObject<Item> MAGNET_SUPERCONDUCTOR_COIL = ITEMS.register("magnet_superconductor_coil",
                () -> new com.jykito.industrialcore.item.custom.MagnetCoilItem(2, 1.5f, 0.11f, "tooltip.industrial_core.res_energy_cryo", new Item.Properties()));
        public static final RegistryObject<Item> MAGNET_GRAVITON_COIL = ITEMS.register("magnet_graviton_coil",
                () -> new com.jykito.industrialcore.item.custom.MagnetCoilItem(3, 2.2f, 0.17f, "tooltip.industrial_core.res_energy_contain", new Item.Properties()));
        public static final RegistryObject<Item> PHASE_LOCK_UPGRADE = ITEMS.register("phase_lock_upgrade",
                () -> new com.jykito.industrialcore.item.custom.PhaseLockUpgradeItem(new Item.Properties()));
        public static final RegistryObject<Item> STRUCTURE_WAND = ITEMS.register("structure_wand",
                () -> new com.jykito.industrialcore.item.custom.StructureWandItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> QUANTUM_CAPACITOR = ITEMS.register("quantum_capacitor",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> SINGULARITY_CORE = ITEMS.register("singularity_core",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> STELLAR_MATRIX = ITEMS.register("stellar_matrix",
                () -> new Item(new Item.Properties().stacksTo(16)));

        public static final RegistryObject<Item> ELECTRUM_INGOT = ITEMS.register("electrum_ingot",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> ELECTRUM_PLATE = ITEMS.register("electrum_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> IRON_DUST = ITEMS.register("iron_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> GOLD_DUST = ITEMS.register("gold_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> COPPER_DUST = ITEMS.register("copper_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> DIAMOND_DUST = ITEMS.register("diamond_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> LAPIS_DUST = ITEMS.register("lapis_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> COAL_DUST = ITEMS.register("coal_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> EMERALD_DUST = ITEMS.register("emerald_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> NETHERITE_DUST = ITEMS.register("netherite_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> TREE_RESIN = ITEMS.register("tree_resin",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> COMPOSITE = ITEMS.register("composite",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> CARBON_PLATE = ITEMS.register("carbon_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> CARBON_MESH = ITEMS.register("carbon_mesh",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> CARBON_FIBER = ITEMS.register("carbon_fiber",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> COMPOSITE_INGOT = ITEMS.register("composite_ingot",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> BRONZE_INGOT = ITEMS.register("bronze_ingot",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> BRONZE_DUST = ITEMS.register("bronze_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> NETHERITE_PLATE = ITEMS.register("netherite_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> IRON_PLATE = ITEMS.register("iron_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> COPPER_PLATE = ITEMS.register("copper_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> GOLD_PLATE = ITEMS.register("gold_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> BRONZE_PLATE = ITEMS.register("bronze_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> LEAD_PLATE = ITEMS.register("lead_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> TIN_PLATE = ITEMS.register("tin_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> DIAMOND_PLATE = ITEMS.register("diamond_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> EMERALD_PLATE = ITEMS.register("emerald_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> LAPIS_PLATE = ITEMS.register("lapis_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> AMETHYST_DUST = ITEMS.register("amethyst_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> AMETHYST_CRYSTAL = ITEMS.register("amethyst_crystal",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> CRYSTAL_INGOT = ITEMS.register("crystal_ingot",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> CRYSTAL_FIBER = ITEMS.register("crystal_fiber",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> CRYSTAL_PLATE = ITEMS.register("crystal_plate",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> EXTRACTOR_ITEM = ITEMS.register("extractor",
                () -> new BlockItem(ModBlocks.EXTRACTOR.get(), new Item.Properties()));

        public static final RegistryObject<Item> KRONIT = ITEMS.register("kronit",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> BASIC_CATALYST = ITEMS.register("basic_catalyst",
                () -> new com.jykito.industrialcore.item.custom.CatalystItem("tooltip.industrial_core.basic_catalyst"));
        public static final RegistryObject<Item> ACTIVATED_CATALYST = ITEMS.register("activated_catalyst",
                () -> new com.jykito.industrialcore.item.custom.CatalystItem("tooltip.industrial_core.activated_catalyst"));
        public static final RegistryObject<Item> RESONANT_CATALYST = ITEMS.register("resonant_catalyst",
                () -> new com.jykito.industrialcore.item.custom.CatalystItem("tooltip.industrial_core.resonant_catalyst"));
        public static final RegistryObject<Item> UNIVERSAL_CATALYST = ITEMS.register("universal_catalyst",
                () -> new com.jykito.industrialcore.item.custom.CatalystItem("tooltip.industrial_core.universal_catalyst"));

        public static final RegistryObject<Item> RESTRUCTOR_ITEM = ITEMS.register("restructor",
                () -> new BlockItem(ModBlocks.RESTRUCTOR.get(), new Item.Properties()));
        public static final RegistryObject<Item> RESTRUCTOR_T2_ITEM = ITEMS.register("restructor_t2",
                () -> new BlockItem(ModBlocks.RESTRUCTOR_T2.get(), new Item.Properties()));

        // ── Cryo branch ──
        // Cryolite: the base of the branch, crystallizes from coolant.
        // Cryo rods are the heat exchangers of the cryo chamber: they set the processing SPEED (depth is set by
        // the heat flow, batch size by the chamber size) and wear out faster the deeper it goes.
        // Permission for depth. Goes into the chamber core, not consumed. A higher one overrides a lower one,
        // so there is one slot: whatever you insert, that is the stage it lets you reach.
        public static final RegistryObject<Item> DEPTH_MODULE_2 = ITEMS.register("depth_module_2",
                () -> new com.jykito.industrialcore.item.custom.DepthModuleItem(2, new Item.Properties()));

        public static final RegistryObject<Item> DEPTH_MODULE_3 = ITEMS.register("depth_module_3",
                () -> new com.jykito.industrialcore.item.custom.DepthModuleItem(3, new Item.Properties()));

        /** Product of the second cryo chamber stage: T2 rods grow from it. */
        public static final RegistryObject<Item> CRYO_CRYSTAL = ITEMS.register("cryo_crystal",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> CRYOLITE = ITEMS.register("cryolite",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> CRYO_GLASS_ITEM = ITEMS.register("cryo_glass",
                () -> new BlockItem(ModBlocks.CRYO_GLASS.get(), new Item.Properties()));

        public static final RegistryObject<Item> CRYO_CASING_ITEM = ITEMS.register("cryo_casing",
                () -> new BlockItem(ModBlocks.CRYO_CASING.get(), new Item.Properties()));

        public static final RegistryObject<Item> CRYO_CORE_ITEM = ITEMS.register("cryo_core",
                () -> new BlockItem(ModBlocks.CRYO_CORE.get(), new Item.Properties()));

        public static final RegistryObject<Item> REFRIGERATION_CHAMBER_ITEM = ITEMS.register("refrigeration_chamber",
                () -> new BlockItem(ModBlocks.REFRIGERATION_CHAMBER.get(), new Item.Properties()));

        // Cryo rods wear out in the chamber, so they have durability: the bar in the slot shows
        // what is left without any extra UI. The tier gives both speed and lifetime; they cannot be repaired,
        // they are a consumable, not a tool.
        public static final RegistryObject<Item> CRYO_ROD = ITEMS.register("cryo_rod",
                () -> new Item(new Item.Properties().durability(1024).setNoRepair()));

        public static final RegistryObject<Item> CRYO_ROD_T2 = ITEMS.register("cryo_rod_t2",
                () -> new Item(new Item.Properties().durability(2048).setNoRepair()));

        public static final RegistryObject<Item> CRYO_ROD_T3 = ITEMS.register("cryo_rod_t3",
                () -> new Item(new Item.Properties().durability(4096).setNoRepair()));

        public static final RegistryObject<Item> PHASE_SPLITTER_ITEM = ITEMS.register("phase_splitter",
                () -> new BlockItem(ModBlocks.PHASE_SPLITTER.get(), new Item.Properties()));

        public static final RegistryObject<Item> TEXTOLITE_BOARD = ITEMS.register("textolite_board",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> LOGIC_BOARD = ITEMS.register("logic_board",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> DATA_TRANSFER_MATRIX = ITEMS.register("data_transfer_matrix",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> MICROCONTROLLER = ITEMS.register("microcontroller",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> ELECTRIC_MOTOR = ITEMS.register("electric_motor",
                () -> new Item(new Item.Properties()));
        // motor tiers (author, 2026-10-06): the author's textures, animation: the advanced one heats up, the extended one sparks; no recipe yet
        public static final RegistryObject<Item> ELECTRIC_MOTOR_ADVANCED = ITEMS.register("electric_motor_advanced",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> ELECTRIC_MOTOR_EXTENDED = ITEMS.register("electric_motor_extended",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> HEAT_CONDUCTOR = ITEMS.register("heat_conductor",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> ADVANCED_HEAT_EXCHANGER = ITEMS.register("advanced_heat_exchanger",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> HEAT_PROBE = ITEMS.register("heat_probe",
                () -> new com.jykito.industrialcore.item.custom.HeatProbeItem(new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> CRYOGENIC_HEAT_EXCHANGER = ITEMS.register("cryogenic_heat_exchanger",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> INTEGRAL_HEAT_VENT = ITEMS.register("integral_heat_vent",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> BATTERY = ITEMS.register("battery",
                () -> new BatteryItem(new Item.Properties().stacksTo(1), 10000, 1000));

        public static final RegistryObject<Item> LITHIUM_BATTERY = ITEMS.register("lithium_battery",
                () -> new BatteryItem(new Item.Properties().stacksTo(1), 50000, 5000));

        public static final RegistryObject<Item> FLUX_CORE = ITEMS.register("flux_core",
                () -> new BatteryItem(new Item.Properties().stacksTo(1), 100000, 10000));

        public static final RegistryObject<Item> PLASMA_CORE = ITEMS.register("plasma_core",
                () -> new BatteryItem(new Item.Properties().stacksTo(1), 500000, 50000));

        public static final RegistryObject<Item> BATTERY_CHARGER = ITEMS.register("battery_charger",
                () -> new BlockItem(ModBlocks.BATTERY_CHARGER.get(), new Item.Properties()));

        public static final RegistryObject<Item> TIN_ORE_ITEM = ITEMS.register("tin_ore",
                () -> new BlockItem(ModBlocks.TIN_ORE.get(), new Item.Properties()));
        public static final RegistryObject<Item> DEEPSLATE_TIN_ORE_ITEM = ITEMS.register("deepslate_tin_ore",
                () -> new BlockItem(ModBlocks.DEEPSLATE_TIN_ORE.get(), new Item.Properties()));

        public static final RegistryObject<Item> LEAD_ORE_ITEM = ITEMS.register("lead_ore",
                () -> new BlockItem(ModBlocks.LEAD_ORE.get(), new Item.Properties()));
        public static final RegistryObject<Item> DEEPSLATE_LEAD_ORE_ITEM = ITEMS.register("deepslate_lead_ore",
                () -> new BlockItem(ModBlocks.DEEPSLATE_LEAD_ORE.get(), new Item.Properties()));

        public static final RegistryObject<Item> RAW_TIN = ITEMS.register("raw_tin",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> RAW_LEAD = ITEMS.register("raw_lead",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> RESIN_LOG_ITEM = ITEMS.register("resin_log",
                () -> new BlockItem(ModBlocks.RESIN_LOG.get(), new Item.Properties()));

        public static final RegistryObject<Item> RESIN_LEAVES_ITEM = ITEMS.register("resin_leaves",
                () -> new BlockItem(ModBlocks.RESIN_LEAVES.get(), new Item.Properties()));

        public static final RegistryObject<Item> RESIN_SAPLING_ITEM = ITEMS.register("resin_sapling",
                () -> new BlockItem(ModBlocks.RESIN_SAPLING.get(), new Item.Properties()));

        public static final RegistryObject<Item> RESIN_COLLECTOR_ITEM = ITEMS.register("resin_collector",
                () -> new BlockItem(ModBlocks.RESIN_COLLECTOR.get(), new Item.Properties()) {
                    @Override
                    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                                java.util.List<Component> tooltip, TooltipFlag flag) {
                        tooltip.add(Component.translatable("tooltip.industrial_core.resin_collector")
                                .withStyle(net.minecraft.ChatFormatting.GRAY));
                        super.appendHoverText(stack, level, tooltip, flag);
                    }
                });

        public static final RegistryObject<Item> TIN_INGOT = ITEMS.register("tin_ingot",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> LEAD_INGOT = ITEMS.register("lead_ingot",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> TIN_DUST = ITEMS.register("tin_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> LEAD_DUST = ITEMS.register("lead_dust",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> COPPER_CABLE_ITEM = ITEMS.register("copper_cable",
                () -> new com.jykito.industrialcore.item.custom.WireTooltipBlockItem(ModBlocks.COPPER_CABLE.get(), new Item.Properties(), com.jykito.industrialcore.block.custom.wire.WireTier.COPPER));

        public static final RegistryObject<Item> FERRO_CABLE_ITEM = ITEMS.register("ferro_cable",
                () -> new com.jykito.industrialcore.item.custom.WireTooltipBlockItem(ModBlocks.FERRO_CABLE.get(), new Item.Properties(), com.jykito.industrialcore.block.custom.wire.WireTier.FERRO));

        public static final RegistryObject<Item> GOLD_CONDUCTOR_ITEM = ITEMS.register("gold_conductor",
                () -> new com.jykito.industrialcore.item.custom.WireTooltipBlockItem(ModBlocks.GOLD_CONDUCTOR.get(), new Item.Properties(), com.jykito.industrialcore.block.custom.wire.WireTier.GOLD));

        public static final RegistryObject<Item> CRYSTALLINE_CHANNEL_ITEM = ITEMS.register("crystalline_channel",
                () -> new com.jykito.industrialcore.item.custom.WireTooltipBlockItem(ModBlocks.CRYSTALLINE_CHANNEL.get(), new Item.Properties(), com.jykito.industrialcore.block.custom.wire.WireTier.CRYSTALLINE));

        public static final RegistryObject<Item> EMERALD_FLUX_CABLE_ITEM = ITEMS.register("emerald_flux_cable",
                () -> new com.jykito.industrialcore.item.custom.WireTooltipBlockItem(ModBlocks.EMERALD_FLUX_CABLE.get(), new Item.Properties(), com.jykito.industrialcore.block.custom.wire.WireTier.EMERALD_FLUX));

        public static final RegistryObject<Item> HEAVY_MAGISTRAL_ITEM = ITEMS.register("heavy_magistral",
                () -> new com.jykito.industrialcore.item.custom.WireTooltipBlockItem(ModBlocks.HEAVY_MAGISTRAL.get(), new Item.Properties(), com.jykito.industrialcore.block.custom.wire.WireTier.HEAVY_MAGISTRAL));

        public static final RegistryObject<Item> NETHERITE_FIBER_CABLE_ITEM = ITEMS.register("netherite_fiber_cable",
                () -> new com.jykito.industrialcore.item.custom.WireTooltipBlockItem(ModBlocks.NETHERITE_FIBER_CABLE.get(), new Item.Properties(), com.jykito.industrialcore.block.custom.wire.WireTier.NETHERITE_FIBER));

        public static final RegistryObject<Item> LOGISTICS_LINKER = ITEMS.register("logistics_linker",
                () -> new com.jykito.industrialcore.item.custom.LogisticsLinkerItem(new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> LOGISTICS_NODE_ITEM = ITEMS.register("logistics_node",
                () -> new BlockItem(ModBlocks.LOGISTICS_NODE.get(), new Item.Properties()));

        public static final RegistryObject<Item> ITEM_HUB_ITEM = ITEMS.register("item_hub",
                () -> new BlockItem(ModBlocks.ITEM_HUB.get(), new Item.Properties()));

        public static final RegistryObject<Item> FLUID_HUB_ITEM = ITEMS.register("fluid_hub",
                () -> new BlockItem(ModBlocks.FLUID_HUB.get(), new Item.Properties()));

        public static final RegistryObject<Item> HEAT_PIPE_ITEM = ITEMS.register("heat_pipe",
                () -> new BlockItem(ModBlocks.HEAT_PIPE.get(), new Item.Properties()));

        public static final RegistryObject<Item> HEAT_PIPE_T2_ITEM = ITEMS.register("heat_pipe_t2",
                () -> new BlockItem(ModBlocks.HEAT_PIPE_T2.get(), new Item.Properties()));

        public static final RegistryObject<Item> ENERGY_BANK_ITEM = ITEMS.register("energy_bank",
                () -> new BlockItem(ModBlocks.ENERGY_BANK.get(), new Item.Properties()));

        public static final RegistryObject<Item> STRUCTURAL_BLOCK_ITEM = ITEMS.register("structural_block",
                () -> new BlockItem(ModBlocks.STRUCTURAL_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> COIL_HOLDER_ITEM = ITEMS.register("coil_holder",
                () -> new BlockItem(ModBlocks.COIL_HOLDER.get(), new Item.Properties()));
        public static final RegistryObject<Item> ELECTROMAGNET_CORE_ITEM = ITEMS.register("electromagnet_core",
                () -> new BlockItem(ModBlocks.ELECTROMAGNET_CORE.get(), new Item.Properties()));
        public static final RegistryObject<Item> ANTIGRAV_COLUMN_ITEM = ITEMS.register("antigrav_column",
                () -> new BlockItem(ModBlocks.ANTIGRAV_COLUMN.get(), new Item.Properties()));
        public static final RegistryObject<Item> DISTANT_CONTROLLER_ITEM = ITEMS.register("distant_controller",
                () -> new BlockItem(ModBlocks.DISTANT_CONTROLLER.get(), new Item.Properties()));
        public static final RegistryObject<Item> THERMOSTATION_CORE_ITEM = ITEMS.register("thermostation_core",
                () -> new BlockItem(ModBlocks.THERMOSTATION_CORE.get(), new Item.Properties()));
        public static final RegistryObject<Item> THERMOSTATION_EXPANDER_ITEM = ITEMS.register("thermostation_expander",
                () -> new BlockItem(ModBlocks.THERMOSTATION_EXPANDER.get(), new Item.Properties()));
        public static final RegistryObject<Item> THERMOSTATION_RADIATOR_ITEM = ITEMS.register("thermostation_radiator",
                () -> new BlockItem(ModBlocks.THERMOSTATION_RADIATOR.get(), new Item.Properties()));

        public static final RegistryObject<Item> SOLAR_PANEL_T1_ITEM = ITEMS.register("solar_panel_t1",
                () -> new BlockItem(ModBlocks.SOLAR_PANEL_T1.get(), new Item.Properties()));
        public static final RegistryObject<Item> SOLAR_PANEL_T2_ITEM = ITEMS.register("solar_panel_t2",
                () -> new BlockItem(ModBlocks.SOLAR_PANEL_T2.get(), new Item.Properties()));
        public static final RegistryObject<Item> SOLAR_PANEL_T3_ITEM = ITEMS.register("solar_panel_t3",
                () -> new BlockItem(ModBlocks.SOLAR_PANEL_T3.get(), new Item.Properties()));
        public static final RegistryObject<Item> SOLAR_PANEL_T4_ITEM = ITEMS.register("solar_panel_t4",
                () -> new BlockItem(ModBlocks.SOLAR_PANEL_T4.get(), new Item.Properties()));
        public static final RegistryObject<Item> SOLAR_PANEL_T5_ITEM = ITEMS.register("solar_panel_t5",
                () -> new BlockItem(ModBlocks.SOLAR_PANEL_T5.get(), new Item.Properties()));

        public static final RegistryObject<Item> PANEL_COMBINER_ITEM = ITEMS.register("panel_combiner",
                () -> new BlockItem(ModBlocks.PANEL_COMBINER.get(), new Item.Properties()));

        public static final RegistryObject<Item> CRATE_T1_ITEM = ITEMS.register("crate_t1",
                () -> new BlockItem(ModBlocks.CRATE_T1.get(), new Item.Properties()));
        public static final RegistryObject<Item> CRATE_T2_ITEM = ITEMS.register("crate_t2",
                () -> new BlockItem(ModBlocks.CRATE_T2.get(), new Item.Properties()));
        public static final RegistryObject<Item> CRATE_T3_ITEM = ITEMS.register("crate_t3",
                () -> new BlockItem(ModBlocks.CRATE_T3.get(), new Item.Properties()));

        public static final RegistryObject<Item> BARREL_T1_ITEM = ITEMS.register("barrel_t1",
                () -> new com.jykito.industrialcore.item.custom.BarrelBlockItem(ModBlocks.BARREL_T1.get(), new Item.Properties()));
        public static final RegistryObject<Item> BARREL_T2_ITEM = ITEMS.register("barrel_t2",
                () -> new com.jykito.industrialcore.item.custom.BarrelBlockItem(ModBlocks.BARREL_T2.get(), new Item.Properties()));
        public static final RegistryObject<Item> BARREL_T3_ITEM = ITEMS.register("barrel_t3",
                () -> new com.jykito.industrialcore.item.custom.BarrelBlockItem(ModBlocks.BARREL_T3.get(), new Item.Properties()));

        public static final RegistryObject<Item> TOOL_BASE = ITEMS.register("tool_base",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> IRON_DRILL_TIP = ITEMS.register("iron_drill_tip",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> NETHERITE_DRILL_TIP = ITEMS.register("netherite_drill_tip",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> ELECTRIC_WRENCH = ITEMS.register("electric_wrench",
                () -> new com.jykito.industrialcore.item.custom.ElectricWrenchItem(
                        new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> ELECTRIC_DRILL = ITEMS.register("electric_drill",
                () -> new com.jykito.industrialcore.item.custom.ElectricDrillItem(new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> INDUSTRIAL_DRILL = ITEMS.register("industrial_drill",
                () -> new com.jykito.industrialcore.item.custom.IndustrialDrillItem(new Item.Properties().stacksTo(1)));

        // weapons: the energy blade (pairs with Nexite armor) and the gauss rifle (pairs with Plasma armor)
        public static final RegistryObject<Item> ENERGY_BLADE = ITEMS.register("energy_blade",
                () -> new com.jykito.industrialcore.item.custom.weapon.EnergyBladeItem(new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> GAUSS_RIFLE = ITEMS.register("gauss_rifle",
                () -> new com.jykito.industrialcore.item.custom.weapon.GaussRifleItem(new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> CUTTERS = ITEMS.register("cutters",
                () -> new com.jykito.industrialcore.item.custom.CraftingToolItem(new Item.Properties().durability(128)));

        public static final RegistryObject<Item> HAMMER = ITEMS.register("hammer",
                () -> new com.jykito.industrialcore.item.custom.CraftingToolItem(new Item.Properties().durability(128)));

        public static final RegistryObject<Item> BARE_COPPER_WIRE = ITEMS.register("bare_copper_wire",
                () -> new com.jykito.industrialcore.item.custom.TooltipItem(new Item.Properties(), "tooltip.industrial_core.crafting_ingredient"));

        public static final RegistryObject<Item> BARE_FERRO_WIRE = ITEMS.register("bare_ferro_wire",
                () -> new com.jykito.industrialcore.item.custom.TooltipItem(new Item.Properties(), "tooltip.industrial_core.crafting_ingredient"));

        public static final RegistryObject<Item> BARE_GOLD_WIRE = ITEMS.register("bare_gold_wire",
                () -> new com.jykito.industrialcore.item.custom.TooltipItem(new Item.Properties(), "tooltip.industrial_core.crafting_ingredient"));

        public static final RegistryObject<Item> BARE_CRYSTALLINE_WIRE = ITEMS.register("bare_crystalline_wire",
                () -> new com.jykito.industrialcore.item.custom.TooltipItem(new Item.Properties(), "tooltip.industrial_core.crafting_ingredient"));

        public static final RegistryObject<Item> BARE_EMERALD_WIRE = ITEMS.register("bare_emerald_wire",
                () -> new com.jykito.industrialcore.item.custom.TooltipItem(new Item.Properties(), "tooltip.industrial_core.crafting_ingredient"));

        public static final RegistryObject<Item> BARE_HEAVY_WIRE = ITEMS.register("bare_heavy_wire",
                () -> new com.jykito.industrialcore.item.custom.TooltipItem(new Item.Properties(), "tooltip.industrial_core.crafting_ingredient"));

        public static final RegistryObject<Item> BARE_NETHERITE_WIRE = ITEMS.register("bare_netherite_wire",
                () -> new com.jykito.industrialcore.item.custom.TooltipItem(new Item.Properties(), "tooltip.industrial_core.crafting_ingredient"));

        public static final RegistryObject<Item> RUBBER = ITEMS.register("rubber",
                () -> new Item(new Item.Properties()));

        // E1 (2026-10-08): the solenoid is the basic electrical part (generator, crusher, electric furnace, motor),
        // solder is a tin and lead alloy (motor, later the logic board)
        public static final RegistryObject<Item> COPPER_SOLENOID = ITEMS.register("copper_solenoid",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> SOLDER = ITEMS.register("solder",
                () -> new Item(new Item.Properties()));

        // E2 (2026-10-08): glass fiber is the base of textolite (the former draws it from glass),
        // the transistor is the first electronics (logic board, hub upgrade T2)
        public static final RegistryObject<Item> GLASS_FIBER = ITEMS.register("glass_fiber",
                () -> new Item(new Item.Properties()));

        // E3: bioreactor input, the compressor presses it from greenery
        public static final RegistryObject<Item> BIOMASS = ITEMS.register("biomass",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> TRANSISTOR = ITEMS.register("transistor",
                () -> new Item(new Item.Properties()));

        // E5 (2026-10-08): the superconductor ladder to the era key: wire (cryo chamber, stage 1) -> strand (rod
        // fabricator) -> solenoid (on kronite and cryo crystals, ID superconducting_winding) -> superconducting coil
        public static final RegistryObject<Item> SUPERCONDUCTING_WIRE = ITEMS.register("superconducting_wire",
                () -> new com.jykito.industrialcore.item.custom.TooltipItem(new Item.Properties(), "tooltip.industrial_core.crafting_ingredient"));
        public static final RegistryObject<Item> SUPERCONDUCTING_STRAND = ITEMS.register("superconducting_strand",
                () -> new com.jykito.industrialcore.item.custom.TooltipItem(new Item.Properties(), "tooltip.industrial_core.crafting_ingredient"));
        public static final RegistryObject<Item> SUPERCONDUCTING_WINDING = ITEMS.register("superconducting_winding",
                () -> new Item(new Item.Properties()));

        // E6 (2026-10-08): condensate ampoule: the magnet turns cryo crystals into the superfluid phase, the liquefier opens it into 1000 mB
        public static final RegistryObject<Item> CONDENSATE_AMPOULE = ITEMS.register("condensate_ampoule",
                () -> new Item(new Item.Properties()));

        // E7 (2026-10-08): the late hardware ladder (armor sheet, servo drive, control unit), clusters from bulk materials,
        // Voltex alloy and the resonance matrix: components of the Heart of Voltex
        public static final RegistryObject<Item> ARMOR_SHEET = ITEMS.register("armor_sheet",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> SERVO_DRIVE = ITEMS.register("servo_drive",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> CONTROL_UNIT = ITEMS.register("control_unit",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> IRON_CLUSTER = ITEMS.register("iron_cluster",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> CRYSTAL_CLUSTER = ITEMS.register("crystal_cluster",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> EMERALD_CLUSTER = ITEMS.register("emerald_cluster",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> VOLTEX_INGOT = ITEMS.register("voltex_ingot",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> VOLTEX_PLATE = ITEMS.register("voltex_plate",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> RESONANCE_MATRIX = ITEMS.register("resonance_matrix",
                () -> new Item(new Item.Properties()));
        /** The mod's finale: a trophy, gives no power and no resources. */
        public static final RegistryObject<Item> HEART_OF_VOLTEX = ITEMS.register("heart_of_voltex",
                () -> new com.jykito.industrialcore.item.custom.TooltipItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC), "tooltip.industrial_core.heart_of_voltex") {
                    @Override public boolean isFoil(net.minecraft.world.item.ItemStack stack) { return true; }
                });

        public static final RegistryObject<Item> HARDENED_STEEL_INGOT = ITEMS.register("hardened_steel_ingot",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> UNIVERSAL_CAPSULE = ITEMS.register("capsule",
                () -> new UniversalCapsuleItem(new Item.Properties()));

        public static final RegistryObject<Item> RAW_TIN_BLOCK_ITEM = ITEMS.register("raw_tin_block",
                () -> new BlockItem(ModBlocks.RAW_TIN_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> RAW_LEAD_BLOCK_ITEM = ITEMS.register("raw_lead_block",
                () -> new BlockItem(ModBlocks.RAW_LEAD_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> RAW_ALUMINUM_BLOCK_ITEM = ITEMS.register("raw_aluminum_block",
                () -> new BlockItem(ModBlocks.RAW_ALUMINUM_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> INVAR_BLOCK_ITEM = ITEMS.register("invar_block",
                () -> new BlockItem(ModBlocks.INVAR_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> ELECTRUM_BLOCK_ITEM = ITEMS.register("electrum_block",
                () -> new BlockItem(ModBlocks.ELECTRUM_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> MECHANISM_BLOCK_ITEM = ITEMS.register("mechanism_block",
                () -> new BlockItem(ModBlocks.MECHANISM_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> MECHANISM_PARALLEL_1_ITEM = ITEMS.register("mechanism_parallel_1",
                () -> new BlockItem(ModBlocks.MECHANISM_PARALLEL_1.get(), new Item.Properties()));
        public static final RegistryObject<Item> MECHANISM_PARALLEL_2_ITEM = ITEMS.register("mechanism_parallel_2",
                () -> new BlockItem(ModBlocks.MECHANISM_PARALLEL_2.get(), new Item.Properties()));
        public static final RegistryObject<Item> MECHANISM_PARALLEL_3_ITEM = ITEMS.register("mechanism_parallel_3",
                () -> new BlockItem(ModBlocks.MECHANISM_PARALLEL_3.get(), new Item.Properties()));
        public static final RegistryObject<Item> MECHANISM_SPEED_ITEM = ITEMS.register("mechanism_speed",
                () -> new BlockItem(ModBlocks.MECHANISM_SPEED.get(), new Item.Properties()));
        public static final RegistryObject<Item> MECHANISM_EFFICIENCY_ITEM = ITEMS.register("mechanism_efficiency",
                () -> new BlockItem(ModBlocks.MECHANISM_EFFICIENCY.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_CASING_ITEM = ITEMS.register("multiblock_casing",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_CASING.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_CASING_ADVANCED_ITEM = ITEMS.register("multiblock_casing_advanced",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_CASING_ADVANCED.get(), new Item.Properties()));
        public static final RegistryObject<Item> STRUCTURAL_GLASS_ITEM = ITEMS.register("structural_glass",
                () -> new BlockItem(ModBlocks.STRUCTURAL_GLASS.get(), new Item.Properties()));
        public static final RegistryObject<Item> STRUCTURAL_GLASS_ADVANCED_ITEM = ITEMS.register("structural_glass_advanced",
                () -> new BlockItem(ModBlocks.STRUCTURAL_GLASS_ADVANCED.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_MACERATOR_ITEM = ITEMS.register("multiblock_macerator",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_MACERATOR.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_FURNACE_ITEM = ITEMS.register("multiblock_furnace",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_FURNACE.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_COMPRESSOR_ITEM = ITEMS.register("multiblock_compressor",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_COMPRESSOR.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_ROLLING_MILL_ITEM = ITEMS.register("multiblock_rolling_mill",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_ROLLING_MILL.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_LIQUEFIER_ITEM = ITEMS.register("multiblock_liquefier",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_LIQUEFIER.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_BLAST_FURNACE_ITEM = ITEMS.register("multiblock_blast_furnace",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_BLAST_FURNACE.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_FLUID_ENRICHER_ITEM = ITEMS.register("multiblock_fluid_enricher",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_FLUID_ENRICHER.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_MAGNET_ITEM = ITEMS.register("multiblock_magnet",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_MAGNET.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_WASHER_ITEM = ITEMS.register("multiblock_washer",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_WASHER.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_CRAFTER_ITEM = ITEMS.register("multiblock_crafter",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_CRAFTER.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_INPUT_PORT_ITEM = ITEMS.register("multiblock_input_port",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_INPUT_PORT.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_OUTPUT_PORT_ITEM = ITEMS.register("multiblock_output_port",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_OUTPUT_PORT.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_ENERGY_PORT_ITEM = ITEMS.register("multiblock_energy_port",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_ENERGY_PORT.get(), new Item.Properties()));
        public static final RegistryObject<Item> MULTIBLOCK_HEAT_PORT_ITEM = ITEMS.register("multiblock_heat_port",
                () -> new BlockItem(ModBlocks.MULTIBLOCK_HEAT_PORT.get(), new Item.Properties()));
        public static final RegistryObject<Item> ME_INPUT_PORT_ITEM = ITEMS.register("me_input_port",
                () -> new BlockItem(ModBlocks.ME_INPUT_PORT.get(), new Item.Properties()));
        public static final RegistryObject<Item> ME_OUTPUT_PORT_ITEM = ITEMS.register("me_output_port",
                () -> new BlockItem(ModBlocks.ME_OUTPUT_PORT.get(), new Item.Properties()));
        public static final RegistryObject<Item> ME_PATTERN_PORT_ITEM = ITEMS.register("me_pattern_port",
                () -> new BlockItem(ModBlocks.ME_PATTERN_PORT.get(), new Item.Properties()));
        public static final RegistryObject<Item> PORT_UPGRADE_BASIC = ITEMS.register("port_upgrade_basic",
                () -> new PortUpgradeItem(1, new Item.Properties()));
        public static final RegistryObject<Item> PORT_UPGRADE_ADVANCED = ITEMS.register("port_upgrade_advanced",
                () -> new PortUpgradeItem(2, new Item.Properties()));
        public static final RegistryObject<Item> NICKEL_BLOCK_ITEM = ITEMS.register("nickel_block",
                () -> new BlockItem(ModBlocks.NICKEL_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> SILVER_BLOCK_ITEM = ITEMS.register("silver_block",
                () -> new BlockItem(ModBlocks.SILVER_BLOCK.get(), new Item.Properties()));
        public static final RegistryObject<Item> KRONIUM_ALLOY = ITEMS.register("kronium_alloy",
                () -> new Item(new Item.Properties()));

        public static final RegistryObject<Item> EMPTY_FUEL_ROD = ITEMS.register("empty_fuel_rod",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> GRAPHITE_ROD = ITEMS.register("graphite_rod",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> HEAT_VENT = ITEMS.register("heat_vent",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> HEAT_EXCHANGER = ITEMS.register("heat_exchanger",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> COMPONENT_HEAT_VENT = ITEMS.register("component_heat_vent",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> ADVANCED_HEAT_VENT = ITEMS.register("advanced_heat_vent",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> CONDENSATOR = ITEMS.register("condensator",
                () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> ROD_FABRICATOR_ITEM = ITEMS.register("rod_fabricator",
                () -> new BlockItem(ModBlocks.ROD_FABRICATOR.get(), new Item.Properties()));

        public static final RegistryObject<Item> MINER_CASING_ITEM = ITEMS.register("miner_casing",
                () -> new BlockItem(ModBlocks.MINER_CASING.get(), new Item.Properties()));
        public static final RegistryObject<Item> MINER_CORE_ITEM = ITEMS.register("miner_core",
                () -> new com.jykito.industrialcore.item.custom.TooltipBlockItem(ModBlocks.MINER_CORE.get(), new Item.Properties(), "tooltip.industrial_core.miner_core"));
        public static final RegistryObject<Item> INDUSTRIAL_MINER_FRAME_ITEM = ITEMS.register("industrial_miner_frame",
                () -> new BlockItem(ModBlocks.INDUSTRIAL_MINER_FRAME.get(), new Item.Properties()));
        public static final RegistryObject<Item> INDUSTRIAL_MINER_CORE_ITEM = ITEMS.register("industrial_miner_core",
                () -> new com.jykito.industrialcore.item.custom.TooltipBlockItem(ModBlocks.INDUSTRIAL_MINER_CORE.get(), new Item.Properties(), "tooltip.industrial_core.industrial_miner_core"));
        public static final RegistryObject<Item> DRILL_BEACON_ITEM = ITEMS.register("drill_beacon",
                () -> new com.jykito.industrialcore.item.custom.DrillBeaconItem(ModBlocks.DRILL_BEACON.get(), new Item.Properties(), "tooltip.industrial_core.drill_beacon"));

        public static final RegistryObject<Item> MINER_LENS_1 = ITEMS.register("miner_lens_1",
                () -> new com.jykito.industrialcore.item.custom.MinerLensItem(
                        com.jykito.industrialcore.item.custom.MinerLensItem.LensType.OVERWORLD, new Item.Properties()));
        public static final RegistryObject<Item> MINER_LENS_2 = ITEMS.register("miner_lens_2",
                () -> new com.jykito.industrialcore.item.custom.MinerLensItem(
                        com.jykito.industrialcore.item.custom.MinerLensItem.LensType.NETHER, new Item.Properties()));
        public static final RegistryObject<Item> MINER_LENS_3 = ITEMS.register("miner_lens_3",
                () -> new com.jykito.industrialcore.item.custom.MinerLensItem(
                        com.jykito.industrialcore.item.custom.MinerLensItem.LensType.END, new Item.Properties()));
        public static final RegistryObject<Item> MINER_LENS_4 = ITEMS.register("miner_lens_4",
                () -> new com.jykito.industrialcore.item.custom.MinerLensItem(
                        com.jykito.industrialcore.item.custom.MinerLensItem.LensType.ALL, new Item.Properties()));

        // --- Nanites (2026-10-07): a strain is bred in the evolution chamber and goes into the industrial miner ---
        public static final RegistryObject<Item> NANITE_STRAIN = ITEMS.register("nanite_strain",
                () -> new com.jykito.industrialcore.item.custom.NaniteStrainItem(new Item.Properties().stacksTo(1)));

        // Scientist data cards: a blank one and one per topic (topics are NaniteLab.THRESHOLDS). Explicitly, not in a loop:
        // that way completeness checks (tools/checks) and code search can see them.
        public static final RegistryObject<Item> DATA_CARD_BLANK = dataCard("data_card_blank", null);
        public static final RegistryObject<Item> DATA_CARD_CRYSTALLOGRAPHY = dataCard("data_card_crystallography", "crystallography");
        public static final RegistryObject<Item> DATA_CARD_CRYOPHYSICS = dataCard("data_card_cryophysics", "cryophysics");
        public static final RegistryObject<Item> DATA_CARD_MATTER = dataCard("data_card_matter", "matter");
        public static final RegistryObject<Item> DATA_CARD_STELLAR = dataCard("data_card_stellar", "stellar");
        /** Written cards in topic order: the source for the tab and colouring. */
        public static final java.util.List<RegistryObject<Item>> DATA_CARDS = java.util.List.of(
                DATA_CARD_CRYSTALLOGRAPHY, DATA_CARD_CRYOPHYSICS, DATA_CARD_MATTER, DATA_CARD_STELLAR);

        private static RegistryObject<Item> dataCard(String id, @Nullable String theme) {
            return ITEMS.register(id, () -> new com.jykito.industrialcore.item.custom.DataCardItem(theme, new Item.Properties()));
        }

        public static final RegistryObject<Item> ITEM_PUSHER = ITEMS.register("item_pusher",
                () -> new DirectionalUpgradeItem(DirectionalUpgradeItem.Mode.ITEM_PUSH, new Item.Properties()));

        public static final RegistryObject<Item> ITEM_PULLER = ITEMS.register("item_puller",
                () -> new DirectionalUpgradeItem(DirectionalUpgradeItem.Mode.ITEM_PULL, new Item.Properties()));

        public static final RegistryObject<Item> FLUID_PUSHER = ITEMS.register("fluid_pusher",
                () -> new DirectionalUpgradeItem(DirectionalUpgradeItem.Mode.FLUID_PUSH, new Item.Properties()));

        public static final RegistryObject<Item> FLUID_PULLER = ITEMS.register("fluid_puller",
                () -> new DirectionalUpgradeItem(DirectionalUpgradeItem.Mode.FLUID_PULL, new Item.Properties()));

        public static final RegistryObject<Item> ACCELERATOR = ITEMS.register("accelerator",
                () -> new AcceleratorItem(0.70f, 1.30f, new Item.Properties()));

        public static final RegistryObject<Item> ACCELERATOR_MK1 = ITEMS.register("accelerator_mk1",
                () -> new AcceleratorItem(0.50f, 1.50f, new Item.Properties()));

        public static final RegistryObject<Item> ACCELERATOR_MK2 = ITEMS.register("accelerator_mk2",
                () -> new AcceleratorItem(0.25f, 2.00f, new Item.Properties()));

        public static final RegistryObject<Item> PARALLEL_MODULE = ITEMS.register("parallel_module",
                () -> new ParallelModuleItem(8, new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> ADVANCED_PARALLEL_MODULE = ITEMS.register("advanced_parallel_module",
                () -> new AdvancedParallelModuleItem(32, new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> QUANTUM_PARALLEL_MODULE = ITEMS.register("quantum_parallel_module",
                () -> new AdvancedParallelModuleItem(64, new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> BLAST_UPGRADE_FORCED_AIR = ITEMS.register("blast_upgrade_forced_air",
                () -> new BlastUpgradeItem(BlastUpgradeItem.Type.FORCED_AIR, new Item.Properties()));
        public static final RegistryObject<Item> BLAST_UPGRADE_OXYGEN_BOOST = ITEMS.register("blast_upgrade_oxygen_boost",
                () -> new BlastUpgradeItem(BlastUpgradeItem.Type.OXYGEN_BOOST, new Item.Properties()));

        public static final RegistryObject<Item> ENERGY_BACKPACK_T1 = ITEMS.register("energy_backpack_t1",
                () -> new EnergyBackpackItem(1_000_000, 5_000));
        public static final RegistryObject<Item> ENERGY_BACKPACK_T2 = ITEMS.register("energy_backpack_t2",
                () -> new EnergyBackpackItem(10_000_000, 20_000));
        public static final RegistryObject<Item> ENERGY_BACKPACK_T3 = ITEMS.register("energy_backpack_t3",
                () -> new EnergyBackpackItem(100_000_000, 100_000));

        public static final RegistryObject<Item> PLASMA_HELMET = ITEMS.register("plasma_helmet",
                () -> new com.jykito.industrialcore.item.custom.PlasmaArmorItem(
                        ModArmorMaterials.PLASMA, net.minecraft.world.item.ArmorItem.Type.HELMET,
                        new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> PLASMA_CHESTPLATE = ITEMS.register("plasma_chestplate",
                () -> new com.jykito.industrialcore.item.custom.PlasmaArmorItem(
                        ModArmorMaterials.PLASMA, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                        new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> PLASMA_LEGGINGS = ITEMS.register("plasma_leggings",
                () -> new com.jykito.industrialcore.item.custom.PlasmaArmorItem(
                        ModArmorMaterials.PLASMA, net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
                        new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> PLASMA_BOOTS = ITEMS.register("plasma_boots",
                () -> new com.jykito.industrialcore.item.custom.PlasmaArmorItem(
                        ModArmorMaterials.PLASMA, net.minecraft.world.item.ArmorItem.Type.BOOTS,
                        new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> NEXITE_HELMET = ITEMS.register("nexite_helmet",
                () -> new com.jykito.industrialcore.item.custom.NexiteArmorItem(
                        ModArmorMaterials.NEXITE, net.minecraft.world.item.ArmorItem.Type.HELMET,
                        new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> NEXITE_CHESTPLATE = ITEMS.register("nexite_chestplate",
                () -> new com.jykito.industrialcore.item.custom.NexiteArmorItem(
                        ModArmorMaterials.NEXITE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                        new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> NEXITE_LEGGINGS = ITEMS.register("nexite_leggings",
                () -> new com.jykito.industrialcore.item.custom.NexiteArmorItem(
                        ModArmorMaterials.NEXITE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
                        new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> NEXITE_BOOTS = ITEMS.register("nexite_boots",
                () -> new com.jykito.industrialcore.item.custom.NexiteArmorItem(
                        ModArmorMaterials.NEXITE, net.minecraft.world.item.ArmorItem.Type.BOOTS,
                        new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> HUB_UPGRADE_T1 = ITEMS.register("hub_upgrade_t1",
                () -> new com.jykito.industrialcore.item.upgrade.HubUpgradeItem(10, 16, 1000, new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> HUB_UPGRADE_T2 = ITEMS.register("hub_upgrade_t2",
                () -> new com.jykito.industrialcore.item.upgrade.HubUpgradeItem(5, 32, 2000, new Item.Properties().stacksTo(1)));

        public static final RegistryObject<Item> HUB_UPGRADE_T3 = ITEMS.register("hub_upgrade_t3",
                () -> new com.jykito.industrialcore.item.upgrade.HubUpgradeItem(1, 64, 10000, new Item.Properties().stacksTo(1)));

        public static void register(IEventBus eventBus) {
            ITEMS.register(eventBus);
        }
    }
