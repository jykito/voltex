package com.jykito.industrialcore.client;

import com.jykito.industrialcore.screen.GuideScreen;
import net.minecraft.client.Minecraft;

/** Клиентская точка открытия гайд-экрана (вызывается из ManualItem через DistExecutor). */
public final class GuideClient {
    private GuideClient() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new GuideScreen());
    }
}
