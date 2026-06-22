package com.jykito.industrialcore;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class ModStyle {
    private ModStyle() {}

    public static final int ENERGY = 0x00FFCC;

    public static final Style ENERGY_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(ENERGY));
}
