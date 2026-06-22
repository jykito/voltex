package com.jykito.industrialcore.item.upgrade;

import com.jykito.industrialcore.recipe.GrowingRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CultivatorUpgradeItem extends MachineUpgradeItem {

    public enum Mode {
        BIOSTIMULATOR(GrowingRecipe.Category.PLANT, "tooltip.industrial_core.cultivator_biostimulator"),
        CRYSTAL_RESONATOR(GrowingRecipe.Category.MINERAL, "tooltip.industrial_core.cultivator_resonator");

        public final GrowingRecipe.Category category;
        public final String tooltipKey;
        Mode(GrowingRecipe.Category category, String tooltipKey) {
            this.category = category;
            this.tooltipKey = tooltipKey;
        }
    }

    private final Mode mode;

    public CultivatorUpgradeItem(Mode mode, Properties props) {
        super(props.stacksTo(1));
        this.mode = mode;
    }

    public Mode getMode() { return mode; }
    public GrowingRecipe.Category getCategory() { return mode.category; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(mode.tooltipKey).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.industrial_core.cultivator_only").withStyle(ChatFormatting.DARK_GRAY));
    }
}
