package com.jykito.industrialcore.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cryo chamber glass panel.
 * <p>
 * ⚠ We do NOT extend {@code IronBarsBlock}: the vanilla pane has "attach to any
 * solid block" hard-wired, and the panel would stick to stone, other mods' machines and plain glass.
 * Here connection is a whitelist: only the chamber's own blocks (see {@link #connects}).
 * <p>
 * The panel shape depends on its neighbours, so {@code getShape} is called by collision, raycasts and
 * pathfinding many times per frame; all 16 combinations are precomputed in {@link #SHAPES}.
 */
public class CryoGlassBlock extends Block {

    /**
     * Axis of a single panel. With no neighbours there is nothing to orient by, so we remember
     * where the player was looking when placing it and turn the panel to face them.
     * As soon as a neighbour appears, the connection sets the orientation, not this property.
     */
    public static final EnumProperty<Direction.Axis> AXIS = EnumProperty.create(
            "axis", Direction.Axis.class, Direction.Axis.X, Direction.Axis.Z);

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");

    /**
     * Vertical joints. They do not change the geometry, only the texture: when a panel stands on a panel,
     * the seam between them must disappear. That is why they are separate from the four horizontal ones.
     */
    public static final BooleanProperty UP   = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = new EnumMap<>(Direction.class);
    static {
        PROPERTY_BY_DIRECTION.put(Direction.NORTH, NORTH);
        PROPERTY_BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        PROPERTY_BY_DIRECTION.put(Direction.EAST,  EAST);
        PROPERTY_BY_DIRECTION.put(Direction.WEST,  WEST);
        PROPERTY_BY_DIRECTION.put(Direction.UP,    UP);
        PROPERTY_BY_DIRECTION.put(Direction.DOWN,  DOWN);
    }

    /** Mask bit order: north, south, east, west. */
    private static final Direction[] HORIZONTAL = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

    /** Model slab: 16x16x2 across the block. While there is one model; later the shapes will split by variant. */
    private static final VoxelShape PANEL_NS = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape PANEL_EW = Block.box(7, 0, 0, 9, 16, 16);

    /**
     * A diagonal panel cannot be expressed as a shape: {@code VoxelShape} only knows axis-aligned boxes,
     * the engine has no diagonals at all. So the diagonal is built as a STAIRCASE of small boxes,
     * otherwise at a corner the selection looks like an L and does not match what you see.
     * <p>
     * Base case: the NORTH-EAST corner, a segment from the middle of the north face (8,0) to the middle
     * of the east face (16,8), that is along the line {@code z = x - 8}, 2 thick across.
     */
    private static VoxelShape cornerNE() {
        VoxelShape shape = Shapes.empty();
        final double halfBand = Math.sqrt(2);           // 2 across the slope = 2.83 along Z, half of that
        for (int i = 0; i < 8; i++) {
            double x0 = 8 + i, x1 = x0 + 1;
            double zc = (x0 + x1) / 2 - 8;              // centre of the strip at this step
            double z0 = Math.max(0, zc - halfBand), z1 = Math.min(16, zc + halfBand);
            shape = Shapes.or(shape, Block.box(x0, 0, z0, x1, 16, z1));
        }
        return shape;
    }

    /** Rotate the shape 90° clockwise (top view): (x,z) -> (16-z, x). */
    private static VoxelShape rotateCW(VoxelShape src) {
        VoxelShape[] out = { Shapes.empty() };
        src.forAllBoxes((x0, y0, z0, x1, y1, z1) ->
                out[0] = Shapes.or(out[0], Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1)));
        return out[0];
    }

    private static final VoxelShape[] SHAPES = new VoxelShape[16];
    static {
        // corners: index 0 = north-east, then clockwise, as in the blockstate
        VoxelShape[] corner = new VoxelShape[4];
        corner[0] = cornerNE();
        for (int i = 1; i < 4; i++) corner[i] = rotateCW(corner[i - 1]);

        for (int mask = 0; mask < 16; mask++) {
            boolean n = (mask & 1) != 0, s = (mask & 2) != 0, e = (mask & 4) != 0, w = (mask & 8) != 0;
            boolean ns = n || s, ew = e || w;
            VoxelShape shape;

            if (ns && ew) {
                // adjacent pair = corner, same orientation as the model in the blockstate
                if (n && e)      shape = corner[0];
                else if (e && s) shape = corner[1];
                else if (s && w) shape = corner[2];
                else             shape = corner[3];
            } else if (ns) {
                shape = PANEL_EW;                        // wall runs north-south
            } else if (ew) {
                shape = PANEL_NS;                        // wall runs east-west
            } else {
                shape = PANEL_NS;                        // single panel
            }
            SHAPES[mask] = shape.optimize();
        }
    }

    public CryoGlassBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false)
                .setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        for (int i = 0; i < HORIZONTAL.length; i++)
            if (state.getValue(PROPERTY_BY_DIRECTION.get(HORIZONTAL[i]))) mask |= 1 << i;
        // a single panel gets no shape from connections, so take it from the axis the player placed it on
        if (mask == 0) return state.getValue(AXIS) == Direction.Axis.X ? PANEL_EW : PANEL_NS;
        return SHAPES[mask];
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Place the panel facing the player: its plane is across the view, so its axis matches
        // the view direction axis. Looking north or south means the panel runs east-west.
        BlockState state = defaultBlockState()
                .setValue(AXIS, ctx.getHorizontalDirection().getAxis());
        for (Direction d : Direction.values())
            state = state.setValue(PROPERTY_BY_DIRECTION.get(d), connects(ctx.getLevel(), ctx.getClickedPos(), d));
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty prop = PROPERTY_BY_DIRECTION.get(dir);
        return prop == null ? state : state.setValue(prop, connects(level, pos, dir));
    }

    /**
     * Whitelist: the panel attaches ONLY to cryo chamber blocks.
     * ⚠ Add the structural block, the inner refrigerator and the core here when they exist,
     * otherwise a panel at the chamber wall will not see the corner and will not turn diagonal.
     */
    private static boolean connects(BlockGetter level, BlockPos pos, Direction dir) {
        return level.getBlockState(pos.relative(dir)).getBlock() instanceof CryoGlassBlock;
    }
}
