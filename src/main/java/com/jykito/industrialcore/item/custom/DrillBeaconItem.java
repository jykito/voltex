package com.jykito.industrialcore.item.custom;

import net.minecraft.world.level.block.Block;

/** Drill beacon as an item: in the slot, in hand and on the ground its 3D model is drawn (DrillBeaconItemRenderer). */
public class DrillBeaconItem extends TooltipBlockItem {

    public DrillBeaconItem(Block block, Properties properties, String tooltipKey) {
        super(block, properties, tooltipKey);
    }

}
