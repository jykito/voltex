package com.jykito.industrialcore.block.custom;

import com.jykito.industrialcore.block.entity.ChargingPlateBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Charging plate: a 4 px pedestal with four emitter posts. Whoever stands on it gets everything they carry charged
 * from the grid. CHARGING lights the glass field and the post tips.
 */
public class ChargingPlateBlock extends BaseEntityBlock {

    public static final BooleanProperty CHARGING = BooleanProperty.create("charging");

    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 4, 16),
            box(1, 4, 1, 3, 7, 3), box(13, 4, 1, 15, 7, 3), box(1, 4, 13, 3, 7, 15), box(13, 4, 13, 15, 7, 15)).optimize();

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return com.mojang.serialization.MapCodec.unit(this); }

    public ChargingPlateBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(CHARGING, false));
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new ChargingPlateBlockEntity(pos, s); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        if (l.isClientSide) return null;
        return createTickerHelper(t, ModBlockEntities.CHARGING_PLATE_BE.get(), ChargingPlateBlockEntity::serverTick);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(CHARGING); }
}
