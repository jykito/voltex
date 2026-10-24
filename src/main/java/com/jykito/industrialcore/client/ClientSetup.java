package com.jykito.industrialcore.client;


import net.neoforged.fml.common.EventBusSubscriber;
import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.entity.ModEntities;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.menu.ModMenuTypes;
import com.jykito.industrialcore.screen.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = IndustrialCore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.item.ClampedItemPropertyFunction dirProp =
                    (stack, level, entity, seed) -> DirectionalUpgradeItem.getDirectionPredicate(stack);
            net.minecraft.client.renderer.item.ItemProperties.register(
                    ModItems.ITEM_PUSHER.get(),  ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "direction"), dirProp);
            net.minecraft.client.renderer.item.ItemProperties.register(
                    ModItems.ITEM_PULLER.get(),  ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "direction"), dirProp);
            net.minecraft.client.renderer.item.ItemProperties.register(
                    ModItems.FLUID_PUSHER.get(), ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "direction"), dirProp);
            net.minecraft.client.renderer.item.ItemProperties.register(
                    ModItems.FLUID_PULLER.get(), ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "direction"), dirProp);

            net.minecraft.client.renderer.item.ItemProperties.register(ModItems.UNIVERSAL_CAPSULE.get(),
                    ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "filled"),
                    (stack, level, entity, seed) -> FluidUtil.getFluidContained(stack).isPresent() ? 1.0F : 0.0F);

        });
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.COAL_GENERATOR_MENU.get(), CoalGeneratorScreen::new);
        event.register(ModMenuTypes.ELECTROMAGNET_MENU.get(), ElectromagnetScreen::new);
        event.register(ModMenuTypes.CRUSHER_MENU.get(), CrusherScreen::new);
        event.register(ModMenuTypes.ELECTRIC_FURNACE_MENU.get(), ElectricFurnaceScreen::new);
        event.register(ModMenuTypes.COMPRESSOR_MENU.get(), CompressorScreen::new);
        event.register(ModMenuTypes.ROLLING_MENU.get(), MetalFormerScreen::new);
        event.register(ModMenuTypes.BLAST_FURNACE_MENU.get(), BlastFurnaceScreen::new);
        event.register(ModMenuTypes.THERMOGENERATOR_MENU.get(), ThermogeneratorScreen::new);
        event.register(ModMenuTypes.BATTERY_CHARGER_MENU.get(), BatteryChargerScreen::new);
        event.register(ModMenuTypes.FLUID_ENRICHER_MENU.get(), FluidEnricherScreen::new);
        event.register(ModMenuTypes.LIQUEFIER_MENU.get(), LiquefierScreen::new);
        event.register(ModMenuTypes.CRYSTALLIZER_MENU.get(), CrystallizerScreen::new);
        event.register(ModMenuTypes.EVOLUTION_CHAMBER_MENU.get(), EvolutionChamberScreen::new);
        event.register(ModMenuTypes.NANITE_LAB_MENU.get(), NaniteLabScreen::new);
        event.register(ModMenuTypes.CRYO_CHAMBER_MENU.get(), CryoChamberScreen::new);
        event.register(ModMenuTypes.FLUID_GENERATOR_MENU.get(), FluidGeneratorScreen::new);
        event.register(ModMenuTypes.STONE_GENERATOR_MENU.get(), StoneGeneratorScreen::new);
        event.register(ModMenuTypes.CAPSULE_FILLER_MENU.get(), CapsuleFillerScreen::new);
        event.register(ModMenuTypes.CULTIVATOR_MENU.get(), CultivatorScreen::new);
        event.register(ModMenuTypes.CULTIVATOR_T2_MENU.get(), com.jykito.industrialcore.screen.CultivatorT2Screen::new);
        event.register(ModMenuTypes.ORE_WASHER_MENU.get(), OreWasherScreen::new);
        event.register(ModMenuTypes.BIOREACTOR_MENU.get(), BioreactorScreen::new);
        event.register(ModMenuTypes.RECIPE_EXPORTER_MENU.get(), RecipeExporterScreen::new);
        event.register(ModMenuTypes.MATTER_FABRICATOR_MENU.get(), MatterFabricatorScreen::new);
        event.register(ModMenuTypes.ROD_FABRICATOR_MENU.get(), RodFabricatorScreen::new);
        event.register(ModMenuTypes.EXTRACTOR_MENU.get(), ExtractorScreen::new);
        event.register(ModMenuTypes.PHASE_SPLITTER_MENU.get(), PhaseSplitterScreen::new);
        event.register(ModMenuTypes.RESTRUCTOR_MENU.get(), RestructorScreen::new);
        event.register(ModMenuTypes.RESTRUCTOR_T2_MENU.get(), com.jykito.industrialcore.screen.RestructorT2Screen::new);
        event.register(ModMenuTypes.MINER_CORE_MENU.get(), MinerCoreScreen::new);
        event.register(ModMenuTypes.LOGISTICS_NODE_MENU.get(), LogisticsNodeScreen::new);
        event.register(ModMenuTypes.HUB_MENU.get(), HubScreen::new);
        event.register(ModMenuTypes.SOLAR_PANEL_MENU.get(), SolarPanelScreen::new);
        event.register(ModMenuTypes.PANEL_COMBINER_MENU.get(), PanelCombinerScreen::new);
        event.register(ModMenuTypes.RESIN_COLLECTOR_MENU.get(), ResinCollectorScreen::new);
        event.register(ModMenuTypes.CRATE_MENU.get(), CrateScreen::new);
        event.register(ModMenuTypes.THERMOSTATION_MENU.get(), ThermostationScreen::new);
        event.register(ModMenuTypes.ENERGY_BANK_MENU.get(), EnergyBankScreen::new);
        event.register(ModMenuTypes.ITEM_FLUID_PORT_MENU.get(), ItemFluidPortScreen::new);
        event.register(ModMenuTypes.ME_PORT_MENU.get(), MePortScreen::new);
        event.register(ModMenuTypes.ENERGY_PORT_MENU.get(), EnergyPortScreen::new);
        event.register(ModMenuTypes.LATE_MACHINE_CORE_MENU.get(), LateMachineCoreScreen::new);
        event.register(ModMenuTypes.INDUSTRIAL_MINER_MENU.get(), IndustrialMinerScreen::new);
        event.register(ModMenuTypes.DRILL_BEACON_MENU.get(), DrillBeaconScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        ModKeyBindings.register(event);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        com.jykito.industrialcore.client.armor.PowerSuitModel.registerLayers(event);
    }

    /** Layers are rebuilt after a resource reload: armor models are taken anew. */
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        com.jykito.industrialcore.client.armor.PowerSuitModel.clearCache();
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PLASMA_WAVE.get(), PlasmaWaveRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MINER_CORE_BE.get(),
                MinerCoreBER::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LOGISTICS_NODE_BE.get(),
                LogisticsBeamRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ITEM_HUB_BE.get(),
                LogisticsBeamRenderer::new);
        // Heat pipe: the glow is now static emissive by the blockstate hot (in the chunk mesh), the BER renderer was removed
        event.registerBlockEntityRenderer(ModBlockEntities.ELECTROMAGNET_CORE_BE.get(),
                ElectromagnetCoreBER::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DISTANT_CONTROLLER_BE.get(),
                DistantControllerBER::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ANTIGRAV_COLUMN_BE.get(),
                AntigravColumnBER::new);
        event.registerBlockEntityRenderer(ModBlockEntities.INDUSTRIAL_MINER_CORE_BE.get(),
                com.jykito.industrialcore.client.miner.IndustrialMinerBER::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DRILL_BEACON_BE.get(),
                com.jykito.industrialcore.client.miner.DrillBeaconBER::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WIND_TURBINE_NACELLE_BE.get(),
                com.jykito.industrialcore.client.wind.WindTurbineBER::new);
    }

    /** The industrial miner and wind turbine models are read from resources once; reread them after a resource reload. */
    @SubscribeEvent
    public static void onRegisterReloadListeners(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> {
            com.jykito.industrialcore.client.miner.IndustrialMinerModel.clear();
            com.jykito.industrialcore.client.wind.WindTurbineModel.clear();
        });
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MinerCoreBER.DRILL_MODEL_RL);
        event.register(MinerCoreBER.DRILL_GLOW_MODEL_RL);
        event.register(MinerCoreBER.SCREW_MODEL_RL);
        event.register(ElectromagnetCoreBER.BODY_MODEL);
        event.register(ElectromagnetCoreBER.PILONS_MODEL);
        event.register(ElectromagnetCoreBER.COIL_MODEL);
        event.register(ElectromagnetCoreBER.COIL_MODEL_2);
        event.register(ElectromagnetCoreBER.COIL_MODEL_3);
        event.register(ElectromagnetCoreBER.BODY_GLOW);
        event.register(ElectromagnetCoreBER.PILONS_GLOW);
        event.register(ElectromagnetCoreBER.COIL_GLOW_2);
        event.register(ElectromagnetCoreBER.COIL_GLOW_3);
        event.register(AntigravColumnBER.PILON_MODEL);
        event.register(AntigravColumnBER.PILON_GLOW_MODEL);
        event.register(AntigravColumnBER.PILON_CURRENT_MODEL);
    }

    /** Structural glass: frame node in the inner corner of an L-shaped build (see {@link GlassCtmModel}). */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (net.minecraft.world.level.block.Block block : new net.minecraft.world.level.block.Block[]{
                com.jykito.industrialcore.block.ModBlocks.STRUCTURAL_GLASS.get(), com.jykito.industrialcore.block.ModBlocks.STRUCTURAL_GLASS_ADVANCED.get()}) {
            for (net.minecraft.world.level.block.state.BlockState s : block.getStateDefinition().getPossibleStates()) {
                net.minecraft.client.resources.model.ModelResourceLocation key = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(s);
                net.minecraft.client.resources.model.BakedModel model = event.getModels().get(key);
                if (model != null && !(model instanceof GlassCtmModel)) event.getModels().put(key, new GlassCtmModel(model));
            }
        }
        // cables and heat pipes: a cover swaps the model for the cover block's (see CoverModel)
        for (net.minecraft.world.level.block.Block block : coverableBlocks()) {
            for (net.minecraft.world.level.block.state.BlockState s : block.getStateDefinition().getPossibleStates()) {
                net.minecraft.client.resources.model.ModelResourceLocation key = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(s);
                net.minecraft.client.resources.model.BakedModel model = event.getModels().get(key);
                if (model != null && !(model instanceof CoverModel)) event.getModels().put(key, new CoverModel(model));
            }
        }
    }

    private static java.util.List<net.minecraft.world.level.block.Block> coverableBlocks() {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream().toList().stream()
                .filter(b -> b.defaultBlockState().hasProperty(com.jykito.industrialcore.block.custom.cover.Covers.COVERED)).toList();
    }

    /** A covered block takes the cover's tint (grass, leaves); an uncovered cable has none. */
    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        net.minecraft.client.color.block.BlockColors colors = event.getBlockColors();
        event.register((state, level, pos, tint) -> {
            if (level == null || pos == null || !com.jykito.industrialcore.block.custom.cover.Covers.isCovered(state)) return -1;
            return level.getBlockEntity(pos) instanceof com.jykito.industrialcore.block.custom.cover.CoverHolder h && h.getCover() != null
                    ? colors.getColor(h.getCover(), level, pos, tint) : -1;
        }, coverableBlocks().toArray(net.minecraft.world.level.block.Block[]::new));
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {

        event.register((stack, tintIndex) -> {
            if (tintIndex != 1) return 0xFFFFFFFF;
            return FluidUtil.getFluidContained(stack).map(ClientSetup::capsuleFluidColor).orElse(0xFFFFFFFF);
        }, ModItems.UNIVERSAL_CAPSULE.get());

        // nanite strain: tinted parts (layer 1) in the colour of the strain's main ore
        event.register((stack, tintIndex) -> tintIndex != 1 ? 0xFFFFFFFF
                : stack.has(com.jykito.industrialcore.item.ModDataComponents.NANITE_COLOR.get()) ? stack.get(com.jykito.industrialcore.item.ModDataComponents.NANITE_COLOR.get())   // atlas icons: silhouette, trait colour
                : com.jykito.industrialcore.nanite.NaniteColors.of(
                com.jykito.industrialcore.nanite.NaniteGenome.read(stack).dominant()), ModItems.NANITE_STRAIN.get());

        // data cards: the body (layer 1) in the topic colour, a blank one is grey
        event.register((stack, tintIndex) -> tintIndex == 1 && stack.getItem() instanceof com.jykito.industrialcore.item.custom.DataCardItem d
                ? com.jykito.industrialcore.nanite.NaniteColors.theme(d.theme()) : 0xFFFFFFFF,
                java.util.stream.Stream.concat(java.util.stream.Stream.of(ModItems.DATA_CARD_BLANK), ModItems.DATA_CARDS.stream())
                        .map(r -> (net.minecraft.world.level.ItemLike) r.get()).toArray(net.minecraft.world.level.ItemLike[]::new));
    }

    private static final java.util.Map<net.minecraft.world.level.material.Fluid, Integer> CAPSULE_FLUID_COLOR = new java.util.HashMap<>();

    private static int capsuleFluidColor(net.neoforged.neoforge.fluids.FluidStack fs) {
        var f = fs.getFluid();
        if (f == Fluids.WATER || f == Fluids.FLOWING_WATER) return 0xFF3F76E4;
        if (f == Fluids.LAVA  || f == Fluids.FLOWING_LAVA)  return 0xFFFF7A1E;
        return CAPSULE_FLUID_COLOR.computeIfAbsent(f, k -> {
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fs.getFluid());
            int tint = ext.getTintColor(fs);
            int tex  = averageTextureColor(ext.getStillTexture(fs));
            if (tex == 0) return 0xFF000000 | (tint & 0xFFFFFF);
            int r = ((tex >> 16 & 0xFF) * (tint >> 16 & 0xFF)) / 255;
            int g = ((tex >> 8  & 0xFF) * (tint >> 8  & 0xFF)) / 255;
            int b = ((tex       & 0xFF) * (tint       & 0xFF)) / 255;
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        });
    }

    private static int averageTextureColor(ResourceLocation tex) {
        try {
            ResourceLocation png = ResourceLocation.fromNamespaceAndPath(tex.getNamespace(), "textures/" + tex.getPath() + ".png");
            var resOpt = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(png);
            if (resOpt.isEmpty()) return 0;
            try (var is = resOpt.get().open();
                 com.mojang.blaze3d.platform.NativeImage img = com.mojang.blaze3d.platform.NativeImage.read(is)) {
                long rs = 0, gs = 0, bs = 0, n = 0;
                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        int p = img.getPixelRGBA(x, y);
                        if (((p >> 24) & 0xFF) < 16) continue;
                        rs += p & 0xFF; gs += (p >> 8) & 0xFF; bs += (p >> 16) & 0xFF; n++;
                    }
                }
                if (n == 0) return 0;
                return 0xFF000000 | ((int) (rs / n) << 16) | ((int) (gs / n) << 8) | (int) (bs / n);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "armor_energy_hud"), ArmorEnergyHudOverlay.OVERLAY);
    }

    @SubscribeEvent
    public static void onRegisterParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(com.jykito.industrialcore.particle.ModParticles.ENERGY_MOTE.get(),
                com.jykito.industrialcore.client.particle.EnergyMoteParticle.Provider::new);
    }

    private ClientSetup() {}
}
