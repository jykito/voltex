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

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
