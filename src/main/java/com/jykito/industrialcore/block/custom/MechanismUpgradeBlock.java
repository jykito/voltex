package com.jykito.industrialcore.block.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Late multiblock upgrade block: takes the place of any mechanism block.
 * It has no logic of its own; the core counts these blocks when checking the structure.
 */
public class MechanismUpgradeBlock extends Block {

    public enum Kind { PARALLEL, SPEED, EFFICIENCY }

    /** Speed: how many percent of the cycle time one block removes and how many percent of FE/t it adds. */
    public static final int SPEED_TIME_PERCENT = 15, SPEED_ENERGY_PERCENT = 15;
    /** Energy efficiency: how many percent of FE/t one block removes; adds up with speed as a sum. */
    public static final int EFFICIENCY_ENERGY_PERCENT = 10;
    /** Below these fractions of the base value the cycle stops getting faster and usage stops dropping, however many blocks you place. */
    public static final int MIN_TIME_PERCENT = 1, MIN_ENERGY_PERCENT = 25;

    private final Kind kind;
    private final int value;

    /** @param value for parallel, how many operations the block adds; unused for the other kinds */
    public MechanismUpgradeBlock(Properties props, Kind kind, int value) {
        super(props);
        this.kind = kind;
        this.value = value;
    }

    public Kind kind() { return kind; }
    public int value() { return value; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tip, TooltipFlag flag) {
        switch (kind) {
            case PARALLEL -> tip.add(Component.translatable("tooltip.industrial_core.mechanism_upgrade.parallel", value)
                    .withStyle(ChatFormatting.GRAY));
            case SPEED -> tip.add(Component.translatable("tooltip.industrial_core.mechanism_upgrade.speed",
                    SPEED_TIME_PERCENT, SPEED_ENERGY_PERCENT).withStyle(ChatFormatting.GRAY));
            case EFFICIENCY -> tip.add(Component.translatable("tooltip.industrial_core.mechanism_upgrade.efficiency",
                    EFFICIENCY_ENERGY_PERCENT).withStyle(ChatFormatting.GRAY));
        }
        tip.add(Component.translatable("tooltip.industrial_core.mechanism_upgrade.place").withStyle(ChatFormatting.DARK_GRAY));
    }
}
