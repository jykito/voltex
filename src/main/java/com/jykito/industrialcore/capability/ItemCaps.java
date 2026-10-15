package com.jykito.industrialcore.capability;

import com.jykito.industrialcore.item.EnergyItem;
import com.jykito.industrialcore.item.ModDataComponents;
import com.jykito.industrialcore.item.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

/** Item capabilities (Item.initCapabilities in 1.20.1). Called from Caps.register. */
final class ItemCaps {
    private ItemCaps() {}

    /** Capsule capacity, mB. */
    static final int CAPSULE_CAPACITY = 1000;

    static void register(RegisterCapabilitiesEvent event) {
        for (var holder : ModItems.ITEMS.getEntries()) {
            Item item = holder.get();
            if (item instanceof EnergyItem energy)
                event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> energy.createEnergyStorage(stack), item);
        }
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, ctx) -> new FluidHandlerItemStack(ModDataComponents.FLUID_CONTENT, stack, CAPSULE_CAPACITY),
                ModItems.UNIVERSAL_CAPSULE.get());
    }
}
