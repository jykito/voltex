package com.jykito.industrialcore.menu;


import net.minecraft.core.registries.Registries;
import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, IndustrialCore.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CoalGeneratorMenu>> COAL_GENERATOR_MENU =
            MENUS.register("coal_generator_menu", () -> IMenuTypeExtension.create(CoalGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ItemFluidPortMenu>> ITEM_FLUID_PORT_MENU =
            MENUS.register("item_fluid_port_menu", () -> IMenuTypeExtension.create(ItemFluidPortMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MePortMenu>> ME_PORT_MENU =
            MENUS.register("me_port_menu", () -> IMenuTypeExtension.create(MePortMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyPortMenu>> ENERGY_PORT_MENU =
            MENUS.register("energy_port_menu", () -> IMenuTypeExtension.create(EnergyPortMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<IndustrialMinerMenu>> INDUSTRIAL_MINER_MENU =
            MENUS.register("industrial_miner_menu", () -> IMenuTypeExtension.create(IndustrialMinerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<DrillBeaconMenu>> DRILL_BEACON_MENU =
            MENUS.register("drill_beacon_menu", () -> IMenuTypeExtension.create(DrillBeaconMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<LateMachineCoreMenu>> LATE_MACHINE_CORE_MENU =
            MENUS.register("late_machine_core_menu", () -> IMenuTypeExtension.create(LateMachineCoreMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CrusherMenu>> CRUSHER_MENU =
            MENUS.register("crusher_menu", () -> IMenuTypeExtension.create(CrusherMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE_MENU =
            MENUS.register("electric_furnace_menu", () -> IMenuTypeExtension.create(ElectricFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CompressorMenu>> COMPRESSOR_MENU =
            MENUS.register("compressor_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(CompressorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MetalFormerMenu>> ROLLING_MENU =
            MENUS.register("rolling_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(MetalFormerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BlastFurnaceMenu>> BLAST_FURNACE_MENU =
            MENUS.register("blast_furnace_menu", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(BlastFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ThermogeneratorMenu>> THERMOGENERATOR_MENU =
            MENUS.register("thermogenerator_menu", () -> IMenuTypeExtension.create(ThermogeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BatteryChargerMenu>> BATTERY_CHARGER_MENU =
            MENUS.register("battery_charger_menu",
                    () -> IMenuTypeExtension.create(BatteryChargerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidEnricherMenu>> FLUID_ENRICHER_MENU =
            MENUS.register("fluid_enricher_menu", () -> IMenuTypeExtension.create(FluidEnricherMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<LiquefierMenu>> LIQUEFIER_MENU =
            MENUS.register("liquefier_menu", () -> IMenuTypeExtension.create(LiquefierMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CryoChamberMenu>> CRYO_CHAMBER_MENU =
            MENUS.register("cryo_chamber_menu", () -> IMenuTypeExtension.create(CryoChamberMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CrystallizerMenu>> CRYSTALLIZER_MENU =
            MENUS.register("crystallizer_menu", () -> IMenuTypeExtension.create(CrystallizerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<EvolutionChamberMenu>> EVOLUTION_CHAMBER_MENU =
            MENUS.register("evolution_chamber_menu", () -> IMenuTypeExtension.create(EvolutionChamberMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<NaniteLabMenu>> NANITE_LAB_MENU =
            MENUS.register("nanite_lab_menu", () -> IMenuTypeExtension.create(NaniteLabMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidGeneratorMenu>> FLUID_GENERATOR_MENU =
            MENUS.register("fluid_generator_menu", () -> IMenuTypeExtension.create(FluidGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StoneGeneratorMenu>> STONE_GENERATOR_MENU =
            MENUS.register("stone_generator_menu", () -> IMenuTypeExtension.create(StoneGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CapsuleFillerMenu>> CAPSULE_FILLER_MENU =
            MENUS.register("capsule_filler_menu", () -> IMenuTypeExtension.create(CapsuleFillerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CultivatorMenu>> CULTIVATOR_MENU =
            MENUS.register("cultivator_menu", () -> IMenuTypeExtension.create(CultivatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CultivatorMenu>> CULTIVATOR_T2_MENU =
            MENUS.register("cultivator_t2_menu", () -> IMenuTypeExtension.create(CultivatorMenu::t2));

    public static final DeferredHolder<MenuType<?>, MenuType<BioreactorMenu>> BIOREACTOR_MENU =
            MENUS.register("bioreactor_menu", () -> IMenuTypeExtension.create(BioreactorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<OreWasherMenu>> ORE_WASHER_MENU =
            MENUS.register("ore_washer_menu", () -> IMenuTypeExtension.create(OreWasherMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RecipeExporterMenu>> RECIPE_EXPORTER_MENU =
            MENUS.register("recipe_exporter_menu", () -> IMenuTypeExtension.create(RecipeExporterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterFabricatorMenu>> MATTER_FABRICATOR_MENU =
            MENUS.register("matter_fabricator_menu", () -> IMenuTypeExtension.create(MatterFabricatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RodFabricatorMenu>> ROD_FABRICATOR_MENU =
            MENUS.register("rod_fabricator_menu", () -> IMenuTypeExtension.create(RodFabricatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ExtractorMenu>> EXTRACTOR_MENU =
            MENUS.register("extractor_menu", () -> IMenuTypeExtension.create(ExtractorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PhaseSplitterMenu>> PHASE_SPLITTER_MENU =
            MENUS.register("phase_splitter_menu", () -> IMenuTypeExtension.create(PhaseSplitterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RestructorMenu>> RESTRUCTOR_MENU =
            MENUS.register("restructor_menu", () -> IMenuTypeExtension.create(RestructorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RestructorMenu>> RESTRUCTOR_T2_MENU =
            MENUS.register("restructor_t2_menu", () -> IMenuTypeExtension.create(RestructorMenu::t2));

    public static final DeferredHolder<MenuType<?>, MenuType<MinerCoreMenu>> MINER_CORE_MENU =
            MENUS.register("miner_core_menu", () -> IMenuTypeExtension.create(MinerCoreMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<LogisticsNodeMenu>> LOGISTICS_NODE_MENU =
            MENUS.register("logistics_node_menu", () -> IMenuTypeExtension.create(LogisticsNodeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<HubMenu>> HUB_MENU =
            MENUS.register("hub_menu", () -> IMenuTypeExtension.create(HubMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SolarPanelMenu>> SOLAR_PANEL_MENU =
            MENUS.register("solar_panel_menu", () -> IMenuTypeExtension.create(SolarPanelMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PanelCombinerMenu>> PANEL_COMBINER_MENU =
            MENUS.register("panel_combiner_menu", () -> IMenuTypeExtension.create(PanelCombinerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ResinCollectorMenu>> RESIN_COLLECTOR_MENU =
            MENUS.register("resin_collector_menu", () -> IMenuTypeExtension.create(ResinCollectorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CrateMenu>> CRATE_MENU =
            MENUS.register("crate_menu", () -> IMenuTypeExtension.create(CrateMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectromagnetMenu>> ELECTROMAGNET_MENU =
            MENUS.register("electromagnet_menu", () -> IMenuTypeExtension.create(ElectromagnetMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ThermostationMenu>> THERMOSTATION_MENU =
            MENUS.register("thermostation_menu", () -> IMenuTypeExtension.create(ThermostationMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyBankMenu>> ENERGY_BANK_MENU =
            MENUS.register("energy_bank_menu", () -> IMenuTypeExtension.create(EnergyBankMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
