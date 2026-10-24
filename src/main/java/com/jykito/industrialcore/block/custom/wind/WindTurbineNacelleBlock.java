package com.jykito.industrialcore.block.custom.wind;
import com.jykito.industrialcore.block.BlockUse;
import net.minecraft.world.ItemInteractionResult;

import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.block.entity.WindTurbineNacelleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Top of a wind turbine. The block model is the platform with rails; the nacelle turning after the wind, the rotor
 * and the anemometer are drawn by {@code client.wind.WindTurbineBER}.
 */
public class WindTurbineNacelleBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 1, 16), Block.box(4, 1, 4, 12, 15, 12));

    public WindTurbineNacelleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return com.mojang.serialization.MapCodec.unit(this); }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /**
     * Right click with a rotor puts it in (the old one goes back to the player); sneak + right click with an empty hand
     * takes the rotor out; a plain right click with an empty hand shows the state.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return BlockUse.toItemResult(use(state, level, pos, player, hand, hit));
    }

    /** Formerly Block.use (1.20.1); called from useItemOn for both hands, as before. */
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (level.getBlockEntity(pos) instanceof WindTurbineNacelleBlockEntity be) {
            boolean put = WindRotors.get(held) != null, take = held.isEmpty() && player.isShiftKeyDown() && !be.rotor().isEmpty();
            if (put || take) {
                if (!level.isClientSide) {
                    ItemStack old = be.swapRotor(put ? held : ItemStack.EMPTY);
                    if (put && !player.getAbilities().instabuild) held.shrink(1);
                    if (!old.isEmpty()) player.getInventory().placeItemBackInInventory(old);
                    level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.8f, put ? 1.1f : 0.8f);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return WindTurbine.showStatus(level, pos, player, hand);
    }

    /** The rotor falls out when the nacelle goes, by a pickaxe or a wrench. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WindTurbineNacelleBlockEntity be && !be.rotor().isEmpty())
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.rotor());
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WindTurbineNacelleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.WIND_TURBINE_NACELLE_BE.get(),
                (lvl, pos, st, be) -> be.serverTick(lvl, pos, st));
    }
}
