package com.jykito.industrialcore.client;

import com.jykito.industrialcore.block.custom.cover.CoverHolder;
import com.jykito.industrialcore.block.custom.cover.Covers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Cable and heat pipe model with a cover: while the block entity holds a cover, the chunk gets the cover block's
 * quads instead of the cable's. The cover is read from the block entity when the chunk is built (like
 * {@link GlassCtmModel} reads its neighbours), so no extra model-data syncing is needed.
 */
public final class CoverModel extends BakedModelWrapper<BakedModel> {

    private static final ModelProperty<BlockState> COVER = new ModelProperty<>();

    public CoverModel(BakedModel original) {
        super(original);
    }

    private static @Nullable BlockState cover(ModelData data) { return data.get(COVER); }
    private static BakedModel modelOf(BlockState cover) { return Minecraft.getInstance().getBlockRenderer().getBlockModel(cover); }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if (Covers.isCovered(state) && level.getBlockEntity(pos) instanceof CoverHolder holder && holder.getCover() != null)
            return modelData.derive().with(COVER, holder.getCover()).build();
        return super.getModelData(level, pos, state, modelData);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        BlockState cover = cover(data);
        if (cover == null) return super.getQuads(state, side, rand, data, renderType);
        BakedModel model = modelOf(cover);
        if (renderType != null && !model.getRenderTypes(cover, rand, ModelData.EMPTY).contains(renderType)) return List.of();
        return model.getQuads(cover, side, rand, ModelData.EMPTY, renderType);
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        BlockState cover = cover(data);
        return cover == null ? super.getRenderTypes(state, rand, data) : modelOf(cover).getRenderTypes(cover, rand, ModelData.EMPTY);
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        BlockState cover = cover(data);
        return cover == null ? super.getParticleIcon(data) : modelOf(cover).getParticleIcon(ModelData.EMPTY);
    }
}
