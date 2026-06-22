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
            MenuScreens.register(ModMenuTypes.CRUSHER_MENU.get(), CrusherScreen::new);
            MenuScreens.register(ModMenuTypes.ELECTRIC_FURNACE_MENU.get(), ElectricFurnaceScreen::new);
            MenuScreens.register(ModMenuTypes.COMPRESSOR_MENU.get(), CompressorScreen::new);
            MenuScreens.register(ModMenuTypes.ROLLING_MENU.get(), MetalFormerScreen::new);
            MenuScreens.register(ModMenuTypes.BLAST_FURNACE_MENU.get(), BlastFurnaceScreen::new);
            MenuScreens.register(ModMenuTypes.THERMOGENERATOR_MENU.get(), ThermogeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.BATTERY_CHARGER_MENU.get(), BatteryChargerScreen::new);
            MenuScreens.register(ModMenuTypes.FLUID_ENRICHER_MENU.get(), FluidEnricherScreen::new);
            MenuScreens.register(ModMenuTypes.LIQUEFIER_MENU.get(), LiquefierScreen::new);
            MenuScreens.register(ModMenuTypes.FLUID_GENERATOR_MENU.get(), FluidGeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.STONE_GENERATOR_MENU.get(), StoneGeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.CAPSULE_FILLER_MENU.get(), CapsuleFillerScreen::new);
            MenuScreens.register(ModMenuTypes.CULTIVATOR_MENU.get(), CultivatorScreen::new);
            MenuScreens.register(ModMenuTypes.ORE_WASHER_MENU.get(), OreWasherScreen::new);
            MenuScreens.register(ModMenuTypes.RECIPE_EXPORTER_MENU.get(), RecipeExporterScreen::new);
            MenuScreens.register(ModMenuTypes.MATTER_FABRICATOR_MENU.get(), MatterFabricatorScreen::new);
            MenuScreens.register(ModMenuTypes.NUCLEAR_REACTOR_MENU.get(), NuclearReactorScreen::new);
            MenuScreens.register(ModMenuTypes.ROD_FABRICATOR_MENU.get(), RodFabricatorScreen::new);
            MenuScreens.register(ModMenuTypes.EXTRACTOR_MENU.get(), ExtractorScreen::new);
            MenuScreens.register(ModMenuTypes.PHASE_SPLITTER_MENU.get(), PhaseSplitterScreen::new);
            MenuScreens.register(ModMenuTypes.RESTRUCTOR_MENU.get(), RestructorScreen::new);
            MenuScreens.register(ModMenuTypes.MINER_CORE_MENU.get(), MinerCoreScreen::new);
            MenuScreens.register(ModMenuTypes.ITEM_CONNECTOR_MENU.get(), ItemConnectorScreen::new);
            MenuScreens.register(ModMenuTypes.FLUID_CONNECTOR_MENU.get(), FluidConnectorScreen::new);
            MenuScreens.register(ModMenuTypes.LOGISTICS_NODE_MENU.get(), LogisticsNodeScreen::new);
            MenuScreens.register(ModMenuTypes.SOLAR_PANEL_MENU.get(), SolarPanelScreen::new);
            MenuScreens.register(ModMenuTypes.PANEL_COMBINER_MENU.get(), PanelCombinerScreen::new);
            MenuScreens.register(ModMenuTypes.RESIN_COLLECTOR_MENU.get(), ResinCollectorScreen::new);
            MenuScreens.register(ModMenuTypes.CRATE_MENU.get(), CrateScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        ModKeyBindings.register(event);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PLASMA_WAVE.get(), PlasmaWaveRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RESTRUCTOR_BE.get(),
                RestructorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.NUCLEAR_REACTOR_BE.get(),
                NuclearReactorBER::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MINER_CORE_BE.get(),
                MinerCoreBER::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ITEM_CONNECTOR_BE.get(),
                LogisticsBeamRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FLUID_CONNECTOR_BE.get(),
                LogisticsBeamRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LOGISTICS_NODE_BE.get(),
                LogisticsBeamRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MinerCoreBER.DRILL_MODEL_RL);
        event.register(MinerCoreBER.SCREW_MODEL_RL);
        event.register(NuclearReactorBER.IDLE_MODEL_RL);
        event.register(NuclearReactorBER.ACTIVE_MODEL_RL);
        event.register(NuclearReactorBER.EMISSIVE_ACTIVE_MODEL_RL);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {

        event.register((stack, tintIndex) -> {
            if (tintIndex != 1) return 0xFFFFFFFF;
            return FluidUtil.getFluidContained(stack).map(ClientSetup::capsuleFluidColor).orElse(0xFFFFFFFF);
        }, ModItems.UNIVERSAL_CAPSULE.get());
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

    private ClientSetup() {}
}
