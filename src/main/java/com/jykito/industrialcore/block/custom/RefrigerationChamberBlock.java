package com.jykito.industrialcore.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Refrigeration chamber of the cryo complex.
 * <p>
 * All four sides are the same, so no rotation is needed; there is only a working flag
 * that swaps the sides to animated ones. The mechanics hook up together with the core.
 */
public class RefrigerationChamberBlock extends Block {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public RefrigerationChamberBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT);
    }
}
