package com.jykito.industrialcore.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Апгрейд-модуль магнита «Фазозахват». В слоте апгрейда ядра удерживает нестабильность
 * на дрейфующей цели резонанса → надёжное ×2 без ручного/редстоун-слежения.
 */
public class PhaseLockUpgradeItem extends Item {
    public PhaseLockUpgradeItem(Properties props) { super(props); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.industrial_core.phase_lock_upgrade").withStyle(ChatFormatting.GRAY));
    }
}
