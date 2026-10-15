package com.jykito.industrialcore.heat;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

/** Our own heat network capability. Block entities expose it through Caps.Provider, registered in Caps.register. */
public class HeatCapability {

    public static final BlockCapability<IHeatHandler, Direction> HEAT = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, "heat"), IHeatHandler.class);
}
