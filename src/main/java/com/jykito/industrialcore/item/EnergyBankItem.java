package com.jykito.industrialcore.item;
import net.minecraft.world.item.Item;

import com.jykito.industrialcore.ModStyle;
import com.jykito.industrialcore.block.custom.EnergyBankBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Energy bank as an item. The charge lives in BlockEntityTag (written by the wrench, the loot table and the upgrade
 * recipe), so placing the item restores it through the vanilla BlockItem path. Shows the charge as a tooltip and bar.
 */
public class EnergyBankItem extends BlockItem {

    public EnergyBankItem(EnergyBankBlock block, Properties props) {
        super(block, props);
    }

    public static int storedEnergy(ItemStack stack) {
        return stack.getOrDefault(com.jykito.industrialcore.item.ModDataComponents.ENERGY.get(), 0);
    }

    private int capacity() { return ((EnergyBankBlock) getBlock()).tier.capacity; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        @Nullable Level level = context.level();
        tooltip.add(Component.translatable("tooltip.industrial_core.battery_energy", storedEnergy(stack), capacity())
                .withStyle(ModStyle.ENERGY_STYLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override public boolean isBarVisible(ItemStack stack) { return storedEnergy(stack) > 0; }
    @Override public int getBarWidth(ItemStack stack) { return ModStyle.energyBar(storedEnergy(stack), capacity()); }
    @Override public int getBarColor(ItemStack stack) { return ModStyle.ENERGY; }
}
