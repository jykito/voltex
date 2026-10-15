package com.jykito.industrialcore.item.custom;
import net.minecraft.world.item.Item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import com.jykito.industrialcore.block.custom.BarrelBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BarrelBlockItem extends BlockItem {

    public BarrelBlockItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        @Nullable Level level = context.level();

        if (getBlock() instanceof BarrelBlock b) {
            tooltip.add(Component.translatable("tooltip.industrial_core.barrel_capacity",
                    String.format("%,d", b.capacity)).withStyle(ChatFormatting.GRAY));
        }

        var content = stack.get(com.jykito.industrialcore.item.ModDataComponents.FLUID_CONTENT.get());
        if (content != null) {
            FluidStack fs = content.copy();
            if (!fs.isEmpty()) {
                tooltip.add(Component.literal(fs.getDisplayName().getString() + ": " + fs.getAmount() + " mB")
                        .withStyle(ChatFormatting.AQUA));
            }
        }

        tooltip.add(Component.translatable("tooltip.industrial_core.barrel_hint").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
