package com.jykito.industrialcore.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Formed views of other mods' multiblocks for the Voltex guide panel (client only).
 * <p>
 * A guide page shows a multiblock layer by layer from its schema; for structures that hide their blocks when formed and
 * draw one model from the core (like the industrial miner), the "formed" view needs that model. Register it here under a
 * name and use the same name in the {@code "model"} field of the structure element in the add-on's guide chapter
 * ({@code assets/industrial_core/guide/<lang>/*.json}). The renderer is called with the pose already set in the core
 * block (front to the north) and the time in ticks for animation.
 */
public final class GuideModels {

    private GuideModels() {}

    @FunctionalInterface
    public interface Renderer {
        void render(PoseStack pose, MultiBufferSource buffers, float ticks);
    }

    private record Entry(ResourceLocation coreBlock, Renderer renderer) {}

    private static final Map<String, Entry> MODELS = new ConcurrentHashMap<>();

    /** @param model name used in the guide json, best namespaced ({@code modid:reactor}); @param coreBlock the block the model is drawn from */
    public static void register(String model, ResourceLocation coreBlock, Renderer renderer) {
        MODELS.put(model, new Entry(coreBlock, renderer));
    }

    public static @Nullable String coreOf(String model) {
        Entry e = MODELS.get(model);
        return e == null ? null : e.coreBlock().toString();
    }

    /** Returns false if no such model is registered. */
    public static boolean render(String model, PoseStack pose, MultiBufferSource buffers, float ticks) {
        Entry e = MODELS.get(model);
        if (e == null) return false;
        e.renderer().render(pose, buffers, ticks);
        return true;
    }
}
