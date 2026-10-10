package com.jykito.industrialcore.client;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.entity.ModEntities;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.menu.ModMenuTypes;
import com.jykito.industrialcore.screen.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = IndustrialCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.item.ClampedItemPropertyFunction dirProp =
                    (stack, level, entity, seed) -> DirectionalUpgradeItem.getDirectionPredicate(stack);
            net.minecraft.client.renderer.item.ItemProperties.register(
                    ModItems.ITEM_PUSHER.get(),  new ResourceLocation(IndustrialCore.MODID, "direction"), dirProp);
            net.minecraft.client.renderer.item.ItemProperties.register(
                    ModItems.ITEM_PULLER.get(),  new ResourceLocation(IndustrialCore.MODID, "direction"), dirProp);
            net.minecraft.client.renderer.item.ItemProperties.register(
                    ModItems.FLUID_PUSHER.get(), new ResourceLocation(IndustrialCore.MODID, "direction"), dirProp);
            net.minecraft.client.renderer.item.ItemProperties.register(
                    ModItems.FLUID_PULLER.get(), new ResourceLocation(IndustrialCore.MODID, "direction"), dirProp);

            net.minecraft.client.renderer.item.ItemProperties.register(ModItems.UNIVERSAL_CAPSULE.get(),
                    new ResourceLocation(IndustrialCore.MODID, "filled"),
                    (stack, level, entity, seed) -> FluidUtil.getFluidContained(stack).isPresent() ? 1.0F : 0.0F);

            MenuScreens.register(ModMenuTypes.COAL_GENERATOR_MENU.get(), CoalGeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.ELECTROMAGNET_MENU.get(), ElectromagnetScreen::new);
            MenuScreens.register(ModMenuTypes.CRUSHER_MENU.get(), CrusherScreen::new);
            MenuScreens.register(ModMenuTypes.ELECTRIC_FURNACE_MENU.get(), ElectricFurnaceScreen::new);
            MenuScreens.register(ModMenuTypes.COMPRESSOR_MENU.get(), CompressorScreen::new);
            MenuScreens.register(ModMenuTypes.ROLLING_MENU.get(), MetalFormerScreen::new);
            MenuScreens.register(ModMenuTypes.BLAST_FURNACE_MENU.get(), BlastFurnaceScreen::new);
            MenuScreens.register(ModMenuTypes.THERMOGENERATOR_MENU.get(), ThermogeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.BATTERY_CHARGER_MENU.get(), BatteryChargerScreen::new);
            MenuScreens.register(ModMenuTypes.FLUID_ENRICHER_MENU.get(), FluidEnricherScreen::new);
            MenuScreens.register(ModMenuTypes.LIQUEFIER_MENU.get(), LiquefierScreen::new);
            MenuScreens.register(ModMenuTypes.CRYSTALLIZER_MENU.get(), CrystallizerScreen::new);
            MenuScreens.register(ModMenuTypes.EVOLUTION_CHAMBER_MENU.get(), EvolutionChamberScreen::new);
            MenuScreens.register(ModMenuTypes.NANITE_LAB_MENU.get(), NaniteLabScreen::new);
            MenuScreens.register(ModMenuTypes.CRYO_CHAMBER_MENU.get(), CryoChamberScreen::new);
            MenuScreens.register(ModMenuTypes.FLUID_GENERATOR_MENU.get(), FluidGeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.STONE_GENERATOR_MENU.get(), StoneGeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.CAPSULE_FILLER_MENU.get(), CapsuleFillerScreen::new);
            MenuScreens.register(ModMenuTypes.CULTIVATOR_MENU.get(), CultivatorScreen::new);
            MenuScreens.register(ModMenuTypes.CULTIVATOR_T2_MENU.get(), com.jykito.industrialcore.screen.CultivatorT2Screen::new);
            MenuScreens.register(ModMenuTypes.ORE_WASHER_MENU.get(), OreWasherScreen::new);
            MenuScreens.register(ModMenuTypes.BIOREACTOR_MENU.get(), BioreactorScreen::new);
            MenuScreens.register(ModMenuTypes.RECIPE_EXPORTER_MENU.get(), RecipeExporterScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_FABRICATOR_MENU.get(), MatterFabricatorScreen::new);
            MenuScreens.register(ModMenuTypes.ROD_FABRICATOR_MENU.get(), RodFabricatorScreen::new);
            MenuScreens.register(ModMenuTypes.EXTRACTOR_MENU.get(), ExtractorScreen::new);
            MenuScreens.register(ModMenuTypes.PHASE_SPLITTER_MENU.get(), PhaseSplitterScreen::new);
            MenuScreens.register(ModMenuTypes.RESTRUCTOR_MENU.get(), RestructorScreen::new);
            MenuScreens.register(ModMenuTypes.RESTRUCTOR_T2_MENU.get(), com.jykito.industrialcore.screen.RestructorT2Screen::new);
            MenuScreens.register(ModMenuTypes.MINER_CORE_MENU.get(), MinerCoreScreen::new);
            MenuScreens.register(ModMenuTypes.LOGISTICS_NODE_MENU.get(), LogisticsNodeScreen::new);
            MenuScreens.register(ModMenuTypes.HUB_MENU.get(), HubScreen::new);
            MenuScreens.register(ModMenuTypes.SOLAR_PANEL_MENU.get(), SolarPanelScreen::new);
            MenuScreens.register(ModMenuTypes.PANEL_COMBINER_MENU.get(), PanelCombinerScreen::new);
            MenuScreens.register(ModMenuTypes.RESIN_COLLECTOR_MENU.get(), ResinCollectorScreen::new);
            MenuScreens.register(ModMenuTypes.CRATE_MENU.get(), CrateScreen::new);
            MenuScreens.register(ModMenuTypes.THERMOSTATION_MENU.get(), ThermostationScreen::new);
            MenuScreens.register(ModMenuTypes.ENERGY_BANK_MENU.get(), EnergyBankScreen::new);
            MenuScreens.register(ModMenuTypes.ITEM_FLUID_PORT_MENU.get(), ItemFluidPortScreen::new);
            MenuScreens.register(ModMenuTypes.ME_PORT_MENU.get(), MePortScreen::new);
            MenuScreens.register(ModMenuTypes.ENERGY_PORT_MENU.get(), EnergyPortScreen::new);
            MenuScreens.register(ModMenuTypes.LATE_MACHINE_CORE_MENU.get(), LateMachineCoreScreen::new);
            MenuScreens.register(ModMenuTypes.INDUSTRIAL_MINER_MENU.get(), IndustrialMinerScreen::new);
            MenuScreens.register(ModMenuTypes.DRILL_BEACON_MENU.get(), DrillBeaconScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        ModKeyBindings.register(event);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        com.jykito.industrialcore.client.armor.PowerSuitModel.registerLayers(event);
    }

    /** Слои пересобраны после перезагрузки ресурсов: модели брони берутся заново. */
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
        // Теплотруба: свечение теперь статик-эмиссив по blockstate hot (в чанк-меше), BER-рендерер убран
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
    }

    /** Модель промышленного шахтёра читается из ресурсов один раз — после перезагрузки ресурсов перечитать. */
    @SubscribeEvent
    public static void onRegisterReloadListeners(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm ->
                com.jykito.industrialcore.client.miner.IndustrialMinerModel.clear());
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

    /** Структурное стекло: узел рамки во внутреннем углу Г-образной постройки (см. {@link GlassCtmModel}). */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (net.minecraft.world.level.block.Block block : new net.minecraft.world.level.block.Block[]{
                com.jykito.industrialcore.block.ModBlocks.STRUCTURAL_GLASS.get(), com.jykito.industrialcore.block.ModBlocks.STRUCTURAL_GLASS_ADVANCED.get()}) {
            for (net.minecraft.world.level.block.state.BlockState s : block.getStateDefinition().getPossibleStates()) {
                ResourceLocation key = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(s);
                net.minecraft.client.resources.model.BakedModel model = event.getModels().get(key);
                if (model != null && !(model instanceof GlassCtmModel)) event.getModels().put(key, new GlassCtmModel(model));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {

        event.register((stack, tintIndex) -> {
            if (tintIndex != 1) return 0xFFFFFFFF;
            return FluidUtil.getFluidContained(stack).map(ClientSetup::capsuleFluidColor).orElse(0xFFFFFFFF);
        }, ModItems.UNIVERSAL_CAPSULE.get());

        // штамм нанитов: окрашенные части (слой 1) в цвет главной руды штамма
        event.register((stack, tintIndex) -> tintIndex != 1 ? 0xFFFFFFFF
                : stack.hasTag() && stack.getTag().contains(com.jykito.industrialcore.nanite.NaniteAtlas.COLOR_TAG) ? stack.getTag().getInt(com.jykito.industrialcore.nanite.NaniteAtlas.COLOR_TAG)   // иконки атласа: силуэт, цвет черты
                : com.jykito.industrialcore.nanite.NaniteColors.of(
                com.jykito.industrialcore.nanite.NaniteGenome.read(stack).dominant()), ModItems.NANITE_STRAIN.get());

        // карты данных: корпус (слой 1) в цвет темы, пустая — серая
        event.register((stack, tintIndex) -> tintIndex == 1 && stack.getItem() instanceof com.jykito.industrialcore.item.custom.DataCardItem d
                ? com.jykito.industrialcore.nanite.NaniteColors.theme(d.theme()) : 0xFFFFFFFF,
                java.util.stream.Stream.concat(java.util.stream.Stream.of(ModItems.DATA_CARD_BLANK), ModItems.DATA_CARDS.stream())
                        .map(r -> (net.minecraft.world.level.ItemLike) r.get()).toArray(net.minecraft.world.level.ItemLike[]::new));
    }

    private static final java.util.Map<net.minecraft.world.level.material.Fluid, Integer> CAPSULE_FLUID_COLOR = new java.util.HashMap<>();

    private static int capsuleFluidColor(net.minecraftforge.fluids.FluidStack fs) {
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
            ResourceLocation png = new ResourceLocation(tex.getNamespace(), "textures/" + tex.getPath() + ".png");
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
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("armor_energy_hud", ArmorEnergyHudOverlay.OVERLAY);
    }

    @SubscribeEvent
    public static void onRegisterParticles(net.minecraftforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(com.jykito.industrialcore.particle.ModParticles.ENERGY_MOTE.get(),
                com.jykito.industrialcore.client.particle.EnergyMoteParticle.Provider::new);
    }

    private ClientSetup() {}
}
