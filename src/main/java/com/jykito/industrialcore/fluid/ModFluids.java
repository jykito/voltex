package com.jykito.industrialcore.fluid;


import net.minecraft.core.registries.Registries;
import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES, IndustrialCore.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, IndustrialCore.MODID);

    public static final DeferredHolder<FluidType, FluidType> COOLANT_FLUID_TYPE = FLUID_TYPES.register("coolant_fluid",
            () -> new FluidType(FluidType.Properties.create().density(1500).viscosity(1000)));

    public static final DeferredHolder<Fluid, FlowingFluid> SOURCE_COOLANT = FLUIDS.register("coolant",
            () -> new BaseFlowingFluid.Source(ModFluids.COOLANT_PROPERTIES));

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_COOLANT = FLUIDS.register("coolant_flowing",
            () -> new BaseFlowingFluid.Flowing(ModFluids.COOLANT_PROPERTIES));

    private static final BaseFlowingFluid.Properties COOLANT_PROPERTIES = new BaseFlowingFluid.Properties(
            COOLANT_FLUID_TYPE, SOURCE_COOLANT, FLOWING_COOLANT)
            .bucket(() -> net.minecraft.world.item.Items.AIR);

    public static final DeferredHolder<FluidType, FluidType> HOT_COOLANT_FLUID_TYPE = FLUID_TYPES.register("hot_coolant_fluid",
            () -> new FluidType(FluidType.Properties.create().density(1500).viscosity(1000).temperature(800)));

    public static final DeferredHolder<Fluid, FlowingFluid> SOURCE_HOT_COOLANT = FLUIDS.register("hot_coolant",
            () -> new BaseFlowingFluid.Source(ModFluids.HOT_COOLANT_PROPERTIES));

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_HOT_COOLANT = FLUIDS.register("hot_coolant_flowing",
            () -> new BaseFlowingFluid.Flowing(ModFluids.HOT_COOLANT_PROPERTIES));

    private static final BaseFlowingFluid.Properties HOT_COOLANT_PROPERTIES = new BaseFlowingFluid.Properties(
            HOT_COOLANT_FLUID_TYPE, SOURCE_HOT_COOLANT, FLOWING_HOT_COOLANT)
            .bucket(() -> net.minecraft.world.item.Items.AIR);

    public static final DeferredHolder<FluidType, FluidType> DRILL_FLUID_TYPE = FLUID_TYPES.register("drill_fluid",
            () -> new FluidType(FluidType.Properties.create().density(1200).viscosity(1500)));

    public static final DeferredHolder<Fluid, FlowingFluid> SOURCE_DRILL_FLUID = FLUIDS.register("drill_fluid",
            () -> new BaseFlowingFluid.Source(ModFluids.DRILL_FLUID_PROPERTIES));

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_DRILL_FLUID = FLUIDS.register("drill_fluid_flowing",
            () -> new BaseFlowingFluid.Flowing(ModFluids.DRILL_FLUID_PROPERTIES));

    private static final BaseFlowingFluid.Properties DRILL_FLUID_PROPERTIES = new BaseFlowingFluid.Properties(
            DRILL_FLUID_TYPE, SOURCE_DRILL_FLUID, FLOWING_DRILL_FLUID)
            .bucket(() -> net.minecraft.world.item.Items.AIR);

    public static final DeferredHolder<FluidType, FluidType> MOLTEN_AMETHYST_FLUID_TYPE = FLUID_TYPES.register("molten_amethyst_fluid",
            () -> new FluidType(FluidType.Properties.create().density(2000).viscosity(3000).temperature(1200).lightLevel(5)));

    public static final DeferredHolder<Fluid, FlowingFluid> SOURCE_MOLTEN_AMETHYST = FLUIDS.register("molten_amethyst",
            () -> new BaseFlowingFluid.Source(ModFluids.MOLTEN_AMETHYST_PROPERTIES));

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_MOLTEN_AMETHYST = FLUIDS.register("molten_amethyst_flowing",
            () -> new BaseFlowingFluid.Flowing(ModFluids.MOLTEN_AMETHYST_PROPERTIES));

    private static final BaseFlowingFluid.Properties MOLTEN_AMETHYST_PROPERTIES = new BaseFlowingFluid.Properties(
            MOLTEN_AMETHYST_FLUID_TYPE, SOURCE_MOLTEN_AMETHYST, FLOWING_MOLTEN_AMETHYST)
            .bucket(() -> net.minecraft.world.item.Items.AIR);

    // Superfluid condensate: the top of the cryo branch. Viscosity is almost zero (that is the point
    // of superfluidity), density like a liquefied gas, temperature around 4 K.
    // Almost white with a blue tone: next to the teal coolant it reads as "so
    // cold it lost its colour", and it is not confused with any of the four taken fluids.
    public static final DeferredHolder<FluidType, FluidType> SUPERFLUID_CONDENSATE_FLUID_TYPE = FLUID_TYPES.register("superfluid_condensate_fluid",
            () -> new FluidType(FluidType.Properties.create().density(200).viscosity(100).temperature(4)));

    public static final DeferredHolder<Fluid, FlowingFluid> SOURCE_SUPERFLUID_CONDENSATE = FLUIDS.register("superfluid_condensate",
            () -> new BaseFlowingFluid.Source(ModFluids.SUPERFLUID_CONDENSATE_PROPERTIES));

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_SUPERFLUID_CONDENSATE = FLUIDS.register("superfluid_condensate_flowing",
            () -> new BaseFlowingFluid.Flowing(ModFluids.SUPERFLUID_CONDENSATE_PROPERTIES));

    private static final BaseFlowingFluid.Properties SUPERFLUID_CONDENSATE_PROPERTIES = new BaseFlowingFluid.Properties(
            SUPERFLUID_CONDENSATE_FLUID_TYPE, SOURCE_SUPERFLUID_CONDENSATE, FLOWING_SUPERFLUID_CONDENSATE)
            .bucket(() -> net.minecraft.world.item.Items.AIR);

    // Molten lead: the first fluid with its own texture rather than recoloured water:
    // opaque silvery metal with highlights and dross (generator tools/gui_gen/build_molten_lead.ps1),
    // so the tint is white. Lead melts around 600 K, it has no reason to glow.
    public static final DeferredHolder<FluidType, FluidType> MOLTEN_LEAD_FLUID_TYPE = FLUID_TYPES.register("molten_lead_fluid",
            () -> new FluidType(FluidType.Properties.create().density(3000).viscosity(2500).temperature(600)));

    public static final DeferredHolder<Fluid, FlowingFluid> SOURCE_MOLTEN_LEAD = FLUIDS.register("molten_lead",
            () -> new BaseFlowingFluid.Source(ModFluids.MOLTEN_LEAD_PROPERTIES));

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_MOLTEN_LEAD = FLUIDS.register("molten_lead_flowing",
            () -> new BaseFlowingFluid.Flowing(ModFluids.MOLTEN_LEAD_PROPERTIES));

    private static final BaseFlowingFluid.Properties MOLTEN_LEAD_PROPERTIES = new BaseFlowingFluid.Properties(
            MOLTEN_LEAD_FLUID_TYPE, SOURCE_MOLTEN_LEAD, FLOWING_MOLTEN_LEAD)
            .bucket(() -> net.minecraft.world.item.Items.AIR);

    // E7: mechanical alloy, molten lead with steel; the first step of the ladder, the assembler builds late machines with it.
    // The four E7 fluids use the molten lead texture recoloured to their own tone (tools/gui_gen/fluids_e7/build.js), so the tint is white.
    public static final DeferredHolder<FluidType, FluidType> MECHANICAL_ALLOY_FLUID_TYPE = FLUID_TYPES.register("mechanical_alloy_fluid",
            () -> new FluidType(FluidType.Properties.create().density(3000).viscosity(4000).temperature(900).lightLevel(3)));

    public static final DeferredHolder<Fluid, FlowingFluid> SOURCE_MECHANICAL_ALLOY = FLUIDS.register("mechanical_alloy",
            () -> new BaseFlowingFluid.Source(ModFluids.MECHANICAL_ALLOY_PROPERTIES));

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_MECHANICAL_ALLOY = FLUIDS.register("mechanical_alloy_flowing",
            () -> new BaseFlowingFluid.Flowing(ModFluids.MECHANICAL_ALLOY_PROPERTIES));

    private static final BaseFlowingFluid.Properties MECHANICAL_ALLOY_PROPERTIES = new BaseFlowingFluid.Properties(
            MECHANICAL_ALLOY_FLUID_TYPE, SOURCE_MECHANICAL_ALLOY, FLOWING_MECHANICAL_ALLOY)
            .bucket(() -> net.minecraft.world.item.Items.AIR);

    // E7: photonic melt, an alloy with a photonic lens and crystal, step 2
    public static final DeferredHolder<FluidType, FluidType> PHOTONIC_MELT_FLUID_TYPE = FLUID_TYPES.register("photonic_melt_fluid",
            () -> new FluidType(FluidType.Properties.create().density(2200).viscosity(2000).temperature(1100).lightLevel(8)));

    public static final DeferredHolder<Fluid, FlowingFluid> SOURCE_PHOTONIC_MELT = FLUIDS.register("photonic_melt",
            () -> new BaseFlowingFluid.Source(ModFluids.PHOTONIC_MELT_PROPERTIES));

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_PHOTONIC_MELT = FLUIDS.register("photonic_melt_flowing",
            () -> new BaseFlowingFluid.Flowing(ModFluids.PHOTONIC_MELT_PROPERTIES));

    private static final BaseFlowingFluid.Properties PHOTONIC_MELT_PROPERTIES = new BaseFlowingFluid.Properties(
            PHOTONIC_MELT_FLUID_TYPE, SOURCE_PHOTONIC_MELT, FLOWING_PHOTONIC_MELT)
            .bucket(() -> net.minecraft.world.item.Items.AIR);

    // E7: cryoplasma, step 3, superconductor and cryo crystal in a melt
    public static final DeferredHolder<FluidType, FluidType> CRYOPLASMA_FLUID_TYPE = FLUID_TYPES.register("cryoplasma_fluid",
            () -> new FluidType(FluidType.Properties.create().density(800).viscosity(300).temperature(20).lightLevel(6)));

    public static final DeferredHolder<Fluid, FlowingFluid> SOURCE_CRYOPLASMA = FLUIDS.register("cryoplasma",
            () -> new BaseFlowingFluid.Source(ModFluids.CRYOPLASMA_PROPERTIES));

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_CRYOPLASMA = FLUIDS.register("cryoplasma_flowing",
            () -> new BaseFlowingFluid.Flowing(ModFluids.CRYOPLASMA_PROPERTIES));

    private static final BaseFlowingFluid.Properties CRYOPLASMA_PROPERTIES = new BaseFlowingFluid.Properties(
            CRYOPLASMA_FLUID_TYPE, SOURCE_CRYOPLASMA, FLOWING_CRYOPLASMA)
            .bucket(() -> net.minecraft.world.item.Items.AIR);

    // E7: Voltex Essence, the top of the ladder, goes into the Heart of Voltex through the late magnet port
    public static final DeferredHolder<FluidType, FluidType> VOLTEX_ESSENCE_FLUID_TYPE = FLUID_TYPES.register("voltex_essence_fluid",
            () -> new FluidType(FluidType.Properties.create().density(1500).viscosity(1500).temperature(3000).lightLevel(12)));

    public static final DeferredHolder<Fluid, FlowingFluid> SOURCE_VOLTEX_ESSENCE = FLUIDS.register("voltex_essence",
            () -> new BaseFlowingFluid.Source(ModFluids.VOLTEX_ESSENCE_PROPERTIES));

    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_VOLTEX_ESSENCE = FLUIDS.register("voltex_essence_flowing",
            () -> new BaseFlowingFluid.Flowing(ModFluids.VOLTEX_ESSENCE_PROPERTIES));

    private static final BaseFlowingFluid.Properties VOLTEX_ESSENCE_PROPERTIES = new BaseFlowingFluid.Properties(
            VOLTEX_ESSENCE_FLUID_TYPE, SOURCE_VOLTEX_ESSENCE, FLOWING_VOLTEX_ESSENCE)
            .bucket(() -> net.minecraft.world.item.Items.AIR);

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
