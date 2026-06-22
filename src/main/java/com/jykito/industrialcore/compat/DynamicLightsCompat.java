package com.jykito.industrialcore.compat;

import com.jykito.industrialcore.block.entity.NuclearReactorBlockEntity;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DynamicLightsCompat {

    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean available = false;

    public static void init() {
        available = ModList.get().isLoaded("dynamiclights");
        if (available) {
            LOGGER.info("[IndustrialCore] Dynamic Lights detected — reactor glow integration active.");
        }
    }

    public static boolean isAvailable() { return available; }

    public static void registerSource(NuclearReactorBlockEntity be) {
        if (!available) return;
        try {

        } catch (Exception e) {
            LOGGER.warn("[IndustrialCore] Dynamic Lights API call failed: {}", e.getMessage());
            available = false;
        }
    }

    public static void removeSource(NuclearReactorBlockEntity be) {
        if (!available) return;
        try {

        } catch (Exception e) {
            LOGGER.warn("[IndustrialCore] Dynamic Lights API call failed: {}", e.getMessage());
            available = false;
        }
    }

    private DynamicLightsCompat() {}
}
