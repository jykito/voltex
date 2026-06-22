package com.jykito.industrialcore.item.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ConnectorUpgradeItem extends Item {

    private final int interval;
    private final int maxItems;
    private final int maxMb;

    public ConnectorUpgradeItem(int interval, int maxItems, int maxMb, Properties props) {
        super(props);
        this.interval = interval;
        this.maxItems = maxItems;
        this.maxMb    = maxMb;
    }

    public int getInterval() { return interval; }
    public int getMaxItems() { return maxItems; }
    public int getMaxMb()    { return maxMb; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.industrial_core.connector_interval", interval)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.industrial_core.connector_items", maxItems)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.industrial_core.connector_fluid", maxMb)
                .withStyle(ChatFormatting.AQUA));
    }
}
