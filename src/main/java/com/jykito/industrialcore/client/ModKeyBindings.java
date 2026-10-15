package com.jykito.industrialcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class ModKeyBindings {

    public static final KeyMapping DRILL_MODE = new KeyMapping(
            "key.industrial_core.drill_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.industrial_core"
    );

    public static final KeyMapping PLASMA_BURST = new KeyMapping(
            "key.industrial_core.plasma_burst",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.industrial_core"
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(DRILL_MODE);
        event.register(PLASMA_BURST);
    }
}
