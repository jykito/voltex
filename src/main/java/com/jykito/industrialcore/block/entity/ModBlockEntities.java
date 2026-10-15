package com.jykito.industrialcore.block.entity;


import net.minecraft.core.registries.Registries;
import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IndustrialCore.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR_BE =
            BLOCK_ENTITIES.register("coal_generator_be", () ->
                    BlockEntityType.Builder.of(CoalGeneratorBlockEntity::new, ModBlocks.COAL_GENERATOR.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER_BE =
            BLOCK_ENTITIES.register("crusher_be", () ->
                    BlockEntityType.Builder.of(CrusherBlockEntity::new, ModBlocks.CRUSHER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BE =
            BLOCK_ENTITIES.register("electric_furnace_be", () ->
                    BlockEntityType.Builder.of(ElectricFurnaceBlockEntity::new, ModBlocks.ELECTRIC_FURNACE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressorBlockEntity>> COMPRESSOR_BE =
            BLOCK_ENTITIES.register("compressor_be", () ->
                    BlockEntityType.Builder.of(CompressorBlockEntity::new, ModBlocks.COMPRESSOR.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MetalFormerBlockEntity>> METAL_FORMER_BE =
            BLOCK_ENTITIES.register("metal_former_be", () ->
                    BlockEntityType.Builder.of(MetalFormerBlockEntity::new, ModBlocks.METAL_FORMER.get())
                            .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BaseWireBlockEntity>> WIRE_BE =
            BLOCK_ENTITIES.register("wire_be", () ->
                    BlockEntityType.Builder.of(BaseWireBlockEntity::new,
                            ModBlocks.COPPER_CABLE.get(),
                            ModBlocks.FERRO_CABLE.get(),
                            ModBlocks.GOLD_CONDUCTOR.get(),
                            ModBlocks.CRYSTALLINE_CHANNEL.get(),
                            ModBlocks.EMERALD_FLUX_CABLE.get(),
                            ModBlocks.HEAVY_MAGISTRAL.get(),
                            ModBlocks.NETHERITE_FIBER_CABLE.get()
                    ).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlastFurnaceBlockEntity>> BLAST_FURNACE_BE =
            BLOCK_ENTITIES.register("blast_furnace_be", () ->
                    BlockEntityType.Builder.of(BlastFurnaceBlockEntity::new, ModBlocks.BLAST_FURNACE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermogeneratorBlockEntity>> THERMOGENERATOR_BE =
            BLOCK_ENTITIES.register("thermogenerator_be", () ->
                    BlockEntityType.Builder.of(ThermogeneratorBlockEntity::new, ModBlocks.THERMOGENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryChargerBlockEntity>> BATTERY_CHARGER_BE =
            BLOCK_ENTITIES.register("battery_charger_be",
                    () -> BlockEntityType.Builder.of(BatteryChargerBlockEntity::new,
                            ModBlocks.BATTERY_CHARGER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidEnricherBlockEntity>> FLUID_ENRICHER_BE =
            BLOCK_ENTITIES.register("fluid_enricher_be", () ->
                    BlockEntityType.Builder.of(FluidEnricherBlockEntity::new,
                            ModBlocks.FLUID_ENRICHER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatterFabricatorBlockEntity>> MATTER_FABRICATOR_BE =
            BLOCK_ENTITIES.register("matter_fabricator_be", () ->
                    BlockEntityType.Builder.of(MatterFabricatorBlockEntity::new,
                            ModBlocks.MATTER_FABRICATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RecipeExporterBlockEntity>> RECIPE_EXPORTER_BE =
            BLOCK_ENTITIES.register("recipe_exporter_be", () ->
                    BlockEntityType.Builder.of(RecipeExporterBlockEntity::new,
                            ModBlocks.RECIPE_EXPORTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BioreactorBlockEntity>> BIOREACTOR_BE =
            BLOCK_ENTITIES.register("bioreactor_be", () ->
                    BlockEntityType.Builder.of(BioreactorBlockEntity::new,
                            ModBlocks.BIOREACTOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OreWasherBlockEntity>> ORE_WASHER_BE =
            BLOCK_ENTITIES.register("ore_washer_be", () ->
                    BlockEntityType.Builder.of(OreWasherBlockEntity::new,
                            ModBlocks.ORE_WASHER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.jykito.industrialcore.block.entity.LiquefierBlockEntity>> LIQUEFIER_BE =
            BLOCK_ENTITIES.register("liquefier_be", () ->
                    BlockEntityType.Builder.of(com.jykito.industrialcore.block.entity.LiquefierBlockEntity::new,
                            ModBlocks.LIQUEFIER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CryoCasingBlockEntity>> CRYO_CASING_BE =
            BLOCK_ENTITIES.register("cryo_casing_be", () ->
                    BlockEntityType.Builder.of(CryoCasingBlockEntity::new,
                            ModBlocks.CRYO_CASING.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystallizerBlockEntity>> CRYSTALLIZER_BE =
            BLOCK_ENTITIES.register("crystallizer_be", () ->
                    BlockEntityType.Builder.of(CrystallizerBlockEntity::new,
                            ModBlocks.CRYSTALLIZER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EvolutionChamberBlockEntity>> EVOLUTION_CHAMBER_BE =
            BLOCK_ENTITIES.register("evolution_chamber_be", () ->
                    BlockEntityType.Builder.of(EvolutionChamberBlockEntity::new,
                            ModBlocks.EVOLUTION_CHAMBER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NaniteLabBlockEntity>> NANITE_LAB_BE =
            BLOCK_ENTITIES.register("nanite_lab_be", () ->
                    BlockEntityType.Builder.of(NaniteLabBlockEntity::new,
                            ModBlocks.NANITE_LAB.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidGeneratorBlockEntity>> LAVA_GENERATOR_BE =
            BLOCK_ENTITIES.register("lava_generator_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new FluidGeneratorBlockEntity(
                                    ModBlockEntities.LAVA_GENERATOR_BE.get(), FluidGeneratorType.LAVA, pos, state),
                            ModBlocks.LAVA_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidGeneratorBlockEntity>> WATER_GENERATOR_BE =
            BLOCK_ENTITIES.register("water_generator_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new FluidGeneratorBlockEntity(
                                    ModBlockEntities.WATER_GENERATOR_BE.get(), FluidGeneratorType.WATER, pos, state),
                            ModBlocks.WATER_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoneGeneratorBlockEntity>> STONE_GENERATOR_BE =
            BLOCK_ENTITIES.register("stone_generator_be", () ->
                    BlockEntityType.Builder.of(StoneGeneratorBlockEntity::new,
                            ModBlocks.STONE_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CapsuleFillerBlockEntity>> CAPSULE_FILLER_BE =
            BLOCK_ENTITIES.register("capsule_filler_be", () ->
                    BlockEntityType.Builder.of(CapsuleFillerBlockEntity::new,
                            ModBlocks.CAPSULE_FILLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CultivatorBlockEntity>> CULTIVATOR_BE =
            BLOCK_ENTITIES.register("cultivator_be", () ->
                    BlockEntityType.Builder.of(CultivatorBlockEntity::new,
                            ModBlocks.CULTIVATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CultivatorBlockEntity>> CULTIVATOR_T2_BE =
            BLOCK_ENTITIES.register("cultivator_t2_be", () ->
                    BlockEntityType.Builder.of(CultivatorBlockEntity::t2,
                            ModBlocks.CULTIVATOR_T2.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RodFabricatorBlockEntity>> ROD_FABRICATOR_BE =
            BLOCK_ENTITIES.register("rod_fabricator_be", () ->
                    BlockEntityType.Builder.of(RodFabricatorBlockEntity::new,
                            ModBlocks.ROD_FABRICATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExtractorBlockEntity>> EXTRACTOR_BE =
            BLOCK_ENTITIES.register("extractor_be", () ->
                    BlockEntityType.Builder.of(ExtractorBlockEntity::new,
                            ModBlocks.EXTRACTOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhaseSplitterBlockEntity>> PHASE_SPLITTER_BE =
            BLOCK_ENTITIES.register("phase_splitter_be", () ->
                    BlockEntityType.Builder.of(PhaseSplitterBlockEntity::new,
                            ModBlocks.PHASE_SPLITTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RestructorBlockEntity>> RESTRUCTOR_BE =
            BLOCK_ENTITIES.register("restructor_be", () ->
                    BlockEntityType.Builder.of(RestructorBlockEntity::new,
                            ModBlocks.RESTRUCTOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RestructorBlockEntity>> RESTRUCTOR_T2_BE =
            BLOCK_ENTITIES.register("restructor_t2_be", () ->
                    BlockEntityType.Builder.of(RestructorBlockEntity::t2,
                            ModBlocks.RESTRUCTOR_T2.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResinCollectorBlockEntity>> RESIN_COLLECTOR_BE =
            BLOCK_ENTITIES.register("resin_collector_be", () ->
                    BlockEntityType.Builder.of(ResinCollectorBlockEntity::new,
                            ModBlocks.RESIN_COLLECTOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MinerCoreBlockEntity>> MINER_CORE_BE =
            BLOCK_ENTITIES.register("miner_core_be", () ->
                    BlockEntityType.Builder.of(MinerCoreBlockEntity::new,
                            ModBlocks.MINER_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialMinerCoreBlockEntity>> INDUSTRIAL_MINER_CORE_BE =
            BLOCK_ENTITIES.register("industrial_miner_core_be", () ->
                    BlockEntityType.Builder.of(IndustrialMinerCoreBlockEntity::new, ModBlocks.INDUSTRIAL_MINER_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialMinerFrameBlockEntity>> INDUSTRIAL_MINER_FRAME_BE =
            BLOCK_ENTITIES.register("industrial_miner_frame_be", () ->
                    BlockEntityType.Builder.of(IndustrialMinerFrameBlockEntity::new, ModBlocks.INDUSTRIAL_MINER_FRAME.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrillBeaconBlockEntity>> DRILL_BEACON_BE =
            BLOCK_ENTITIES.register("drill_beacon_be", () ->
                    BlockEntityType.Builder.of(DrillBeaconBlockEntity::new, ModBlocks.DRILL_BEACON.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MinerCasingBlockEntity>> MINER_CASING_BE =
            BLOCK_ENTITIES.register("miner_casing_be", () ->
                    BlockEntityType.Builder.of(MinerCasingBlockEntity::new,
                            ModBlocks.MINER_CASING.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogisticsNodeBlockEntity>> LOGISTICS_NODE_BE =
            BLOCK_ENTITIES.register("logistics_node_be", () ->
                    BlockEntityType.Builder.of(LogisticsNodeBlockEntity::new,
                            ModBlocks.LOGISTICS_NODE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HubBlockEntity>> ITEM_HUB_BE =
            BLOCK_ENTITIES.register("item_hub_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new HubBlockEntity(pos, state, state.is(ModBlocks.FLUID_HUB.get())),
                            ModBlocks.ITEM_HUB.get(), ModBlocks.FLUID_HUB.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatPipeBlockEntity>> HEAT_PIPE_BE =
            BLOCK_ENTITIES.register("heat_pipe_be", () ->
                    BlockEntityType.Builder.of(HeatPipeBlockEntity::new,
                            ModBlocks.HEAT_PIPE.get(), ModBlocks.HEAT_PIPE_T2.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyBankBlockEntity>> ENERGY_BANK_BE =
            BLOCK_ENTITIES.register("energy_bank_be", () ->
                    BlockEntityType.Builder.of(EnergyBankBlockEntity::new,
                            ModBlocks.ENERGY_BANK.get()).build(null));

    /** One type for input and output ports: the entity takes its direction from the block. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemFluidPortBlockEntity>> ITEM_FLUID_PORT_BE =
            BLOCK_ENTITIES.register("item_fluid_port_be", () ->
                    BlockEntityType.Builder.of(ItemFluidPortBlockEntity::new,
                            ModBlocks.MULTIBLOCK_INPUT_PORT.get(), ModBlocks.MULTIBLOCK_OUTPUT_PORT.get()).build(null));

    /** One type for all late multiblock cores: the entity takes shape and recipes from the block. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LateMachineCoreBlockEntity>> LATE_MACHINE_CORE_BE =
            BLOCK_ENTITIES.register("late_machine_core_be", () ->
                    BlockEntityType.Builder.of(LateMachineCoreBlockEntity::new,
                            ModBlocks.MULTIBLOCK_MACERATOR.get(), ModBlocks.MULTIBLOCK_FURNACE.get(), ModBlocks.MULTIBLOCK_WASHER.get(),
                            ModBlocks.MULTIBLOCK_COMPRESSOR.get(), ModBlocks.MULTIBLOCK_ROLLING_MILL.get(),
                            ModBlocks.MULTIBLOCK_CRAFTER.get(), ModBlocks.MULTIBLOCK_LIQUEFIER.get(), ModBlocks.MULTIBLOCK_BLAST_FURNACE.get(),
                            ModBlocks.MULTIBLOCK_FLUID_ENRICHER.get(), ModBlocks.MULTIBLOCK_MAGNET.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatPortBlockEntity>> HEAT_PORT_BE =
            BLOCK_ENTITIES.register("heat_port_be", () ->
                    BlockEntityType.Builder.of(HeatPortBlockEntity::new, ModBlocks.MULTIBLOCK_HEAT_PORT.get()).build(null));

    /** ME ports: the entity class is chosen by {@link com.jykito.industrialcore.compat.ae2.MePorts}, an ME port with AE2, a regular one without. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemFluidPortBlockEntity>> ME_PORT_BE =
            BLOCK_ENTITIES.register("me_port_be", () ->
                    BlockEntityType.Builder.of(com.jykito.industrialcore.compat.ae2.MePorts::create,
                            ModBlocks.ME_INPUT_PORT.get(), ModBlocks.ME_OUTPUT_PORT.get(), ModBlocks.ME_PATTERN_PORT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyPortBlockEntity>> ENERGY_PORT_BE =
            BLOCK_ENTITIES.register("energy_port_be", () ->
                    BlockEntityType.Builder.of(EnergyPortBlockEntity::new,
                            ModBlocks.MULTIBLOCK_ENERGY_PORT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectromagnetCoreBlockEntity>> ELECTROMAGNET_CORE_BE =
            BLOCK_ENTITIES.register("electromagnet_core_be", () ->
                    BlockEntityType.Builder.of(ElectromagnetCoreBlockEntity::new,
                            ModBlocks.ELECTROMAGNET_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistantControllerBlockEntity>> DISTANT_CONTROLLER_BE =
            BLOCK_ENTITIES.register("distant_controller_be", () ->
                    BlockEntityType.Builder.of(DistantControllerBlockEntity::new,
                            ModBlocks.DISTANT_CONTROLLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CryoCoreBlockEntity>> CRYO_CORE_BE =
            BLOCK_ENTITIES.register("cryo_core_be", () ->
                    BlockEntityType.Builder.of(CryoCoreBlockEntity::new,
                            ModBlocks.CRYO_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermostationBlockEntity>> THERMOSTATION_BE =
            BLOCK_ENTITIES.register("thermostation_be", () ->
                    BlockEntityType.Builder.of(ThermostationBlockEntity::new,
                            ModBlocks.THERMOSTATION_CORE.get(), ModBlocks.THERMOSTATION_EXPANDER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagnetPortBlockEntity>> MAGNET_PORT_BE =
            BLOCK_ENTITIES.register("magnet_port_be", () ->
                    BlockEntityType.Builder.of(MagnetPortBlockEntity::new,
                            ModBlocks.STRUCTURAL_BLOCK.get(), ModBlocks.COIL_HOLDER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AntigravColumnBlockEntity>> ANTIGRAV_COLUMN_BE =
            BLOCK_ENTITIES.register("antigrav_column_be", () ->
                    BlockEntityType.Builder.of(AntigravColumnBlockEntity::new,
                            ModBlocks.ANTIGRAV_COLUMN.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T1_BE =
            BLOCK_ENTITIES.register("solar_panel_t1_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T1_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T1.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T2_BE =
            BLOCK_ENTITIES.register("solar_panel_t2_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T2_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T2.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T3_BE =
            BLOCK_ENTITIES.register("solar_panel_t3_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T3_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T3.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T4_BE =
            BLOCK_ENTITIES.register("solar_panel_t4_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T4_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T4.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T5_BE =
            BLOCK_ENTITIES.register("solar_panel_t5_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T5_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T5.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T6_BE =
            BLOCK_ENTITIES.register("solar_panel_t6_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T6_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T6.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T7_BE =
            BLOCK_ENTITIES.register("solar_panel_t7_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T7_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T7.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T8_BE =
            BLOCK_ENTITIES.register("solar_panel_t8_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T8_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T8.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T9_BE =
            BLOCK_ENTITIES.register("solar_panel_t9_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T9_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T9.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T10_BE =
            BLOCK_ENTITIES.register("solar_panel_t10_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T10_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T10.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PanelCombinerBlockEntity>> PANEL_COMBINER_BE =
            BLOCK_ENTITIES.register("panel_combiner_be", () ->
                    BlockEntityType.Builder.of(PanelCombinerBlockEntity::new,
                            ModBlocks.PANEL_COMBINER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE_BE =
            BLOCK_ENTITIES.register("crate_be", () ->
                    BlockEntityType.Builder.of(CrateBlockEntity::new,
                            ModBlocks.CRATE_T1.get(), ModBlocks.CRATE_T2.get(), ModBlocks.CRATE_T3.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BarrelBlockEntity>> BARREL_BE =
            BLOCK_ENTITIES.register("barrel_be", () ->
                    BlockEntityType.Builder.of(BarrelBlockEntity::new,
                            ModBlocks.BARREL_T1.get(), ModBlocks.BARREL_T2.get(), ModBlocks.BARREL_T3.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
