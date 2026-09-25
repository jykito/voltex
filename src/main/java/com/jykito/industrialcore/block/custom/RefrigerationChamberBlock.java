package com.jykito.industrialcore.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Холодильная камера криокомплекса.
 * <p>
 * Все четыре бока одинаковые, поэтому поворот не нужен — есть только флаг работы,
 * по которому бока меняются на анимированные. Механика подключится вместе с ядром.
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
