package com.jykito.industrialcore.item.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AcceleratorItem extends MachineUpgradeItem {

    private final float speedMultiplier;
    private final float energyMultiplier;

    public AcceleratorItem(float speedMultiplier, float energyMultiplier, Properties props) {
        super(props);
        this.speedMultiplier  = speedMultiplier;
        this.energyMultiplier = energyMultiplier;
    }

    public float getSpeedMultiplier()  { return speedMultiplier; }
    public float getEnergyMultiplier() { return energyMultiplier; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int speedPct  = Math.round((1f - speedMultiplier) * 100f);
        int energyPct = Math.round((energyMultiplier - 1f) * 100f);
        tooltip.add(Component.translatable("tooltip.industrial_core.accelerator_speed",
                speedMultiplier, speedPct).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.industrial_core.accelerator_energy",
                energyMultiplier, energyPct).withStyle(ChatFormatting.RED));
    }
}
