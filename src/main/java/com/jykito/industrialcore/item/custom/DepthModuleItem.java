package com.jykito.industrialcore.item.custom;

import net.minecraft.world.item.Item;

/**
 * Модуль глубины: разрешение криокамере уходить на ступень {@link #getTier()}.
 * <p>
 * Гейт стоит именно на камере, а не на источнике тепла, потому что тепло масштабируется —
 * четыре термостанции взяли бы любую ступень количеством. Модуль это предмет, и числом
 * котельных его не обойти.
 * <p>
 * Не расходуется и не изнашивается: это установленное оборудование, а не топливо.
 */
public class DepthModuleItem extends Item {

    private final int tier;

    public DepthModuleItem(int tier, Properties props) {
        super(props);
        this.tier = tier;
    }

    /** Максимальная ступень, на которую модуль пускает камеру. */
    public int getTier() { return tier; }
}
