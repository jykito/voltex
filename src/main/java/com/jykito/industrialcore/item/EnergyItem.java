package com.jykito.industrialcore.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** An item with an FE charge. ItemCaps registers the capability for every mod item with this interface. */
public interface EnergyItem {
    IEnergyStorage createEnergyStorage(ItemStack stack);
}
