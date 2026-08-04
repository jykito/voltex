package com.jykito.industrialcore.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Гайд-мануал: ПКМ открывает нативный экран-справочник (клиентски). */
public class ManualItem extends Item {
    public ManualItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide)
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.jykito.industrialcore.client.GuideClient::open);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
