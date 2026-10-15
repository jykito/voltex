package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * "Phase Lock" magnet upgrade module. In the core upgrade slot it holds instability
 * on the drifting resonance target -> reliable x2 without manual or redstone tracking.
 */
public class PhaseLockUpgradeItem extends Item {
    public PhaseLockUpgradeItem(Properties props) { super(props); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.industrial_core.phase_lock_upgrade").withStyle(ChatFormatting.GRAY));
        UpgradeTargets.append(tooltip, ModBlocks.ELECTROMAGNET_CORE);
    }
}
