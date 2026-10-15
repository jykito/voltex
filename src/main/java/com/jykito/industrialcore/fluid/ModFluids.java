package com.jykito.industrialcore.fluid;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.function.Consumer;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, IndustrialCore.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, IndustrialCore.MODID);

    public static final RegistryObject<FluidType> COOLANT_FLUID_TYPE = FLUID_TYPES.register("coolant_fluid",
            () -> new FluidType(FluidType.Properties.create().density(1500).viscosity(1000)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation WATER_STILL = new ResourceLocation("block/water_still");
                        private static final ResourceLocation WATER_FLOW = new ResourceLocation("block/water_flow");

                        @Override
                        public ResourceLocation getStillTexture() { return WATER_STILL; }
                        @Override
                        public ResourceLocation getFlowingTexture() { return WATER_FLOW; }

                        @Override
                        public int getTintColor() {
                            return 0xFF00D5FF;
                        }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> SOURCE_COOLANT = FLUIDS.register("coolant",
            () -> new ForgeFlowingFluid.Source(ModFluids.COOLANT_PROPERTIES));

    public static final RegistryObject<FlowingFluid> FLOWING_COOLANT = FLUIDS.register("coolant_flowing",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.COOLANT_PROPERTIES));

    private static final ForgeFlowingFluid.Properties COOLANT_PROPERTIES = new ForgeFlowingFluid.Properties(
            COOLANT_FLUID_TYPE, SOURCE_COOLANT, FLOWING_COOLANT)
            .bucket(ForgeRegistries.ITEMS.getDelegateOrThrow(net.minecraft.world.item.Items.AIR));

    public static final RegistryObject<FluidType> HOT_COOLANT_FLUID_TYPE = FLUID_TYPES.register("hot_coolant_fluid",
            () -> new FluidType(FluidType.Properties.create().density(1500).viscosity(1000).temperature(800)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation WATER_STILL = new ResourceLocation("block/water_still");
                        private static final ResourceLocation WATER_FLOW  = new ResourceLocation("block/water_flow");

                        @Override public ResourceLocation getStillTexture()   { return WATER_STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return WATER_FLOW; }

                        @Override public int getTintColor() { return 0xFFFF9999; }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> SOURCE_HOT_COOLANT = FLUIDS.register("hot_coolant",
            () -> new ForgeFlowingFluid.Source(ModFluids.HOT_COOLANT_PROPERTIES));

    public static final RegistryObject<FlowingFluid> FLOWING_HOT_COOLANT = FLUIDS.register("hot_coolant_flowing",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.HOT_COOLANT_PROPERTIES));

    private static final ForgeFlowingFluid.Properties HOT_COOLANT_PROPERTIES = new ForgeFlowingFluid.Properties(
            HOT_COOLANT_FLUID_TYPE, SOURCE_HOT_COOLANT, FLOWING_HOT_COOLANT)
            .bucket(ForgeRegistries.ITEMS.getDelegateOrThrow(net.minecraft.world.item.Items.AIR));

    public static final RegistryObject<FluidType> DRILL_FLUID_TYPE = FLUID_TYPES.register("drill_fluid",
            () -> new FluidType(FluidType.Properties.create().density(1200).viscosity(1500)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation STILL = new ResourceLocation("block/water_still");
                        private static final ResourceLocation FLOW  = new ResourceLocation("block/water_flow");
                        @Override public ResourceLocation getStillTexture()   { return STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return FLOW; }
                        @Override public int getTintColor() { return 0xFF3B2E1A; }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> SOURCE_DRILL_FLUID = FLUIDS.register("drill_fluid",
            () -> new ForgeFlowingFluid.Source(ModFluids.DRILL_FLUID_PROPERTIES));

    public static final RegistryObject<FlowingFluid> FLOWING_DRILL_FLUID = FLUIDS.register("drill_fluid_flowing",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.DRILL_FLUID_PROPERTIES));

    private static final ForgeFlowingFluid.Properties DRILL_FLUID_PROPERTIES = new ForgeFlowingFluid.Properties(
            DRILL_FLUID_TYPE, SOURCE_DRILL_FLUID, FLOWING_DRILL_FLUID)
            .bucket(ForgeRegistries.ITEMS.getDelegateOrThrow(net.minecraft.world.item.Items.AIR));

    public static final RegistryObject<FluidType> MOLTEN_AMETHYST_FLUID_TYPE = FLUID_TYPES.register("molten_amethyst_fluid",
            () -> new FluidType(FluidType.Properties.create().density(2000).viscosity(3000).temperature(1200).lightLevel(5)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation STILL = new ResourceLocation("block/water_still");
                        private static final ResourceLocation FLOW  = new ResourceLocation("block/water_flow");

                        @Override public ResourceLocation getStillTexture()   { return STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
                        @Override public int getTintColor()                    { return 0xFF9933FF; }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> SOURCE_MOLTEN_AMETHYST = FLUIDS.register("molten_amethyst",
            () -> new ForgeFlowingFluid.Source(ModFluids.MOLTEN_AMETHYST_PROPERTIES));

    public static final RegistryObject<FlowingFluid> FLOWING_MOLTEN_AMETHYST = FLUIDS.register("molten_amethyst_flowing",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.MOLTEN_AMETHYST_PROPERTIES));

    private static final ForgeFlowingFluid.Properties MOLTEN_AMETHYST_PROPERTIES = new ForgeFlowingFluid.Properties(
            MOLTEN_AMETHYST_FLUID_TYPE, SOURCE_MOLTEN_AMETHYST, FLOWING_MOLTEN_AMETHYST)
            .bucket(ForgeRegistries.ITEMS.getDelegateOrThrow(net.minecraft.world.item.Items.AIR));

    // Superfluid condensate: the top of the cryo branch. Viscosity is almost zero (that is the point
    // of superfluidity), density like a liquefied gas, temperature around 4 K.
    // Almost white with a blue tone: next to the teal coolant it reads as "so
    // cold it lost its colour", and it is not confused with any of the four taken fluids.
    public static final RegistryObject<FluidType> SUPERFLUID_CONDENSATE_FLUID_TYPE = FLUID_TYPES.register("superfluid_condensate_fluid",
            () -> new FluidType(FluidType.Properties.create().density(200).viscosity(100).temperature(4)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation STILL = new ResourceLocation("block/water_still");
                        private static final ResourceLocation FLOW  = new ResourceLocation("block/water_flow");

                        @Override public ResourceLocation getStillTexture()   { return STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
                        @Override public int getTintColor()                    { return 0xFFCCF2FF; }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> SOURCE_SUPERFLUID_CONDENSATE = FLUIDS.register("superfluid_condensate",
            () -> new ForgeFlowingFluid.Source(ModFluids.SUPERFLUID_CONDENSATE_PROPERTIES));

    public static final RegistryObject<FlowingFluid> FLOWING_SUPERFLUID_CONDENSATE = FLUIDS.register("superfluid_condensate_flowing",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.SUPERFLUID_CONDENSATE_PROPERTIES));

    private static final ForgeFlowingFluid.Properties SUPERFLUID_CONDENSATE_PROPERTIES = new ForgeFlowingFluid.Properties(
            SUPERFLUID_CONDENSATE_FLUID_TYPE, SOURCE_SUPERFLUID_CONDENSATE, FLOWING_SUPERFLUID_CONDENSATE)
            .bucket(ForgeRegistries.ITEMS.getDelegateOrThrow(net.minecraft.world.item.Items.AIR));

    // Molten lead: the first fluid with its own texture rather than recoloured water:
    // opaque silvery metal with highlights and dross (generator tools/gui_gen/build_molten_lead.ps1),
    // so the tint is white. Lead melts around 600 K, it has no reason to glow.
    public static final RegistryObject<FluidType> MOLTEN_LEAD_FLUID_TYPE = FLUID_TYPES.register("molten_lead_fluid",
            () -> new FluidType(FluidType.Properties.create().density(3000).viscosity(2500).temperature(600)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation STILL = new ResourceLocation(IndustrialCore.MODID, "block/molten_lead_still");
                        private static final ResourceLocation FLOW  = new ResourceLocation(IndustrialCore.MODID, "block/molten_lead_flow");

                        @Override public ResourceLocation getStillTexture()   { return STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
                        @Override public int getTintColor()                    { return 0xFFFFFFFF; }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> SOURCE_MOLTEN_LEAD = FLUIDS.register("molten_lead",
            () -> new ForgeFlowingFluid.Source(ModFluids.MOLTEN_LEAD_PROPERTIES));

    public static final RegistryObject<FlowingFluid> FLOWING_MOLTEN_LEAD = FLUIDS.register("molten_lead_flowing",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.MOLTEN_LEAD_PROPERTIES));

    private static final ForgeFlowingFluid.Properties MOLTEN_LEAD_PROPERTIES = new ForgeFlowingFluid.Properties(
            MOLTEN_LEAD_FLUID_TYPE, SOURCE_MOLTEN_LEAD, FLOWING_MOLTEN_LEAD)
            .bucket(ForgeRegistries.ITEMS.getDelegateOrThrow(net.minecraft.world.item.Items.AIR));

    // E7: mechanical alloy, molten lead with steel; the first step of the ladder, the assembler builds late machines with it.
    // The four E7 fluids use the molten lead texture recoloured to their own tone (tools/gui_gen/fluids_e7/build.js), so the tint is white.
    public static final RegistryObject<FluidType> MECHANICAL_ALLOY_FLUID_TYPE = FLUID_TYPES.register("mechanical_alloy_fluid",
            () -> new FluidType(FluidType.Properties.create().density(3000).viscosity(4000).temperature(900).lightLevel(3)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation STILL = new ResourceLocation(IndustrialCore.MODID, "block/mechanical_alloy_still");
                        private static final ResourceLocation FLOW  = new ResourceLocation(IndustrialCore.MODID, "block/mechanical_alloy_flow");

                        @Override public ResourceLocation getStillTexture()   { return STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
                        @Override public int getTintColor()                    { return 0xFFFFFFFF; }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> SOURCE_MECHANICAL_ALLOY = FLUIDS.register("mechanical_alloy",
            () -> new ForgeFlowingFluid.Source(ModFluids.MECHANICAL_ALLOY_PROPERTIES));

    public static final RegistryObject<FlowingFluid> FLOWING_MECHANICAL_ALLOY = FLUIDS.register("mechanical_alloy_flowing",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.MECHANICAL_ALLOY_PROPERTIES));

    private static final ForgeFlowingFluid.Properties MECHANICAL_ALLOY_PROPERTIES = new ForgeFlowingFluid.Properties(
            MECHANICAL_ALLOY_FLUID_TYPE, SOURCE_MECHANICAL_ALLOY, FLOWING_MECHANICAL_ALLOY)
            .bucket(ForgeRegistries.ITEMS.getDelegateOrThrow(net.minecraft.world.item.Items.AIR));

    // E7: photonic melt, an alloy with a photonic lens and crystal, step 2
    public static final RegistryObject<FluidType> PHOTONIC_MELT_FLUID_TYPE = FLUID_TYPES.register("photonic_melt_fluid",
            () -> new FluidType(FluidType.Properties.create().density(2200).viscosity(2000).temperature(1100).lightLevel(8)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation STILL = new ResourceLocation(IndustrialCore.MODID, "block/photonic_melt_still");
                        private static final ResourceLocation FLOW  = new ResourceLocation(IndustrialCore.MODID, "block/photonic_melt_flow");

                        @Override public ResourceLocation getStillTexture()   { return STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
                        @Override public int getTintColor()                    { return 0xFFFFFFFF; }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> SOURCE_PHOTONIC_MELT = FLUIDS.register("photonic_melt",
            () -> new ForgeFlowingFluid.Source(ModFluids.PHOTONIC_MELT_PROPERTIES));

    public static final RegistryObject<FlowingFluid> FLOWING_PHOTONIC_MELT = FLUIDS.register("photonic_melt_flowing",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.PHOTONIC_MELT_PROPERTIES));

    private static final ForgeFlowingFluid.Properties PHOTONIC_MELT_PROPERTIES = new ForgeFlowingFluid.Properties(
            PHOTONIC_MELT_FLUID_TYPE, SOURCE_PHOTONIC_MELT, FLOWING_PHOTONIC_MELT)
            .bucket(ForgeRegistries.ITEMS.getDelegateOrThrow(net.minecraft.world.item.Items.AIR));

    // E7: cryoplasma, step 3, superconductor and cryo crystal in a melt
    public static final RegistryObject<FluidType> CRYOPLASMA_FLUID_TYPE = FLUID_TYPES.register("cryoplasma_fluid",
            () -> new FluidType(FluidType.Properties.create().density(800).viscosity(300).temperature(20).lightLevel(6)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation STILL = new ResourceLocation(IndustrialCore.MODID, "block/cryoplasma_still");
                        private static final ResourceLocation FLOW  = new ResourceLocation(IndustrialCore.MODID, "block/cryoplasma_flow");

                        @Override public ResourceLocation getStillTexture()   { return STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
                        @Override public int getTintColor()                    { return 0xFFFFFFFF; }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> SOURCE_CRYOPLASMA = FLUIDS.register("cryoplasma",
            () -> new ForgeFlowingFluid.Source(ModFluids.CRYOPLASMA_PROPERTIES));

    public static final RegistryObject<FlowingFluid> FLOWING_CRYOPLASMA = FLUIDS.register("cryoplasma_flowing",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.CRYOPLASMA_PROPERTIES));

    private static final ForgeFlowingFluid.Properties CRYOPLASMA_PROPERTIES = new ForgeFlowingFluid.Properties(
            CRYOPLASMA_FLUID_TYPE, SOURCE_CRYOPLASMA, FLOWING_CRYOPLASMA)
            .bucket(ForgeRegistries.ITEMS.getDelegateOrThrow(net.minecraft.world.item.Items.AIR));

    // E7: Voltex Essence, the top of the ladder, goes into the Heart of Voltex through the late magnet port
    public static final RegistryObject<FluidType> VOLTEX_ESSENCE_FLUID_TYPE = FLUID_TYPES.register("voltex_essence_fluid",
            () -> new FluidType(FluidType.Properties.create().density(1500).viscosity(1500).temperature(3000).lightLevel(12)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation STILL = new ResourceLocation(IndustrialCore.MODID, "block/voltex_essence_still");
                        private static final ResourceLocation FLOW  = new ResourceLocation(IndustrialCore.MODID, "block/voltex_essence_flow");

                        @Override public ResourceLocation getStillTexture()   { return STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return FLOW;  }
                        @Override public int getTintColor()                    { return 0xFFFFFFFF; }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> SOURCE_VOLTEX_ESSENCE = FLUIDS.register("voltex_essence",
            () -> new ForgeFlowingFluid.Source(ModFluids.VOLTEX_ESSENCE_PROPERTIES));

    public static final RegistryObject<FlowingFluid> FLOWING_VOLTEX_ESSENCE = FLUIDS.register("voltex_essence_flowing",
            () -> new ForgeFlowingFluid.Flowing(ModFluids.VOLTEX_ESSENCE_PROPERTIES));

    private static final ForgeFlowingFluid.Properties VOLTEX_ESSENCE_PROPERTIES = new ForgeFlowingFluid.Properties(
            VOLTEX_ESSENCE_FLUID_TYPE, SOURCE_VOLTEX_ESSENCE, FLOWING_VOLTEX_ESSENCE)
            .bucket(ForgeRegistries.ITEMS.getDelegateOrThrow(net.minecraft.world.item.Items.AIR));

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
