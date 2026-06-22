package com.jykito.industrialcore.block.custom.wire;

public enum WireTier {
    COPPER(4096, "copper_cable"),
    FERRO(65536, "ferro_cable"),
    GOLD(262144, "gold_conductor"),
    CRYSTALLINE(1048576, "crystalline_channel"),
    EMERALD_FLUX(4194304, "emerald_flux_cable"),
    HEAVY_MAGISTRAL(16777216, "heavy_magistral"),
    NETHERITE_FIBER(Integer.MAX_VALUE, "netherite_fiber_cable");

    private final int maxTransferRate;
    private final String name;

    WireTier(int maxTransferRate, String name) {
        this.maxTransferRate = maxTransferRate;
        this.name = name;
    }

    public int getMaxTransferRate() {
        return maxTransferRate;
    }

    public String getName() {
        return name;
    }
}
