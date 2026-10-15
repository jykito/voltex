package com.jykito.industrialcore.compat.ae2;

import com.jykito.industrialcore.block.custom.MePortBlock;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

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
}
