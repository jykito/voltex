package com.jykito.industrialcore.block.custom.cover;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A block entity that can hide its block inside a cover (any full block). See {@link Covers}. */
public interface CoverHolder {
    @Nullable BlockState getCover();
    void setCover(@Nullable BlockState cover);
}
