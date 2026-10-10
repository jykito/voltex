package com.jykito.industrialcore.item.custom;

import net.minecraft.world.level.block.Block;

/** Буровой маяк как предмет: в слоте, руке и на земле рисуется его 3D-модель (DrillBeaconItemRenderer). */
public class DrillBeaconItem extends TooltipBlockItem {

    public DrillBeaconItem(Block block, Properties properties, String tooltipKey) {
        super(block, properties, tooltipKey);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.jykito.industrialcore.client.miner.DrillBeaconItemRenderer.get();
            }
        });
    }
}
