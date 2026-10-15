package com.jykito.industrialcore.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Multiblock structural glass: the same joining as the casing (frame only along the outline
 * of the glazing), but transparent. Faces between neighbouring glass blocks are not drawn, like vanilla glass.
 */
public class StructuralGlassBlock extends ConnectedCasingBlock {

    public StructuralGlassBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction dir) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, dir);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
