package com.jykito.industrialcore.client;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.fluid.ModFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Fluid textures and tints (in 1.20.1 they were FluidType#initializeClient). */
@EventBusSubscriber(modid = IndustrialCore.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientFluidExtensions {
    private ClientFluidExtensions() {}

    @SubscribeEvent
    public static void onRegister(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation WATER_STILL = ResourceLocation.parse("block/water_still");
            private static final ResourceLocation WATER_FLOW = ResourceLocation.parse("block/water_flow");

            @Override
            public ResourceLocation getStillTexture() { return WATER_STILL; }
            @Override
            public ResourceLocation getFlowingTexture() { return WATER_FLOW; }

            @Override
            public int getTintColor() {
                return 0xFF00D5FF;
            }
        }, ModFluids.COOLANT_FLUID_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation WATER_STILL = ResourceLocation.parse("block/water_still");
            private static final ResourceLocation WATER_FLOW  = ResourceLocation.parse("block/water_flow");

            @Override public ResourceLocation getStillTexture()   { return WATER_STILL; }
            @Override public ResourceLocation getFlowingTexture() { return WATER_FLOW; }

            @Override public int getTintColor() { return 0xFFFF9999; }
        }, ModFluids.HOT_COOLANT_FLUID_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.parse("block/water_still");
            private static final ResourceLocation FLOW  = ResourceLocation.parse("block/water_flow");
            @Override public ResourceLocation getStillTexture()   { return STILL; }
            @Override public ResourceLocation getFlowingTexture() { return FLOW; }
            @Override public int getTintColor() { return 0xFF3B2E1A; }
        }, ModFluids.DRILL_FLUID_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.parse("block/water_still");
            private static final ResourceLocation FLOW  = ResourceLocation.parse("block/water_flow");

            @Override public ResourceLocation getStillTexture()   { return STILL; }
            @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
            @Override public int getTintColor()                    { return 0xFF9933FF; }
        }, ModFluids.MOLTEN_AMETHYST_FLUID_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.parse("block/water_still");
            private static final ResourceLocation FLOW  = ResourceLocation.parse("block/water_flow");

            @Override public ResourceLocation getStillTexture()   { return STILL; }
            @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
            @Override public int getTintColor()                    { return 0xFFCCF2FF; }
        }, ModFluids.SUPERFLUID_CONDENSATE_FLUID_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "block/molten_lead_still");
            private static final ResourceLocation FLOW  = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "block/molten_lead_flow");

            @Override public ResourceLocation getStillTexture()   { return STILL; }
            @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
            @Override public int getTintColor()                    { return 0xFFFFFFFF; }
        }, ModFluids.MOLTEN_LEAD_FLUID_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "block/mechanical_alloy_still");
            private static final ResourceLocation FLOW  = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "block/mechanical_alloy_flow");

            @Override public ResourceLocation getStillTexture()   { return STILL; }
            @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
            @Override public int getTintColor()                    { return 0xFFFFFFFF; }
        }, ModFluids.MECHANICAL_ALLOY_FLUID_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "block/photonic_melt_still");
            private static final ResourceLocation FLOW  = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "block/photonic_melt_flow");

            @Override public ResourceLocation getStillTexture()   { return STILL; }
            @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
            @Override public int getTintColor()                    { return 0xFFFFFFFF; }
        }, ModFluids.PHOTONIC_MELT_FLUID_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "block/cryoplasma_still");
            private static final ResourceLocation FLOW  = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "block/cryoplasma_flow");

            @Override public ResourceLocation getStillTexture()   { return STILL; }
            @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
            @Override public int getTintColor()                    { return 0xFFFFFFFF; }
        }, ModFluids.CRYOPLASMA_FLUID_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "block/voltex_essence_still");
            private static final ResourceLocation FLOW  = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "block/voltex_essence_flow");

            @Override public ResourceLocation getStillTexture()   { return STILL; }
            @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
            @Override public int getTintColor()                    { return 0xFFFFFFFF; }
        }, ModFluids.VOLTEX_ESSENCE_FLUID_TYPE.get());
    }
}
