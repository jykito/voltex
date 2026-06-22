package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, IndustrialCore.MODID);

    public static final RegistryObject<BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR_BE =
            BLOCK_ENTITIES.register("coal_generator_be", () ->
                    BlockEntityType.Builder.of(CoalGeneratorBlockEntity::new, ModBlocks.COAL_GENERATOR.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<CrusherBlockEntity>> CRUSHER_BE =
            BLOCK_ENTITIES.register("crusher_be", () ->
                    BlockEntityType.Builder.of(CrusherBlockEntity::new, ModBlocks.CRUSHER.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BE =
            BLOCK_ENTITIES.register("electric_furnace_be", () ->
                    BlockEntityType.Builder.of(ElectricFurnaceBlockEntity::new, ModBlocks.ELECTRIC_FURNACE.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<CompressorBlockEntity>> COMPRESSOR_BE =
            BLOCK_ENTITIES.register("compressor_be", () ->
                    BlockEntityType.Builder.of(CompressorBlockEntity::new, ModBlocks.COMPRESSOR.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<MetalFormerBlockEntity>> METAL_FORMER_BE =
            BLOCK_ENTITIES.register("metal_former_be", () ->
                    BlockEntityType.Builder.of(MetalFormerBlockEntity::new, ModBlocks.METAL_FORMER.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<BaseWireBlockEntity>> WIRE_BE =
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
    public static final RegistryObject<BlockEntityType<BlastFurnaceBlockEntity>> BLAST_FURNACE_BE =
            BLOCK_ENTITIES.register("blast_furnace_be", () ->
                    BlockEntityType.Builder.of(BlastFurnaceBlockEntity::new, ModBlocks.BLAST_FURNACE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ThermogeneratorBlockEntity>> THERMOGENERATOR_BE =
            BLOCK_ENTITIES.register("thermogenerator_be", () ->
                    BlockEntityType.Builder.of(ThermogeneratorBlockEntity::new, ModBlocks.THERMOGENERATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<BatteryChargerBlockEntity>> BATTERY_CHARGER_BE =
            BLOCK_ENTITIES.register("battery_charger_be",
                    () -> BlockEntityType.Builder.of(BatteryChargerBlockEntity::new,
                            ModBlocks.BATTERY_CHARGER.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidEnricherBlockEntity>> FLUID_ENRICHER_BE =
            BLOCK_ENTITIES.register("fluid_enricher_be", () ->
                    BlockEntityType.Builder.of(FluidEnricherBlockEntity::new,
                            ModBlocks.FLUID_ENRICHER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MatterFabricatorBlockEntity>> MATTER_FABRICATOR_BE =
            BLOCK_ENTITIES.register("matter_fabricator_be", () ->
                    BlockEntityType.Builder.of(MatterFabricatorBlockEntity::new,
                            ModBlocks.MATTER_FABRICATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<RecipeExporterBlockEntity>> RECIPE_EXPORTER_BE =
            BLOCK_ENTITIES.register("recipe_exporter_be", () ->
                    BlockEntityType.Builder.of(RecipeExporterBlockEntity::new,
                            ModBlocks.RECIPE_EXPORTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<OreWasherBlockEntity>> ORE_WASHER_BE =
            BLOCK_ENTITIES.register("ore_washer_be", () ->
                    BlockEntityType.Builder.of(OreWasherBlockEntity::new,
                            ModBlocks.ORE_WASHER.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.jykito.industrialcore.block.entity.LiquefierBlockEntity>> LIQUEFIER_BE =
            BLOCK_ENTITIES.register("liquefier_be", () ->
                    BlockEntityType.Builder.of(com.jykito.industrialcore.block.entity.LiquefierBlockEntity::new,
                            ModBlocks.LIQUEFIER.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidGeneratorBlockEntity>> LAVA_GENERATOR_BE =
            BLOCK_ENTITIES.register("lava_generator_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new FluidGeneratorBlockEntity(
                                    ModBlockEntities.LAVA_GENERATOR_BE.get(), FluidGeneratorType.LAVA, pos, state),
                            ModBlocks.LAVA_GENERATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidGeneratorBlockEntity>> WATER_GENERATOR_BE =
            BLOCK_ENTITIES.register("water_generator_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new FluidGeneratorBlockEntity(
                                    ModBlockEntities.WATER_GENERATOR_BE.get(), FluidGeneratorType.WATER, pos, state),
                            ModBlocks.WATER_GENERATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<StoneGeneratorBlockEntity>> STONE_GENERATOR_BE =
            BLOCK_ENTITIES.register("stone_generator_be", () ->
                    BlockEntityType.Builder.of(StoneGeneratorBlockEntity::new,
                            ModBlocks.STONE_GENERATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<CapsuleFillerBlockEntity>> CAPSULE_FILLER_BE =
            BLOCK_ENTITIES.register("capsule_filler_be", () ->
                    BlockEntityType.Builder.of(CapsuleFillerBlockEntity::new,
                            ModBlocks.CAPSULE_FILLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<CultivatorBlockEntity>> CULTIVATOR_BE =
            BLOCK_ENTITIES.register("cultivator_be", () ->
                    BlockEntityType.Builder.of(CultivatorBlockEntity::new,
                            ModBlocks.CULTIVATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<NuclearReactorBlockEntity>> NUCLEAR_REACTOR_BE =
            BLOCK_ENTITIES.register("nuclear_reactor_be", () ->
                    BlockEntityType.Builder.of(NuclearReactorBlockEntity::new,
                            ModBlocks.NUCLEAR_REACTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<ReactorChamberBlockEntity>> REACTOR_CHAMBER_BE =
            BLOCK_ENTITIES.register("reactor_chamber_be", () ->
                    BlockEntityType.Builder.of(ReactorChamberBlockEntity::new,
                            ModBlocks.REACTOR_CHAMBER.get()).build(null));

    public static final RegistryObject<BlockEntityType<RodFabricatorBlockEntity>> ROD_FABRICATOR_BE =
            BLOCK_ENTITIES.register("rod_fabricator_be", () ->
                    BlockEntityType.Builder.of(RodFabricatorBlockEntity::new,
                            ModBlocks.ROD_FABRICATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<ExtractorBlockEntity>> EXTRACTOR_BE =
            BLOCK_ENTITIES.register("extractor_be", () ->
                    BlockEntityType.Builder.of(ExtractorBlockEntity::new,
                            ModBlocks.EXTRACTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<PhaseSplitterBlockEntity>> PHASE_SPLITTER_BE =
            BLOCK_ENTITIES.register("phase_splitter_be", () ->
                    BlockEntityType.Builder.of(PhaseSplitterBlockEntity::new,
                            ModBlocks.PHASE_SPLITTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<RestructorBlockEntity>> RESTRUCTOR_BE =
            BLOCK_ENTITIES.register("restructor_be", () ->
                    BlockEntityType.Builder.of(RestructorBlockEntity::new,
                            ModBlocks.RESTRUCTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<ResinCollectorBlockEntity>> RESIN_COLLECTOR_BE =
            BLOCK_ENTITIES.register("resin_collector_be", () ->
                    BlockEntityType.Builder.of(ResinCollectorBlockEntity::new,
                            ModBlocks.RESIN_COLLECTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<MinerCoreBlockEntity>> MINER_CORE_BE =
            BLOCK_ENTITIES.register("miner_core_be", () ->
                    BlockEntityType.Builder.of(MinerCoreBlockEntity::new,
                            ModBlocks.MINER_CORE.get()).build(null));

    public static final RegistryObject<BlockEntityType<MinerCasingBlockEntity>> MINER_CASING_BE =
            BLOCK_ENTITIES.register("miner_casing_be", () ->
                    BlockEntityType.Builder.of(MinerCasingBlockEntity::new,
                            ModBlocks.MINER_CASING.get()).build(null));

    public static final RegistryObject<BlockEntityType<ItemConnectorBlockEntity>> ITEM_CONNECTOR_BE =
            BLOCK_ENTITIES.register("item_connector_be", () ->
                    BlockEntityType.Builder.of(ItemConnectorBlockEntity::new,
                            ModBlocks.ITEM_CONNECTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<FluidConnectorBlockEntity>> FLUID_CONNECTOR_BE =
            BLOCK_ENTITIES.register("fluid_connector_be", () ->
                    BlockEntityType.Builder.of(FluidConnectorBlockEntity::new,
                            ModBlocks.FLUID_CONNECTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<LogisticsNodeBlockEntity>> LOGISTICS_NODE_BE =
            BLOCK_ENTITIES.register("logistics_node_be", () ->
                    BlockEntityType.Builder.of(LogisticsNodeBlockEntity::new,
                            ModBlocks.LOGISTICS_NODE.get()).build(null));

    public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T1_BE =
            BLOCK_ENTITIES.register("solar_panel_t1_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T1_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T1.get()).build(null));

    public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T2_BE =
            BLOCK_ENTITIES.register("solar_panel_t2_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T2_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T2.get()).build(null));

    public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T3_BE =
            BLOCK_ENTITIES.register("solar_panel_t3_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T3_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T3.get()).build(null));

    public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T4_BE =
            BLOCK_ENTITIES.register("solar_panel_t4_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T4_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T4.get()).build(null));

    public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T5_BE =
            BLOCK_ENTITIES.register("solar_panel_t5_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T5_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T5.get()).build(null));

    public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T6_BE =
            BLOCK_ENTITIES.register("solar_panel_t6_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T6_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T6.get()).build(null));

    public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T7_BE =
            BLOCK_ENTITIES.register("solar_panel_t7_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T7_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T7.get()).build(null));

    public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T8_BE =
            BLOCK_ENTITIES.register("solar_panel_t8_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T8_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T8.get()).build(null));

    public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T9_BE =
            BLOCK_ENTITIES.register("solar_panel_t9_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T9_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T9.get()).build(null));

    public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_T10_BE =
            BLOCK_ENTITIES.register("solar_panel_t10_be", () ->
                    BlockEntityType.Builder.of((pos, state) -> new SolarPanelBlockEntity(ModBlockEntities.SOLAR_PANEL_T10_BE.get(), pos, state),
                            ModBlocks.SOLAR_PANEL_T10.get()).build(null));

    public static final RegistryObject<BlockEntityType<PanelCombinerBlockEntity>> PANEL_COMBINER_BE =
            BLOCK_ENTITIES.register("panel_combiner_be", () ->
                    BlockEntityType.Builder.of(PanelCombinerBlockEntity::new,
                            ModBlocks.PANEL_COMBINER.get()).build(null));

    public static final RegistryObject<BlockEntityType<CrateBlockEntity>> CRATE_BE =
            BLOCK_ENTITIES.register("crate_be", () ->
                    BlockEntityType.Builder.of(CrateBlockEntity::new,
                            ModBlocks.CRATE_T1.get(), ModBlocks.CRATE_T2.get(), ModBlocks.CRATE_T3.get()).build(null));

    public static final RegistryObject<BlockEntityType<BarrelBlockEntity>> BARREL_BE =
            BLOCK_ENTITIES.register("barrel_be", () ->
                    BlockEntityType.Builder.of(BarrelBlockEntity::new,
                            ModBlocks.BARREL_T1.get(), ModBlocks.BARREL_T2.get(), ModBlocks.BARREL_T3.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
