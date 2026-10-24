package com.jykito.industrialcore;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class ModStyle {
    private ModStyle() {}

    public static final int ENERGY = 0x00FFCC;

    public static final Style ENERGY_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(ENERGY));

    /** Fill of an item's energy bar: a share of the fixed 13-pixel frame, never wider than the frame. */
    public static int energyBar(long stored, long capacity) {
        if (capacity <= 0) return 0;
        return (int) Math.round(13.0 * Math.min(1.0, Math.max(0.0, (double) stored / capacity)));
    }
}
