package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.reactor.ReactorSchemes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ReactorSchemeItem extends Item {
    private final int schemeIndex;

    public ReactorSchemeItem(int schemeIndex, Properties props) {
        super(props);
        this.schemeIndex = schemeIndex;
    }

    public ReactorSchemes.Scheme getScheme() { return ReactorSchemes.SCHEMES[schemeIndex]; }
    public int getSchemeIndex() { return schemeIndex; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ReactorSchemes.Scheme s = getScheme();
        tooltip.add(Component.translatable(s.nameKey()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(formatFE(s.energyPerTick()) + " FE/t").withStyle(com.jykito.industrialcore.ModStyle.ENERGY_STYLE));
    }

    private static String formatFE(int fe) {
        if (fe >= 1_000_000) return String.format("%.1fM", fe / 1_000_000.0);
        if (fe >= 1_000)     return String.format("%.0fk", fe / 1_000.0);
        return String.valueOf(fe);
    }

    public static boolean charMatchesStack(char c, ItemStack stack) {
        if (c == '.') return stack.isEmpty();
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return switch (c) {
            case 'U' -> item == ModItems.URANIUM_FUEL_ROD.get();
            case 'P' -> item == ModItems.PLUTONIUM_FUEL_ROD.get();
            case 'M' -> item == ModItems.MOX_FUEL_ROD.get();
            case 'G' -> item instanceof ReactorComponentItem rc && rc.getComponentType() == ReactorComponentItem.ComponentType.GRAPHITE;
            case 'V' -> item instanceof ReactorComponentItem rc && rc.getComponentType() == ReactorComponentItem.ComponentType.HEAT_VENT;
            case 'I' -> item instanceof ReactorComponentItem rc && rc.getComponentType() == ReactorComponentItem.ComponentType.INTEGRAL_VENT;
            case 'X' -> item instanceof ReactorComponentItem rc && rc.getComponentType() == ReactorComponentItem.ComponentType.HEAT_EXCHANGER;
            case 'Y' -> item instanceof ReactorComponentItem rc && rc.getComponentType() == ReactorComponentItem.ComponentType.ADVANCED_EXCHANGER;
            case 'C' -> item instanceof ReactorComponentItem rc && rc.getComponentType() == ReactorComponentItem.ComponentType.COMPONENT_VENT;
            case 'A' -> item instanceof ReactorComponentItem rc && rc.getComponentType() == ReactorComponentItem.ComponentType.ADVANCED_VENT;
            case 'K' -> item instanceof ReactorComponentItem rc && rc.getComponentType() == ReactorComponentItem.ComponentType.CONDENSATOR;
            default -> false;
        };
    }
}
