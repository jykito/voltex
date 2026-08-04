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
 * Катушка электромагнита-ускорителя. Тир задаёт обороты и скорость набора нестабильности.
 * Вставляется ПКМ по держателю, снимается Shift+ПКМ. Статы выводятся в тултипе.
 */
public class MagnetCoilItem extends Item {

    private final int tier;
    private final float spinMult;       // множитель оборотов
    private final float instabPerTick;  // прирост нестабильности/тик под нагрузкой
    private final String resourceKey;   // translatable-ключ требуемого питания

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
    /** Базовая скорость крафта от тира катушки (катушка = движок магнита). */
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
    }
}
