package com.jykito.industrialcore.item.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AdvancedParallelModuleItem extends MachineUpgradeItem {
    private final int parallelCount;

    public AdvancedParallelModuleItem(int parallelCount, Properties props) {
        super(props);
        this.parallelCount = parallelCount;
    }

    public int getParallelCount() { return parallelCount; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.industrial_core.parallel_count", parallelCount).withStyle(ChatFormatting.GOLD));
    }
}
