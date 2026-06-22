package com.jykito.industrialcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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
