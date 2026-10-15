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
 * Electromagnet-accelerator coil. The tier sets the spin and the instability build-up rate.
 * Inserted with RMB on a holder, removed with Shift+RMB. Stats are shown in the tooltip.
 */
public class MagnetCoilItem extends Item {

    private final int tier;
    private final float spinMult;       // spin multiplier
    private final float instabPerTick;  // instability gain per tick under load
    private final String resourceKey;   // translatable key of the required power

    public MagnetCoilItem(int tier, float spinMult, float instabPerTick, String resourceKey, Properties props) {
        super(props);
        this.tier = tier;
        this.spinMult = spinMult;
        this.instabPerTick = instabPerTick;
        this.resourceKey = resourceKey;
    }

    public int getTier() { return tier; }
    public float getSpinMult() { return spinMult; }
    public float getInstabPerTick() { return instabPerTick; }
    /** Base crafting speed by coil tier (the coil is the magnet's engine). */
    public float getCraftSpeed() { return switch (tier) { case 3 -> 1.6f; case 2 -> 1.3f; default -> 1.0f; }; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.industrial_core.coil_tier", tier).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.industrial_core.coil_speed",
                String.format("%.1f", spinMult)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.industrial_core.coil_instability",
                String.format("%.1f", instabPerTick * 20f)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.industrial_core.coil_resource",
                Component.translatable(resourceKey)).withStyle(ChatFormatting.DARK_GRAY));
        UpgradeTargets.append(tooltip, ModBlocks.ELECTROMAGNET_CORE);
    }
}
