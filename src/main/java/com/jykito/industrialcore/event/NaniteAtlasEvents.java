package com.jykito.industrialcore.event;


import net.neoforged.fml.common.EventBusSubscriber;
import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.nanite.NaniteAtlas;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/** The nanite species atlas is sent to the client on joining the world: after that, discovering species updates it. */
@EventBusSubscriber(modid = IndustrialCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class NaniteAtlasEvents {

    private NaniteAtlasEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) NaniteAtlas.sync(player);
    }
}
