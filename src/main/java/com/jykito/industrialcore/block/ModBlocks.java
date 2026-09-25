package com.jykito.industrialcore.block;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.custom.BatteryChargerBlock;
import com.jykito.industrialcore.block.custom.RubberLogBlock;
import com.jykito.industrialcore.block.custom.PanelCombinerBlock;
import com.jykito.industrialcore.block.custom.SolarPanelBlock;
import com.jykito.industrialcore.block.custom.SolarPanelTier;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.jykito.industrialcore.block.custom.wire.BaseWireBlock;
import com.jykito.industrialcore.block.custom.wire.WireTier;
import com.jykito.industrialcore.block.custom.logistics.LogisticsNodeBlock;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, IndustrialCore.MODID);

    public static final RegistryObject<Block> MACHINE_CASING = BLOCKS.register("machine_casing",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> COAL_GENERATOR = BLOCKS.register("coal_generator",
            () -> new CoalGeneratorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CRUSHER = BLOCKS.register("crusher",
            () -> new CrusherBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> ELECTRIC_FURNACE = BLOCKS.register("electric_furnace",
            () -> new ElectricFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).lightLevel(state -> state.getValue(ElectricFurnaceBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> COMPRESSOR = BLOCKS.register("compressor",
            () -> new CompressorBlock(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).lightLevel(state -> state.getValue(CompressorBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> METAL_FORMER = BLOCKS.register("metal_former",
            () -> new MetalFormerBlock(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.IRON_BLOCK).lightLevel(state -> state.getValue(MetalFormerBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> RAW_TIN_BLOCK = BLOCKS.register("raw_tin_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(5.0f, 6.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> RAW_LEAD_BLOCK = BLOCKS.register("raw_lead_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).strength(5.0f, 6.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> INVAR_BLOCK = BLOCKS.register("invar_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
    public static final RegistryObject<Block> ELECTRUM_BLOCK = BLOCKS.register("electrum_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.GOLD_BLOCK)));
    public static final RegistryObject<Block> NICKEL_BLOCK = BLOCKS.register("nickel_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
    public static final RegistryObject<Block> SILVER_BLOCK = BLOCKS.register("silver_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryObject<Block> TIN_ORE = BLOCKS.register("tin_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(3f).requiresCorrectToolForDrops(), UniformInt.of(1, 3)));

    public static final RegistryObject<Block> DEEPSLATE_TIN_ORE = BLOCKS.register("deepslate_tin_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE)
                    .strength(4.5f, 3f).requiresCorrectToolForDrops(), UniformInt.of(1, 4)));

    public static final RegistryObject<Block> LEAD_ORE = BLOCKS.register("lead_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(3f).requiresCorrectToolForDrops(), UniformInt.of(1, 3)));

    public static final RegistryObject<Block> DEEPSLATE_LEAD_ORE = BLOCKS.register("deepslate_lead_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE)
                    .strength(4.5f, 3f).requiresCorrectToolForDrops(), UniformInt.of(1, 4)));

    public static final RegistryObject<Block> BAUXITE_ORE = BLOCKS.register("bauxite_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(3f).requiresCorrectToolForDrops(), UniformInt.of(0, 2)));

    public static final RegistryObject<Block> DEEPSLATE_BAUXITE_ORE = BLOCKS.register("deepslate_bauxite_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE)
                    .strength(4.5f, 3f).requiresCorrectToolForDrops(), UniformInt.of(0, 2)));

    public static final RegistryObject<Block> COPPER_CABLE = BLOCKS.register("copper_cable",
            () -> new BaseWireBlock(WireTier.COPPER, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> FERRO_CABLE = BLOCKS.register("ferro_cable",
            () -> new BaseWireBlock(WireTier.FERRO, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> GOLD_CONDUCTOR = BLOCKS.register("gold_conductor",
            () -> new BaseWireBlock(WireTier.GOLD, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> CRYSTALLINE_CHANNEL = BLOCKS.register("crystalline_channel",
            () -> new BaseWireBlock(WireTier.CRYSTALLINE, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> EMERALD_FLUX_CABLE = BLOCKS.register("emerald_flux_cable",
            () -> new BaseWireBlock(WireTier.EMERALD_FLUX, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> HEAVY_MAGISTRAL = BLOCKS.register("heavy_magistral",
            () -> new BaseWireBlock(WireTier.HEAVY_MAGISTRAL, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> NETHERITE_FIBER_CABLE = BLOCKS.register("netherite_fiber_cable",
            () -> new BaseWireBlock(WireTier.NETHERITE_FIBER, BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> RESIN_LOG = BLOCKS.register("resin_log",
            () -> new RubberLogBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)));

    public static final RegistryObject<Block> RESIN_LEAVES = BLOCKS.register("resin_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .noOcclusion()));

    public static final RegistryObject<Block> RESIN_SAPLING = BLOCKS.register("resin_sapling",
            () -> new com.jykito.industrialcore.block.custom.RubberSaplingBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING).noOcclusion()));

    public static final RegistryObject<Block> RESIN_COLLECTOR = BLOCKS.register("resin_collector",
            () -> new com.jykito.industrialcore.block.custom.ResinCollectorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> THERMOGENERATOR = BLOCKS.register("thermogenerator",
            () -> new com.jykito.industrialcore.block.custom.ThermogeneratorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f)));

    public static final RegistryObject<Block> BLAST_FURNACE = BLOCKS.register("blast_furnace",
            () -> new com.jykito.industrialcore.block.custom.BlastFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f)));

    public static final RegistryObject<Block> BATTERY_CHARGER = BLOCKS.register("battery_charger",
            () -> new BatteryChargerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> FLUID_ENRICHER = BLOCKS.register("fluid_enricher",
            () -> new com.jykito.industrialcore.block.custom.FluidEnricherBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f)));

    public static final RegistryObject<Block> RECIPE_EXPORTER = BLOCKS.register("recipe_exporter",
            () -> new com.jykito.industrialcore.block.custom.RecipeExporterBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f)));

    public static final RegistryObject<Block> MATTER_FABRICATOR = BLOCKS.register("matter_fabricator",
            () -> new com.jykito.industrialcore.block.custom.MatterFabricatorBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                            .requiresCorrectToolForDrops().strength(6.0f, 10.0f)
                            .lightLevel(state -> state.getValue(com.jykito.industrialcore.block.custom.MatterFabricatorBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> ORE_WASHER = BLOCKS.register("ore_washer",
            () -> new com.jykito.industrialcore.block.custom.OreWasherBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(5.0f)
                    .lightLevel(state -> state.getValue(com.jykito.industrialcore.block.custom.OreWasherBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> LIQUEFIER = BLOCKS.register("liquefier",
            () -> new com.jykito.industrialcore.block.custom.LiquefierBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(5.0f)
                    .lightLevel(state -> state.getValue(com.jykito.industrialcore.block.custom.LiquefierBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> CRYSTALLIZER = BLOCKS.register("crystallizer",
            () -> new com.jykito.industrialcore.block.custom.CrystallizerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(5.0f)
                    .lightLevel(state -> state.getValue(com.jykito.industrialcore.block.custom.CrystallizerBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> LAVA_GENERATOR = BLOCKS.register("lava_generator",
            () -> new com.jykito.industrialcore.block.custom.FluidGeneratorBlock(
                    com.jykito.industrialcore.block.entity.FluidGeneratorType.LAVA,
                    () -> com.jykito.industrialcore.block.entity.ModBlockEntities.LAVA_GENERATOR_BE.get(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f)
                            .lightLevel(state -> state.getValue(com.jykito.industrialcore.block.custom.FluidGeneratorBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> WATER_GENERATOR = BLOCKS.register("water_generator",
            () -> new com.jykito.industrialcore.block.custom.FluidGeneratorBlock(
                    com.jykito.industrialcore.block.entity.FluidGeneratorType.WATER,
                    () -> com.jykito.industrialcore.block.entity.ModBlockEntities.WATER_GENERATOR_BE.get(),
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f)
                            .lightLevel(state -> state.getValue(com.jykito.industrialcore.block.custom.FluidGeneratorBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> STONE_GENERATOR = BLOCKS.register("stone_generator",
            () -> new com.jykito.industrialcore.block.custom.StoneGeneratorBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f)
                            .lightLevel(state -> state.getValue(com.jykito.industrialcore.block.custom.StoneGeneratorBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> CAPSULE_FILLER = BLOCKS.register("capsule_filler",
            () -> new com.jykito.industrialcore.block.custom.CapsuleFillerBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f)
                            .lightLevel(state -> state.getValue(com.jykito.industrialcore.block.custom.CapsuleFillerBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> CULTIVATOR = BLOCKS.register("cultivator",
            () -> new com.jykito.industrialcore.block.custom.CultivatorBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f).noOcclusion()
                            .lightLevel(state -> state.getValue(com.jykito.industrialcore.block.custom.CultivatorBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> EXTRACTOR = BLOCKS.register("extractor",
            () -> new ExtractorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(ExtractorBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> PHASE_SPLITTER = BLOCKS.register("phase_splitter",
            () -> new PhaseSplitterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(5.0f)
                    .lightLevel(state -> state.getValue(PhaseSplitterBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> RESTRUCTOR = BLOCKS.register("restructor",
            () -> new com.jykito.industrialcore.block.custom.RestructorBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                            .requiresCorrectToolForDrops().strength(6.0f, 10.0f).noOcclusion()
                            .lightLevel(state -> state.getValue(com.jykito.industrialcore.block.custom.RestructorBlock.LIT) ? 1 : 0)));

    public static final RegistryObject<Block> MINER_CASING = BLOCKS.register("miner_casing",
            () -> new com.jykito.industrialcore.block.custom.MinerCasingBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f).noOcclusion()));

    public static final RegistryObject<Block> MINER_CORE = BLOCKS.register("miner_core",
            () -> new com.jykito.industrialcore.block.custom.MinerCoreBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f).noOcclusion()
                            .lightLevel(state -> state.getValue(com.jykito.industrialcore.block.custom.MinerCoreBlock.LIT) ? 12 : 0)));

    public static final RegistryObject<Block> LOGISTICS_NODE = BLOCKS.register("logistics_node",
            () -> new LogisticsNodeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().strength(2.0f)));

    public static final RegistryObject<Block> ITEM_HUB = BLOCKS.register("item_hub",
            () -> new com.jykito.industrialcore.block.custom.logistics.HubBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().strength(2.0f)));

    public static final RegistryObject<Block> FLUID_HUB = BLOCKS.register("fluid_hub",
            () -> new com.jykito.industrialcore.block.custom.logistics.HubBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion().strength(2.0f)));

    // ── Криокамера ──
    // Свойства от стекла: бьётся быстро, света не глушит, инструмент не требует.
    // noOcclusion обязателен — иначе движок сочтёт панель сплошным кубом, срежет соседние
    // грани и внутри камеры будет темно.
    // Кладка читается цельной плитой: рамка не в текстуре, а брусками по рёбрам,
    // которые гасятся соседом. Occlusion НЕ снимаем — ядро полноценный куб.
    public static final RegistryObject<Block> CRYO_CASING = BLOCKS.register("cryo_casing",
            () -> new com.jykito.industrialcore.block.custom.CryoCasingBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.0f)));

    public static final RegistryObject<Block> CRYO_CORE = BLOCKS.register("cryo_core",
            () -> new com.jykito.industrialcore.block.custom.CryoCoreBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.0f)));

    public static final RegistryObject<Block> REFRIGERATION_CHAMBER = BLOCKS.register("refrigeration_chamber",
            () -> new com.jykito.industrialcore.block.custom.RefrigerationChamberBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(2.5f)));

    public static final RegistryObject<Block> CRYO_GLASS = BLOCKS.register("cryo_glass",
            () -> new com.jykito.industrialcore.block.custom.CryoGlassBlock(
                    BlockBehaviour.Properties.copy(Blocks.GLASS).noOcclusion()));

    public static final RegistryObject<Block> HEAT_PIPE = BLOCKS.register("heat_pipe",
            () -> new com.jykito.industrialcore.block.custom.HeatPipeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(1.5f).noOcclusion()));

    public static final RegistryObject<Block> HEAT_PIPE_T2 = BLOCKS.register("heat_pipe_t2",
            () -> new com.jykito.industrialcore.block.custom.HeatPipeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(1.5f).noOcclusion()));

    public static final RegistryObject<Block> ENERGY_BANK = BLOCKS.register("energy_bank",
            () -> new com.jykito.industrialcore.block.custom.EnergyBankBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.0f).requiresCorrectToolForDrops()));

    // ── Электромагнит / ускоритель частиц (мультиблок) ──
    public static final RegistryObject<Block> STRUCTURAL_BLOCK = BLOCKS.register("structural_block",
            () -> new com.jykito.industrialcore.block.custom.MultiblockShellBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f).noOcclusion()));

    public static final RegistryObject<Block> COIL_HOLDER = BLOCKS.register("coil_holder",
            () -> new com.jykito.industrialcore.block.custom.MultiblockShellBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f).noOcclusion()));

    public static final RegistryObject<Block> ELECTROMAGNET_CORE = BLOCKS.register("electromagnet_core",
            () -> new com.jykito.industrialcore.block.custom.ElectromagnetCoreBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(6.0f, 10.0f).noOcclusion()));

    public static final RegistryObject<Block> DISTANT_CONTROLLER = BLOCKS.register("distant_controller",
            () -> new com.jykito.industrialcore.block.custom.DistantControllerBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(4.0f).noOcclusion()));

    // ── Термостанция (башня: ядро + расширители + радиаторы) ──
    public static final RegistryObject<Block> THERMOSTATION_CORE = BLOCKS.register("thermostation_core",
            () -> new com.jykito.industrialcore.block.custom.ThermostationCoreBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f).noOcclusion()));
    public static final RegistryObject<Block> THERMOSTATION_EXPANDER = BLOCKS.register("thermostation_expander",
            () -> new com.jykito.industrialcore.block.custom.ThermostationExpanderBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0f).noOcclusion()));
    public static final RegistryObject<Block> THERMOSTATION_RADIATOR = BLOCKS.register("thermostation_radiator",
            () -> new com.jykito.industrialcore.block.custom.ThermostationRadiatorBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(4.0f).noOcclusion()));

    public static final RegistryObject<Block> ANTIGRAV_COLUMN = BLOCKS.register("antigrav_column",
            () -> new com.jykito.industrialcore.block.custom.AntigravColumnBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(4.0f).noOcclusion()));

    public static final RegistryObject<Block> ROD_FABRICATOR = BLOCKS.register("rod_fabricator",
            () -> new RodFabricatorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(5.0f)
                    .lightLevel(state -> state.getValue(RodFabricatorBlock.LIT) ? 1 : 0)));

    private static BlockBehaviour.Properties solarProps(SolarPanelTier tier) {
        return BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                .requiresCorrectToolForDrops().strength(3.0f)
                .lightLevel(state -> state.getValue(SolarPanelBlock.LIT) ? tier.level : 0);
    }

    public static final RegistryObject<Block> SOLAR_PANEL_T1 = BLOCKS.register("solar_panel_t1",
            () -> new SolarPanelBlock(SolarPanelTier.T1, solarProps(SolarPanelTier.T1)));
    public static final RegistryObject<Block> SOLAR_PANEL_T2 = BLOCKS.register("solar_panel_t2",
            () -> new SolarPanelBlock(SolarPanelTier.T2, solarProps(SolarPanelTier.T2)));
    public static final RegistryObject<Block> SOLAR_PANEL_T3 = BLOCKS.register("solar_panel_t3",
            () -> new SolarPanelBlock(SolarPanelTier.T3, solarProps(SolarPanelTier.T3)));
    public static final RegistryObject<Block> SOLAR_PANEL_T4 = BLOCKS.register("solar_panel_t4",
            () -> new SolarPanelBlock(SolarPanelTier.T4, solarProps(SolarPanelTier.T4)));
    public static final RegistryObject<Block> SOLAR_PANEL_T5 = BLOCKS.register("solar_panel_t5",
            () -> new SolarPanelBlock(SolarPanelTier.T5, solarProps(SolarPanelTier.T5)));
    public static final RegistryObject<Block> SOLAR_PANEL_T6 = BLOCKS.register("solar_panel_t6",
            () -> new SolarPanelBlock(SolarPanelTier.T6, solarProps(SolarPanelTier.T6)));
    public static final RegistryObject<Block> SOLAR_PANEL_T7 = BLOCKS.register("solar_panel_t7",
            () -> new SolarPanelBlock(SolarPanelTier.T7, solarProps(SolarPanelTier.T7)));
    public static final RegistryObject<Block> SOLAR_PANEL_T8 = BLOCKS.register("solar_panel_t8",
            () -> new SolarPanelBlock(SolarPanelTier.T8, solarProps(SolarPanelTier.T8)));
    public static final RegistryObject<Block> SOLAR_PANEL_T9 = BLOCKS.register("solar_panel_t9",
            () -> new SolarPanelBlock(SolarPanelTier.T9, solarProps(SolarPanelTier.T9)));
    public static final RegistryObject<Block> SOLAR_PANEL_T10 = BLOCKS.register("solar_panel_t10",
            () -> new SolarPanelBlock(SolarPanelTier.T10, solarProps(SolarPanelTier.T10)));

    public static final RegistryObject<Block> PANEL_COMBINER = BLOCKS.register("panel_combiner",
            () -> new PanelCombinerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(4.0f)));

    public static final RegistryObject<Block> CRATE_T1 = BLOCKS.register("crate_t1",
            () -> new com.jykito.industrialcore.block.custom.CrateBlock(6, BlockBehaviour.Properties.copy(Blocks.BARREL).strength(2.5f)));
    public static final RegistryObject<Block> CRATE_T2 = BLOCKS.register("crate_t2",
            () -> new com.jykito.industrialcore.block.custom.CrateBlock(8, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CRATE_T3 = BLOCKS.register("crate_t3",
            () -> new com.jykito.industrialcore.block.custom.CrateBlock(10, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> BARREL_T1 = BLOCKS.register("barrel_t1",
            () -> new com.jykito.industrialcore.block.custom.BarrelBlock(32000, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.0f).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> BARREL_T2 = BLOCKS.register("barrel_t2",
            () -> new com.jykito.industrialcore.block.custom.BarrelBlock(128000, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> BARREL_T3 = BLOCKS.register("barrel_t3",
            () -> new com.jykito.industrialcore.block.custom.BarrelBlock(512000, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(5.0f).requiresCorrectToolForDrops().noOcclusion()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
