package com.jykito.industrialcore.block.custom.cover;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Covers for cables and heat pipes: the block hides inside any full block and keeps working as before.
 * <p>
 * The cover itself lives in the block entity ({@link CoverHolder}); the blockstate only carries {@link #COVERED},
 * so that shape, light and mining behave like a full block. Rendering swaps the model for the cover's
 * (client CoverModel). Put on: block in hand, sneak and right-click. Take off: sneak-click with the wrench,
 * the first click removes the cover, the next one the cable.
 */
public final class Covers {

    public static final BooleanProperty COVERED = BooleanProperty.create("covered");

    private Covers() {}

    /** Any plain full block without a block entity and without a fluid. */
    public static boolean isValidCover(BlockState cover, BlockGetter level, BlockPos pos) {
        return !cover.hasBlockEntity() && cover.getRenderShape() == RenderShape.MODEL && cover.getFluidState().isEmpty()
                && !cover.hasProperty(COVERED) && cover.isCollisionShapeFullBlock(level, pos);
    }

    public static boolean isCovered(BlockState state) {
        return state.hasProperty(COVERED) && state.getValue(COVERED);
    }

    /** Puts the block from {@code stack} on as a cover. PASS when it does not apply, so normal placement goes on. */
    public static InteractionResult tryApply(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) return InteractionResult.PASS;
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(COVERED) || state.getValue(COVERED)) return InteractionResult.PASS;
        BlockState cover = item.getBlock().defaultBlockState();
        if (!isValidCover(cover, level, pos) || !(level.getBlockEntity(pos) instanceof CoverHolder holder)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            holder.setCover(cover);
            BlockState covered = state.setValue(COVERED, true);
            if (covered.hasProperty(BlockStateProperties.WATERLOGGED)) covered = covered.setValue(BlockStateProperties.WATERLOGGED, false);
            level.setBlock(pos, covered, Block.UPDATE_ALL);
            SoundType sound = cover.getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1f) / 2f, sound.getPitch() * 0.8f);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Takes the cover off and returns it as an item, or EMPTY when there is none. Server side. */
    public static ItemStack remove(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isCovered(state) || !(level.getBlockEntity(pos) instanceof CoverHolder holder)) return ItemStack.EMPTY;
        BlockState cover = holder.getCover();
        holder.setCover(null);
        level.setBlock(pos, state.setValue(COVERED, false), Block.UPDATE_ALL);
        return cover == null ? ItemStack.EMPTY : new ItemStack(cover.getBlock());
    }

    /** Called from onRemove: the cover drops next to the cable's own drop. */
    public static void dropOnRemove(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CoverHolder holder && holder.getCover() != null) {
            Block.popResource(level, pos, new ItemStack(holder.getCover().getBlock()));
            holder.setCover(null);
        }
    }

    /** Creative players break the cable without getting the cover back, like any other block. */
    public static void forgetInCreative(Level level, BlockPos pos, Player player) {
        if (player.getAbilities().instabuild && level.getBlockEntity(pos) instanceof CoverHolder holder) holder.setCover(null);
    }

    public static VoxelShape shape(BlockState state, VoxelShape own) {
        return isCovered(state) ? Shapes.block() : own;
    }

    /** Mining speed of a covered block follows the cover: hiding a cable in stone does not make the stone soft. */
    public static float destroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos, float own) {
        if (isCovered(state) && level.getBlockEntity(pos) instanceof CoverHolder holder && holder.getCover() != null)
            return holder.getCover().getDestroyProgress(player, level, pos);
        return own;
    }

    public static void save(CompoundTag tag, @Nullable BlockState cover) {
        if (cover != null) tag.put("cover", NbtUtils.writeBlockState(cover));
    }

    public static @Nullable BlockState load(CompoundTag tag) {
        if (!tag.contains("cover")) return null;
        BlockState s = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("cover"));
        return s.isAir() ? null : s;
    }
}
