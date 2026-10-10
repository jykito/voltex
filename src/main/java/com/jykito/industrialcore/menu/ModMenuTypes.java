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

    public static final RegistryObject<MenuType<ItemFluidPortMenu>> ITEM_FLUID_PORT_MENU =
            MENUS.register("item_fluid_port_menu", () -> IForgeMenuType.create(ItemFluidPortMenu::new));

    public static final RegistryObject<MenuType<MePortMenu>> ME_PORT_MENU =
            MENUS.register("me_port_menu", () -> IForgeMenuType.create(MePortMenu::new));

    public static final RegistryObject<MenuType<EnergyPortMenu>> ENERGY_PORT_MENU =
            MENUS.register("energy_port_menu", () -> IForgeMenuType.create(EnergyPortMenu::new));

    public static final RegistryObject<MenuType<IndustrialMinerMenu>> INDUSTRIAL_MINER_MENU =
            MENUS.register("industrial_miner_menu", () -> IForgeMenuType.create(IndustrialMinerMenu::new));
    public static final RegistryObject<MenuType<DrillBeaconMenu>> DRILL_BEACON_MENU =
            MENUS.register("drill_beacon_menu", () -> IForgeMenuType.create(DrillBeaconMenu::new));
    public static final RegistryObject<MenuType<LateMachineCoreMenu>> LATE_MACHINE_CORE_MENU =
            MENUS.register("late_machine_core_menu", () -> IForgeMenuType.create(LateMachineCoreMenu::new));

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

    public static final RegistryObject<MenuType<CryoChamberMenu>> CRYO_CHAMBER_MENU =
            MENUS.register("cryo_chamber_menu", () -> IForgeMenuType.create(CryoChamberMenu::new));

    public static final RegistryObject<MenuType<CrystallizerMenu>> CRYSTALLIZER_MENU =
            MENUS.register("crystallizer_menu", () -> IForgeMenuType.create(CrystallizerMenu::new));

    public static final RegistryObject<MenuType<EvolutionChamberMenu>> EVOLUTION_CHAMBER_MENU =
            MENUS.register("evolution_chamber_menu", () -> IForgeMenuType.create(EvolutionChamberMenu::new));

    public static final RegistryObject<MenuType<NaniteLabMenu>> NANITE_LAB_MENU =
            MENUS.register("nanite_lab_menu", () -> IForgeMenuType.create(NaniteLabMenu::new));

    public static final RegistryObject<MenuType<FluidGeneratorMenu>> FLUID_GENERATOR_MENU =
            MENUS.register("fluid_generator_menu", () -> IForgeMenuType.create(FluidGeneratorMenu::new));

    public static final RegistryObject<MenuType<StoneGeneratorMenu>> STONE_GENERATOR_MENU =
            MENUS.register("stone_generator_menu", () -> IForgeMenuType.create(StoneGeneratorMenu::new));

    public static final RegistryObject<MenuType<CapsuleFillerMenu>> CAPSULE_FILLER_MENU =
            MENUS.register("capsule_filler_menu", () -> IForgeMenuType.create(CapsuleFillerMenu::new));

    public static final RegistryObject<MenuType<CultivatorMenu>> CULTIVATOR_MENU =
            MENUS.register("cultivator_menu", () -> IForgeMenuType.create(CultivatorMenu::new));

    public static final RegistryObject<MenuType<CultivatorMenu>> CULTIVATOR_T2_MENU =
            MENUS.register("cultivator_t2_menu", () -> IForgeMenuType.create(CultivatorMenu::t2));

    public static final RegistryObject<MenuType<BioreactorMenu>> BIOREACTOR_MENU =
            MENUS.register("bioreactor_menu", () -> IForgeMenuType.create(BioreactorMenu::new));

    public static final RegistryObject<MenuType<OreWasherMenu>> ORE_WASHER_MENU =
            MENUS.register("ore_washer_menu", () -> IForgeMenuType.create(OreWasherMenu::new));

    public static final RegistryObject<MenuType<RecipeExporterMenu>> RECIPE_EXPORTER_MENU =
            MENUS.register("recipe_exporter_menu", () -> IForgeMenuType.create(RecipeExporterMenu::new));

    public static final RegistryObject<MenuType<MatterFabricatorMenu>> MATTER_FABRICATOR_MENU =
            MENUS.register("matter_fabricator_menu", () -> IForgeMenuType.create(MatterFabricatorMenu::new));

    public static final RegistryObject<MenuType<RodFabricatorMenu>> ROD_FABRICATOR_MENU =
            MENUS.register("rod_fabricator_menu", () -> IForgeMenuType.create(RodFabricatorMenu::new));

    public static final RegistryObject<MenuType<ExtractorMenu>> EXTRACTOR_MENU =
            MENUS.register("extractor_menu", () -> IForgeMenuType.create(ExtractorMenu::new));

    public static final RegistryObject<MenuType<PhaseSplitterMenu>> PHASE_SPLITTER_MENU =
            MENUS.register("phase_splitter_menu", () -> IForgeMenuType.create(PhaseSplitterMenu::new));

    public static final RegistryObject<MenuType<RestructorMenu>> RESTRUCTOR_MENU =
            MENUS.register("restructor_menu", () -> IForgeMenuType.create(RestructorMenu::new));

    public static final RegistryObject<MenuType<RestructorMenu>> RESTRUCTOR_T2_MENU =
            MENUS.register("restructor_t2_menu", () -> IForgeMenuType.create(RestructorMenu::t2));

    public static final RegistryObject<MenuType<MinerCoreMenu>> MINER_CORE_MENU =
            MENUS.register("miner_core_menu", () -> IForgeMenuType.create(MinerCoreMenu::new));

    public static final RegistryObject<MenuType<LogisticsNodeMenu>> LOGISTICS_NODE_MENU =
            MENUS.register("logistics_node_menu", () -> IForgeMenuType.create(LogisticsNodeMenu::new));

    public static final RegistryObject<MenuType<HubMenu>> HUB_MENU =
            MENUS.register("hub_menu", () -> IForgeMenuType.create(HubMenu::new));

    public static final RegistryObject<MenuType<SolarPanelMenu>> SOLAR_PANEL_MENU =
            MENUS.register("solar_panel_menu", () -> IForgeMenuType.create(SolarPanelMenu::new));

    public static final RegistryObject<MenuType<PanelCombinerMenu>> PANEL_COMBINER_MENU =
            MENUS.register("panel_combiner_menu", () -> IForgeMenuType.create(PanelCombinerMenu::new));

    public static final RegistryObject<MenuType<ResinCollectorMenu>> RESIN_COLLECTOR_MENU =
            MENUS.register("resin_collector_menu", () -> IForgeMenuType.create(ResinCollectorMenu::new));

    public static final RegistryObject<MenuType<CrateMenu>> CRATE_MENU =
            MENUS.register("crate_menu", () -> IForgeMenuType.create(CrateMenu::new));

    public static final RegistryObject<MenuType<ElectromagnetMenu>> ELECTROMAGNET_MENU =
            MENUS.register("electromagnet_menu", () -> IForgeMenuType.create(ElectromagnetMenu::new));

    public static final RegistryObject<MenuType<ThermostationMenu>> THERMOSTATION_MENU =
            MENUS.register("thermostation_menu", () -> IForgeMenuType.create(ThermostationMenu::new));

    public static final RegistryObject<MenuType<EnergyBankMenu>> ENERGY_BANK_MENU =
            MENUS.register("energy_bank_menu", () -> IForgeMenuType.create(EnergyBankMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
