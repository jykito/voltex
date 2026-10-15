package com.jykito.industrialcore.block.custom;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Shapes of connected blocks (cable, heat pipe, hub): a cube in the centre plus an arm to every
 * connected face.
 * <p>
 * getShape is called by collision, the crosshair ray, pathfinding and neighbour updates, so very often,
 * and building the shape with Shapes.or every time is expensive. There are only 64 combinations, so all of them
 * are computed once when the block class loads.
 * <p>
 * Sizes in block pixels (0..16). Arms are symmetric: cross-section {@code armMin..armMax},
 * length from the block face inwards {@code armLen}.
 */
public final class ConnectedShapes {

    private final VoxelShape[] shapes = new VoxelShape[64];

    public ConnectedShapes(double coreMin, double coreMax, double armMin, double armMax, double armLen) {
        this(coreMin, coreMax, armMin, armMax, armLen, coreMin, coreMax);
    }

    /**
     * With a separate hub: if the connections form a straight run (exactly two opposite sides),
     * the centre is {@code coreMin..coreMax}, otherwise (fork, corner, end, single) the hub is {@code hubMin..hubMax}.
     */
    public ConnectedShapes(double coreMin, double coreMax, double armMin, double armMax, double armLen, double hubMin, double hubMax) {
        VoxelShape core = Block.box(coreMin, coreMin, coreMin, coreMax, coreMax, coreMax);
        VoxelShape hub = Block.box(hubMin, hubMin, hubMin, hubMax, hubMax, hubMax);
        VoxelShape[] arms = new VoxelShape[6]; // index = Direction.get3DDataValue()
        arms[Direction.DOWN.get3DDataValue()]  = Block.box(armMin, 0, armMin, armMax, armLen, armMax);
        arms[Direction.UP.get3DDataValue()]    = Block.box(armMin, 16 - armLen, armMin, armMax, 16, armMax);
        arms[Direction.NORTH.get3DDataValue()] = Block.box(armMin, armMin, 0, armMax, armMax, armLen);
        arms[Direction.SOUTH.get3DDataValue()] = Block.box(armMin, armMin, 16 - armLen, armMax, armMax, 16);
        arms[Direction.WEST.get3DDataValue()]  = Block.box(0, armMin, armMin, armLen, armMax, armMax);
        arms[Direction.EAST.get3DDataValue()]  = Block.box(16 - armLen, armMin, armMin, 16, armMax, armMax);

        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = isStraight(mask) ? core : hub;
            for (int i = 0; i < 6; i++)
                if ((mask & (1 << i)) != 0) shape = Shapes.or(shape, arms[i]);
            shapes[mask] = shape.optimize();   // simplify once, every later check is cheaper
        }
    }

    /** Exactly two opposite sides: down+up, north+south or west+east (bits by get3DDataValue). */
    private static boolean isStraight(int mask) {
        return mask == 0b000011 || mask == 0b001100 || mask == 0b110000;
    }

    /** Shape from the connections in the blockstate. Other properties (heat, water) are not part of the mask. */
    public VoxelShape get(BlockState state, Map<Direction, BooleanProperty> connections) {
        int mask = 0;
        for (Map.Entry<Direction, BooleanProperty> e : connections.entrySet())
            if (state.getValue(e.getValue())) mask |= 1 << e.getKey().get3DDataValue();
        return shapes[mask];
    }
}
