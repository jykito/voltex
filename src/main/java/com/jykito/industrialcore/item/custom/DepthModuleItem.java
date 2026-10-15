package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Depth module: lets the cryo chamber go down to stage {@link #getTier()}.
 * <p>
 * The gate is on the chamber, not on the heat source, because heat scales:
 * four thermal stations would reach any stage by numbers. The module is an item, and adding more
 * boilers does not get around it.
 * <p>
 * Not consumed and does not wear out: it is installed equipment, not fuel.
 */
public class DepthModuleItem extends Item {

    private final int tier;

    public DepthModuleItem(int tier, Properties props) {
        super(props);
        this.tier = tier;
    }

    /** The highest stage the module lets the chamber reach. */
    public int getTier() { return tier; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        UpgradeTargets.append(tooltip, ModBlocks.CRYO_CORE);
    }
}
