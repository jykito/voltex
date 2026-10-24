package com.jykito.industrialcore.api;

import com.jykito.industrialcore.multiblock.WandBuild;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A multiblock core the Voltex structure wand can complete. Implement it on the core block: a click with the wand
 * calls {@link #buildWithWand} on the server, and the wand reports to the player what was placed and what is missing.
 * <p>
 * Place blocks through {@link WandBuild#place} (empty cells only; in survival it takes them from the inventory) and
 * skip cells that are already correct. Cores extending {@code LateMachineCoreBlock} need nothing: the wand builds
 * their pattern by itself.
 */
public interface WandAssemblable {
    void buildWithWand(Level level, BlockPos core, BlockState coreState, WandBuild build);
}
