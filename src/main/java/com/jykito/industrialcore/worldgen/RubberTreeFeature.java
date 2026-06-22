package com.jykito.industrialcore.worldgen;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.custom.RubberLogBlock;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class RubberTreeFeature extends Feature<NoneFeatureConfiguration> {
    public RubberTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        RandomSource random = context.random();

        BlockState stateBelow = level.getBlockState(pos.below());
        if (!stateBelow.is(Blocks.GRASS_BLOCK) && !stateBelow.is(Blocks.DIRT) && !stateBelow.is(Blocks.PODZOL)) {
            return false;
        }

        int height = 6 + random.nextInt(3);

        for (int y = 0; y <= height + 2; y++) {
            if (!level.getBlockState(pos.above(y)).canBeReplaced()) {
                return false;
            }
        }

        for (int y = 0; y < height; y++) {
            BlockPos logPos = pos.above(y);
            BlockState logState = ModBlocks.RESIN_LOG.get().defaultBlockState();

            if (y > 0 && y < height - 1 && random.nextFloat() < 0.25f) {
                Direction randomDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                logState = logState.setValue(RubberLogBlock.LATEX_STATE, 2).setValue(RubberLogBlock.LATEX_FACING, randomDir);
            } else {
                logState = logState.setValue(RubberLogBlock.LATEX_STATE, 0);
            }
            level.setBlock(logPos, logState, 3);
        }

        BlockState leafState = ModBlocks.RESIN_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
        for (int y = height - 3; y <= height + 1; y++) {
            int radius = (y == height + 1) ? 0 : 1;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (radius == 1 && Math.abs(x) == 1 && Math.abs(z) == 1 && y >= height) continue;
                    BlockPos leafPos = pos.offset(x, y, z);
                    BlockState currentState = level.getBlockState(leafPos);
                    if (currentState.canBeReplaced() && !(currentState.getBlock() instanceof RubberLogBlock)) {
                        level.setBlock(leafPos, leafState, 3);
                    }
                }
            }
        }
        return true;
    }
}
