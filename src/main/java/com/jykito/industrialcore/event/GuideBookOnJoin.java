package com.jykito.industrialcore.event;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = IndustrialCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GuideBookOnJoin {

    private static final String FLAG = "industrial_core:received_guide";
    private static final ResourceLocation GUIDE_BOOK_ITEM = new ResourceLocation("patchouli", "guide_book");
    private static final String BOOK_ID = "industrial_core:guide";

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

        Item guideBook = ForgeRegistries.ITEMS.getValue(GUIDE_BOOK_ITEM);
        if (guideBook != null) {
            ItemStack stack = new ItemStack(guideBook);
            stack.getOrCreateTag().putString("patchouli:book", BOOK_ID);
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }

        persisted.putBoolean(FLAG, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
