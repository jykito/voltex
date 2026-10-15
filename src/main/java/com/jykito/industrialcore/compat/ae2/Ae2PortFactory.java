package com.jykito.industrialcore.compat.ae2;

import appeng.api.AECapabilities;
import appeng.api.networking.IInWorldGridNodeHost;
import com.jykito.industrialcore.block.custom.MePortBlock;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Creates an ME port by block kind. Loaded only when AE2 is installed (see {@link MePorts}). */
final class Ae2PortFactory {

    private Ae2PortFactory() {}

    static ItemFluidPortBlockEntity create(BlockPos pos, BlockState state) {
        MePortBlock.MeKind kind = state.getBlock() instanceof MePortBlock b ? b.meKind() : MePortBlock.MeKind.INPUT;
        return switch (kind) {
            case INPUT -> new MeInputPortBlockEntity(pos, state);
            case OUTPUT -> new MeOutputPortBlockEntity(pos, state);
            case PATTERN -> new MePatternPortBlockEntity(pos, state);
        };
    }

    /**
     * 1.21: AE2 finds an in-world node host through a block capability, not through instanceof as in 1.20.1.
     * Without it a cable next to an ME port never connects and the port stays "no network".
     */
    static void registerCaps(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ModBlockEntities.ME_PORT_BE.get(),
                (be, ctx) -> be instanceof IInWorldGridNodeHost host ? host : null);
    }
}
