package com.jykito.industrialcore.client;

import com.jykito.industrialcore.screen.GuideScreen;
import net.minecraft.client.Minecraft;

/** Client entry point for opening the guide screen (called from ManualItem through DistExecutor). */
public final class GuideClient {
    private GuideClient() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new GuideScreen());
    }
}
