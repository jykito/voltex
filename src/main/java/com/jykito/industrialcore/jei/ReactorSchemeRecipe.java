package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.reactor.ReactorSchemes;
import net.minecraft.world.item.ItemStack;

public class ReactorSchemeRecipe {

    private final String nameKey;
    private final String layout;
    private final int energyPerTick;

    public ReactorSchemeRecipe(ReactorSchemes.Scheme scheme) {
        this.nameKey      = scheme.nameKey();
        this.layout       = scheme.layout();
        this.energyPerTick = scheme.energyPerTick();
    }

    public String getNameKey()       { return nameKey; }
    public String getLayout()        { return layout; }
    public int    getEnergyPerTick() { return energyPerTick; }

    public static ItemStack charToStack(char c) {
        return switch (c) {
            case 'U' -> new ItemStack(ModItems.URANIUM_FUEL_ROD.get());
            case 'P' -> new ItemStack(ModItems.PLUTONIUM_FUEL_ROD.get());
            case 'M' -> new ItemStack(ModItems.MOX_FUEL_ROD.get());
            case 'G' -> new ItemStack(ModItems.GRAPHITE_ROD.get());
            case 'V' -> new ItemStack(ModItems.HEAT_VENT.get());
            case 'X' -> new ItemStack(ModItems.HEAT_EXCHANGER.get());
            case 'Y' -> new ItemStack(ModItems.ADVANCED_HEAT_EXCHANGER.get());
            case 'C' -> new ItemStack(ModItems.COMPONENT_HEAT_VENT.get());
            case 'I' -> new ItemStack(ModItems.INTEGRAL_HEAT_VENT.get());
            case 'A' -> new ItemStack(ModItems.ADVANCED_HEAT_VENT.get());
            case 'K' -> new ItemStack(ModItems.CONDENSATOR.get());
            default  -> ItemStack.EMPTY;
        };
    }
}
