package com.jykito.industrialcore.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class ModRenderTypes extends RenderType {

    private ModRenderTypes(String name, VertexFormat fmt, VertexFormat.Mode mode,
                           int bufSize, boolean crumbling, boolean sort,
                           Runnable setup, Runnable clear) {
        super(name, fmt, mode, bufSize, crumbling, sort, setup, clear);
    }

    public static final Function<ResourceLocation, RenderType> ADDITIVE_GLOW = Util.memoize(tex ->
        create("industrial_core_glow",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS, 256, false, false,
            CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .setLightmapState(NO_LIGHTMAP)
                .setCullState(NO_CULL)
                .createCompositeState(false)
        ));

    private static final Function<ResourceLocation, RenderType> ADDITIVE_ENTITY_GLOW = Util.memoize(tex ->
        create("ic_additive_entity_glow",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, true, false,
            CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(POLYGON_OFFSET_LAYERING)
                .createCompositeState(true)
        ));

    public static RenderType additiveEntityGlow(ResourceLocation tex) {
        return ADDITIVE_ENTITY_GLOW.apply(tex);
    }

    private static final Function<ResourceLocation, RenderType> ENTITY_CUTOUT_NO_CULL_OFFSET = Util.memoize(tex ->
        create("ic_entity_cutout_no_cull_offset",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, true, false,
            CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(POLYGON_OFFSET_LAYERING)
                .createCompositeState(true)
        ));

    public static RenderType entityCutoutNoCullOffset(ResourceLocation tex) {
        return ENTITY_CUTOUT_NO_CULL_OFFSET.apply(tex);
    }

    private static final RenderType PLASMA_RING_TYPE = create("ic_plasma_ring",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS, 1024, false, true,
        CompositeState.builder()
            .setShaderState(POSITION_COLOR_SHADER)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .createCompositeState(false)
    );

    public static RenderType plasmaRing() { return PLASMA_RING_TYPE; }

    public static final Function<ResourceLocation, RenderType> ADDITIVE_GLOW_INNER = Util.memoize(tex ->
        create("industrial_core_glow_inner",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS, 256, false, false,
            CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setDepthTestState(NO_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .setLightmapState(NO_LIGHTMAP)
                .setCullState(NO_CULL)
                .createCompositeState(false)
        ));
}
