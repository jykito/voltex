package com.jykito.industrialcore.compat.ae2;

import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Switch for the ME ports without AE2 references: this class always loads, the classes with the AE2 API
 * ({@link Ae2PortFactory} and the ports) only when AE2 is installed. Without AE2 an ME port is a regular port.
 */
public final class MePorts {

    private MePorts() {}

    public static final boolean AE2 = ModList.get().isLoaded("ae2");

    public static ItemFluidPortBlockEntity create(BlockPos pos, BlockState state) {
        return AE2 ? Ae2PortFactory.create(pos, state) : new ItemFluidPortBlockEntity(ModBlockEntities.ME_PORT_BE.get(), pos, state);
    }

    /** AE2 capabilities of the ME ports; a no-op without AE2. */
    public static void registerCaps(RegisterCapabilitiesEvent event) {
        if (AE2) Ae2PortFactory.registerCaps(event);
    }
}
