package com.jykito.industrialcore.item.upgrade;

import com.jykito.industrialcore.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Улучшение портов мультиблока: одно и то же на все порты, растит их ёмкость.
 * Тир 0 = порт без улучшения, 1 = базовое, 2 = продвинутое. Все цифры портов живут здесь.
 */
public class PortUpgradeItem extends MachineUpgradeItem {

    private static final int[] SLOTS  = {9, 18, 27};
    private static final int[] FLUID  = {16_000, 64_000, 256_000};
    private static final int[] ENERGY = {1_000_000, 10_000_000, 100_000_000};

    public static final int MAX_SLOTS = SLOTS[SLOTS.length - 1];

    private final int tier;

    public PortUpgradeItem(int tier, Properties props) {
        super(props.stacksTo(1));
        this.tier = tier;
    }

    public static int tierOf(ItemStack stack) {
        return stack.getItem() instanceof PortUpgradeItem u ? u.tier : 0;
    }

    public static int slots(int tier)  { return SLOTS[tier]; }
    public static int fluid(int tier)  { return FLUID[tier]; }
    public static int energy(int tier) { return ENERGY[tier]; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.industrial_core.port_upgrade.slots", slots(tier)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.industrial_core.port_upgrade.fluid", num(fluid(tier))).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.industrial_core.port_upgrade.energy", num(energy(tier))).withStyle(ChatFormatting.GRAY));
        UpgradeTargets.append(tooltip, ModBlocks.MULTIBLOCK_INPUT_PORT, ModBlocks.MULTIBLOCK_OUTPUT_PORT, ModBlocks.MULTIBLOCK_ENERGY_PORT);
    }

    private static String num(int v) {
        return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
    }
}
