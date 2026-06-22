package com.jykito.industrialcore.item.custom;

import net.minecraft.world.item.Item;
import java.util.function.Supplier;

public class ReactorFuelRodItem extends Item {
    private final int energyPerTick;
    private final int heatPerTick;
    private final Supplier<Item> depleted;

    public ReactorFuelRodItem(int energyPerTick, int heatPerTick, Supplier<Item> depleted, Properties props) {
        super(props);
        this.energyPerTick = energyPerTick;
        this.heatPerTick   = heatPerTick;
        this.depleted      = depleted;
    }

    public int getEnergyPerTick() { return energyPerTick; }
    public int getHeatPerTick()   { return heatPerTick; }
    public Item getDepletedVariant() { return depleted.get(); }

}
