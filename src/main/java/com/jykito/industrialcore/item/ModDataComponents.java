package com.jykito.industrialcore.item;

import com.jykito.industrialcore.IndustrialCore;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data (in 1.20.1 it lived in the stack NBT). */
public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, IndustrialCore.MODID);

    /** FE charge of all energy items: drills, wrench, batteries, armor, weapons, amulets. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
            COMPONENTS.registerComponentType("energy", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Item mode: amulet charging, blade strike, drill. Each item gives the numbers its own meaning. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MODE =
            COMPONENTS.registerComponentType("mode", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Accumulated blast charge of the plasma armor. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BURST_CHARGE =
            COMPONENTS.registerComponentType("burst_charge", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Side of a directional upgrade (puller/pusher). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Direction>> DIRECTION =
            COMPONENTS.registerComponentType("direction", b -> b.persistent(Direction.CODEC).networkSynchronized(Direction.STREAM_CODEC));

    /** Fluid in the capsule. The name is tied to recipes: the neoforge:components ingredient with industrial_core:fluid_content. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT =
            COMPONENTS.registerComponentType("fluid_content", b -> b.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    /** Nanite strain genome (same structure as in 1.20.1, the whole tag). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> NANITE_GENOME =
            COMPONENTS.registerComponentType("nanite_genome", b -> b.persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC));

    /** Explicit tint of a nanite strain icon in the species atlas (silhouette, trait colour). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NANITE_COLOR =
            COMPONENTS.registerComponentType("nanite_color", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Logistics linker: the selected source of a link. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.jykito.industrialcore.item.custom.LogisticsLinkerItem.LinkSource>> LINK_SOURCE =
            COMPONENTS.registerComponentType("link_source", b -> b.persistent(com.jykito.industrialcore.item.custom.LogisticsLinkerItem.LinkSource.CODEC)
                    .networkSynchronized(com.jykito.industrialcore.item.custom.LogisticsLinkerItem.LinkSource.STREAM_CODEC));

    /** Structure wand: the two selection corners. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> WAND_CORNER_A =
            COMPONENTS.registerComponentType("wand_corner_a", b -> b.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> WAND_CORNER_B =
            COMPONENTS.registerComponentType("wand_corner_b", b -> b.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
