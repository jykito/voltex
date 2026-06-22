package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, IndustrialCore.MODID);

    public static final RegistryObject<MenuType<CoalGeneratorMenu>> COAL_GENERATOR_MENU =
            MENUS.register("coal_generator_menu", () -> IForgeMenuType.create(CoalGeneratorMenu::new));

    public static final RegistryObject<MenuType<CrusherMenu>> CRUSHER_MENU =
            MENUS.register("crusher_menu", () -> IForgeMenuType.create(CrusherMenu::new));

    public static final RegistryObject<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE_MENU =
            MENUS.register("electric_furnace_menu", () -> IForgeMenuType.create(ElectricFurnaceMenu::new));

    public static final RegistryObject<MenuType<CompressorMenu>> COMPRESSOR_MENU =
            MENUS.register("compressor_menu", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(CompressorMenu::new));

    public static final RegistryObject<MenuType<MetalFormerMenu>> ROLLING_MENU =
            MENUS.register("rolling_menu", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(MetalFormerMenu::new));

    public static final RegistryObject<MenuType<BlastFurnaceMenu>> BLAST_FURNACE_MENU =
            MENUS.register("blast_furnace_menu", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(BlastFurnaceMenu::new));

    public static final RegistryObject<MenuType<ThermogeneratorMenu>> THERMOGENERATOR_MENU =
            MENUS.register("thermogenerator_menu", () -> IForgeMenuType.create(ThermogeneratorMenu::new));

    public static final RegistryObject<MenuType<BatteryChargerMenu>> BATTERY_CHARGER_MENU =
            MENUS.register("battery_charger_menu",
                    () -> IForgeMenuType.create(BatteryChargerMenu::new));

    public static final RegistryObject<MenuType<FluidEnricherMenu>> FLUID_ENRICHER_MENU =
            MENUS.register("fluid_enricher_menu", () -> IForgeMenuType.create(FluidEnricherMenu::new));

    public static final RegistryObject<MenuType<LiquefierMenu>> LIQUEFIER_MENU =
            MENUS.register("liquefier_menu", () -> IForgeMenuType.create(LiquefierMenu::new));

    public static final RegistryObject<MenuType<FluidGeneratorMenu>> FLUID_GENERATOR_MENU =
            MENUS.register("fluid_generator_menu", () -> IForgeMenuType.create(FluidGeneratorMenu::new));

    public static final RegistryObject<MenuType<StoneGeneratorMenu>> STONE_GENERATOR_MENU =
            MENUS.register("stone_generator_menu", () -> IForgeMenuType.create(StoneGeneratorMenu::new));

    public static final RegistryObject<MenuType<CapsuleFillerMenu>> CAPSULE_FILLER_MENU =
            MENUS.register("capsule_filler_menu", () -> IForgeMenuType.create(CapsuleFillerMenu::new));

    public static final RegistryObject<MenuType<CultivatorMenu>> CULTIVATOR_MENU =
            MENUS.register("cultivator_menu", () -> IForgeMenuType.create(CultivatorMenu::new));

    public static final RegistryObject<MenuType<OreWasherMenu>> ORE_WASHER_MENU =
            MENUS.register("ore_washer_menu", () -> IForgeMenuType.create(OreWasherMenu::new));

    public static final RegistryObject<MenuType<RecipeExporterMenu>> RECIPE_EXPORTER_MENU =
            MENUS.register("recipe_exporter_menu", () -> IForgeMenuType.create(RecipeExporterMenu::new));

    public static final RegistryObject<MenuType<MatterFabricatorMenu>> MATTER_FABRICATOR_MENU =
            MENUS.register("matter_fabricator_menu", () -> IForgeMenuType.create(MatterFabricatorMenu::new));

    public static final RegistryObject<MenuType<NuclearReactorMenu>> NUCLEAR_REACTOR_MENU =
            MENUS.register("nuclear_reactor_menu", () -> IForgeMenuType.create(NuclearReactorMenu::new));

    public static final RegistryObject<MenuType<RodFabricatorMenu>> ROD_FABRICATOR_MENU =
            MENUS.register("rod_fabricator_menu", () -> IForgeMenuType.create(RodFabricatorMenu::new));

    public static final RegistryObject<MenuType<ExtractorMenu>> EXTRACTOR_MENU =
            MENUS.register("extractor_menu", () -> IForgeMenuType.create(ExtractorMenu::new));

    public static final RegistryObject<MenuType<PhaseSplitterMenu>> PHASE_SPLITTER_MENU =
            MENUS.register("phase_splitter_menu", () -> IForgeMenuType.create(PhaseSplitterMenu::new));

    public static final RegistryObject<MenuType<RestructorMenu>> RESTRUCTOR_MENU =
            MENUS.register("restructor_menu", () -> IForgeMenuType.create(RestructorMenu::new));

    public static final RegistryObject<MenuType<MinerCoreMenu>> MINER_CORE_MENU =
            MENUS.register("miner_core_menu", () -> IForgeMenuType.create(MinerCoreMenu::new));

    public static final RegistryObject<MenuType<ItemConnectorMenu>> ITEM_CONNECTOR_MENU =
            MENUS.register("item_connector_menu", () -> IForgeMenuType.create(ItemConnectorMenu::new));

    public static final RegistryObject<MenuType<FluidConnectorMenu>> FLUID_CONNECTOR_MENU =
            MENUS.register("fluid_connector_menu", () -> IForgeMenuType.create(FluidConnectorMenu::new));

    public static final RegistryObject<MenuType<LogisticsNodeMenu>> LOGISTICS_NODE_MENU =
            MENUS.register("logistics_node_menu", () -> IForgeMenuType.create(LogisticsNodeMenu::new));

    public static final RegistryObject<MenuType<SolarPanelMenu>> SOLAR_PANEL_MENU =
            MENUS.register("solar_panel_menu", () -> IForgeMenuType.create(SolarPanelMenu::new));

    public static final RegistryObject<MenuType<PanelCombinerMenu>> PANEL_COMBINER_MENU =
            MENUS.register("panel_combiner_menu", () -> IForgeMenuType.create(PanelCombinerMenu::new));

    public static final RegistryObject<MenuType<ResinCollectorMenu>> RESIN_COLLECTOR_MENU =
            MENUS.register("resin_collector_menu", () -> IForgeMenuType.create(ResinCollectorMenu::new));

    public static final RegistryObject<MenuType<CrateMenu>> CRATE_MENU =
            MENUS.register("crate_menu", () -> IForgeMenuType.create(CrateMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
