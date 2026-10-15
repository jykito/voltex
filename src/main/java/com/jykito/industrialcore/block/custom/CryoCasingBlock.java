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
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Structural block of the cryo chamber.
 * <p>
 * The masonry must read as one solid slab with a frame ONLY along the outer perimeter. So
 * the frame is not part of the texture but twelve bars along the cube edges: an edge shows
 * if neither of its two adjacent sides has a neighbour. Inside the masonry all edges hide by themselves.
 * <p>
 * This way, instead of sixteen texture variants per face, two textures cover the whole block:
 * a seamless fill and the frame material.
 * <p>
 * The casing also <b>accepts heat on behalf of the core</b>: the core has six faces, and a deep stage needs
 * several pipe lines. Details in {@link com.jykito.industrialcore.block.entity.CryoCasingBlockEntity}.
 */
public class CryoCasingBlock extends Block implements net.minecraft.world.level.block.EntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = new EnumMap<>(Direction.class);
    static {
        PROPERTY_BY_DIRECTION.put(Direction.NORTH, NORTH);
        PROPERTY_BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        PROPERTY_BY_DIRECTION.put(Direction.EAST,  EAST);
        PROPERTY_BY_DIRECTION.put(Direction.WEST,  WEST);
        PROPERTY_BY_DIRECTION.put(Direction.UP,    UP);
        PROPERTY_BY_DIRECTION.put(Direction.DOWN,  DOWN);
    }

    public CryoCasingBlock(Properties props) {
        super(props);
        BlockState s = this.stateDefinition.any();
        for (BooleanProperty p : PROPERTY_BY_DIRECTION.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = defaultBlockState();
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
     * The frame is hidden ONLY by another casing. Glass, the core and everything else do not count as neighbours:
     * the seam between the casing and them must be outlined; the masonry is outlined only along its own contour.
     * ⚠ Add the core here when it exists; the author decided to add it separately.
     */
    private static boolean connects(BlockGetter level, BlockPos pos, Direction dir) {
        return level.getBlockState(pos.relative(dir)).getBlock() instanceof CryoCasingBlock;
    }

    /** The entity only forwards heat to the core and does not tick; the block has no ticker on purpose. */
    @Nullable
    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.jykito.industrialcore.block.entity.CryoCasingBlockEntity(pos, state);
    }
}
