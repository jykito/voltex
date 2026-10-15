package com.jykito.industrialcore.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Guide manual: RMB opens the native reference screen (client side). */
public class ManualItem extends Item {
    public ManualItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // client only: on a dedicated server this branch never runs, so the client class is never loaded there
        if (level.isClientSide)
            com.jykito.industrialcore.client.GuideClient.open();
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
