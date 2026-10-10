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
 * Блок улучшения лейт-мультиблока: встаёт на место любого блока механизма.
 * Своей логики нет, ядро считает такие блоки при проверке структуры.
 */
public class MechanismUpgradeBlock extends Block {

    public enum Kind { PARALLEL, SPEED, EFFICIENCY }

    /** Скорость: столько процентов времени цикла снимает один блок и столько процентов FE/т добавляет. */
    public static final int SPEED_TIME_PERCENT = 15, SPEED_ENERGY_PERCENT = 15;
    /** Энергоэффективность: столько процентов FE/т снимает один блок; складывается со скоростью суммой. */
    public static final int EFFICIENCY_ENERGY_PERCENT = 10;
    /** Ниже этих долей от исходного цикл не ускоряется и расход не падает, сколько блоков ни ставь. */
    public static final int MIN_TIME_PERCENT = 1, MIN_ENERGY_PERCENT = 25;

    private final Kind kind;
    private final int value;

    /** @param value для параллели — сколько операций добавляет блок; для остальных видов не используется */
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
