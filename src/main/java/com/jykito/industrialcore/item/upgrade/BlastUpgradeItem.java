package com.jykito.industrialcore.item.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlastUpgradeItem extends MachineUpgradeItem {

    public enum Type {
        FORCED_AIR(0.70f, 1),
        OXYGEN_BOOST(0.25f, 4);

        public final float speedMultiplier;
        public final int parallel;
        Type(float speedMultiplier, int parallel) {
            this.speedMultiplier = speedMultiplier;
            this.parallel = parallel;
        }
    }

    private final Type type;

    public BlastUpgradeItem(Type type, Properties props) {
        super(props.stacksTo(1));
        this.type = type;
    }

    public Type getUpgradeType()      { return type; }
    public float getSpeedMultiplier() { return type.speedMultiplier; }
    public int getParallel()          { return type.parallel; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int speedPct = Math.round((1f - type.speedMultiplier) * 100f);
        tooltip.add(Component.translatable("tooltip.industrial_core.blast_upgrade_speed", speedPct).withStyle(ChatFormatting.GREEN));
        if (type.parallel > 1)
            tooltip.add(Component.translatable("tooltip.industrial_core.parallel_count", type.parallel).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.industrial_core.blast_upgrade_only").withStyle(ChatFormatting.DARK_GRAY));
    }
}
