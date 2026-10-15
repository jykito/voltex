package com.jykito.industrialcore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Structural glass: frame node in an inner corner. The blockstate only knows the 6 side neighbours, so on the corner
 * block of an L-shaped build the frame was removed from both connected sides and the corner between them stayed empty.
 * The wrapper looks at the diagonal neighbour in the plane of each face and, if both sides of the corner are connected and the diagonal
 * is not glass, swaps the face texture to the "..._connections_nub_&lt;corners&gt;" variant (generator
 * tools/gui_gen/glass_fix/build_glass_fix.ps1). A solid wall stays whole. Only the UVs change, not the geometry.
 */
public final class GlassCtmModel extends BakedModelWrapper<BakedModel> {

    /** 4 bits per face (index Direction.get3DDataValue()): whether there is glass diagonally in the corners ru, rd, lu, ld. */
    private static final ModelProperty<Integer> DIAG = new ModelProperty<>();
    private static final String[] CORNERS = {"ru", "rd", "lu", "ld"};

    /** Sides of the face texture in the world: {right, left, up, down}, as in the 64 glass models. */
    private static final Direction[][] SIDES = new Direction[6][];
    static {
        SIDES[Direction.DOWN.get3DDataValue()]  = new Direction[]{Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
        SIDES[Direction.UP.get3DDataValue()]    = new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH};
        SIDES[Direction.NORTH.get3DDataValue()] = new Direction[]{Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN};
        SIDES[Direction.SOUTH.get3DDataValue()] = new Direction[]{Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};
        SIDES[Direction.WEST.get3DDataValue()]  = new Direction[]{Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN};
        SIDES[Direction.EAST.get3DDataValue()]  = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN};
    }

    /** Texture variants found so far (chunks are built on several threads). The wrapper is recreated on resource reload. */
    private final Map<String, TextureAtlasSprite> sprites = new ConcurrentHashMap<>();

    public GlassCtmModel(BakedModel original) {
        super(original);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        Block block = state.getBlock();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int bits = 0;
        for (Direction face : Direction.values()) {
            Direction[] s = SIDES[face.get3DDataValue()];
            for (int c = 0; c < 4; c++) {
                p.setWithOffset(pos, s[c < 2 ? 0 : 1]).move(s[c % 2 == 0 ? 2 : 3]);
                if (level.getBlockState(p).is(block)) bits |= 1 << (face.get3DDataValue() * 4 + c);
            }
        }
        return modelData.derive().with(DIAG, bits).build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        Integer diag = data.get(DIAG);
        if (diag == null || quads.isEmpty()) return quads;
        List<BakedQuad> out = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad q = quads.get(i), r = withNubs(q, diag);
            if (r != q) {
                if (out == null) out = new ArrayList<>(quads);
                out.set(i, r);
            }
        }
        return out == null ? quads : out;
    }

    private BakedQuad withNubs(BakedQuad q, int diag) {
        ResourceLocation name = q.getSprite().contents().name();
        String path = name.getPath();
        if (!path.endsWith("_connections")) return q;
        int end = path.length() - "_connections".length();
        String mask = path.substring(path.lastIndexOf('_', end - 1) + 1, end);
        int bits = (diag >> (q.getDirection().get3DDataValue() * 4)) & 15;
        StringBuilder nubs = new StringBuilder();
        for (int c = 0; c < 4; c++) {
            String corner = CORNERS[c];
            boolean bothSides = mask.indexOf(corner.charAt(0)) >= 0 && mask.indexOf(corner.charAt(1)) >= 0;
            if (bothSides && (bits & (1 << c)) == 0) nubs.append(nubs.length() == 0 ? "" : "_").append(corner);
        }
        if (nubs.length() == 0) return q;
        String key = path + "_nub_" + nubs;
        TextureAtlasSprite to = sprites.computeIfAbsent(key, k -> Minecraft.getInstance().getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(ResourceLocation.fromNamespaceAndPath(name.getNamespace(), k)));
        if (to.contents().name().equals(MissingTextureAtlasSprite.getLocation())) return q;
        return remap(q, to);
    }

    /** The same face with another sprite's texture: UVs are mapped from the old atlas area to the new one. */
    private static BakedQuad remap(BakedQuad q, TextureAtlasSprite to) {
        TextureAtlasSprite from = q.getSprite();
        int[] v = q.getVertices().clone();
        int stride = v.length / 4;   // DefaultVertexFormat.BLOCK: x y z colour u v light normal
        for (int i = 0; i < 4; i++) {
            int o = i * stride + 4;
            float u = Float.intBitsToFloat(v[o]), w = Float.intBitsToFloat(v[o + 1]);
            v[o] = Float.floatToRawIntBits(to.getU(from.getUOffset(u)));
            v[o + 1] = Float.floatToRawIntBits(to.getV(from.getVOffset(w)));
        }
        return new BakedQuad(v, q.getTintIndex(), q.getDirection(), to, q.isShade(), q.hasAmbientOcclusion());
    }
}
