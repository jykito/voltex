package com.jykito.industrialcore.event;


import net.neoforged.fml.common.EventBusSubscriber;
import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.minecraft.core.registries.BuiltInRegistries;

@EventBusSubscriber(modid = IndustrialCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public class GuideBookOnJoin {

    private static final String FLAG = "industrial_core:received_guide";
    private static final ResourceLocation GUIDE_BOOK_ITEM = ResourceLocation.fromNamespaceAndPath("industrial_core", "manual");

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(FLAG)) {
            return;
        }

        Item guideBook = BuiltInRegistries.ITEM.get(GUIDE_BOOK_ITEM);
        if (guideBook != null) {
            ItemStack stack = new ItemStack(guideBook);
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }

        persisted.putBoolean(FLAG, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
