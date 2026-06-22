package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RubberSaplingBlock extends BushBlock implements BonemealableBlock {

    public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
    protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D);

    public RubberSaplingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) {
            this.advanceTree(level, pos, state, random);
        }
    }

    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
        } else {
            this.growTree(level, pos, random);
        }
    }

    private void growTree(ServerLevel level, BlockPos pos, RandomSource random) {

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 4);

        int height = 6 + random.nextInt(3);

        for (int y = 0; y <= height + 2; y++) {
            if (!level.getBlockState(pos.above(y)).canBeReplaced()) {
                level.setBlock(pos, this.defaultBlockState(), 4);
                return;
            }
        }

        for (int y = 0; y < height; y++) {
            BlockPos logPos = pos.above(y);
            BlockState logState = ModBlocks.RESIN_LOG.get().defaultBlockState();

            if (y > 0 && y < height - 1 && random.nextFloat() < 0.25f) {
                Direction randomDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                logState = logState.setValue(RubberLogBlock.LATEX_STATE, 2)
                        .setValue(RubberLogBlock.LATEX_FACING, randomDir);
            } else {
                logState = logState.setValue(RubberLogBlock.LATEX_STATE, 0);
            }

            level.setBlock(logPos, logState, 3);
        }

        BlockState leafState = ModBlocks.RESIN_LEAVES.get().defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, false);

        for (int y = height - 3; y <= height + 1; y++) {
            int radius = (y == height + 1) ? 0 : 1;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {

                    if (radius == 1 && Math.abs(x) == 1 && Math.abs(z) == 1 && y >= height) {
                        continue;
                    }

                    BlockPos leafPos = pos.offset(x, y, z);
                    BlockState currentState = level.getBlockState(leafPos);

                    if (currentState.canBeReplaced() && !(currentState.getBlock() instanceof RubberLogBlock)) {
                        level.setBlock(leafPos, leafState, 3);
                    }
                }
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) { return true; }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return (double)level.random.nextFloat() < 0.45D; }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) { this.advanceTree(level, pos, state, random); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(STAGE); }
}
