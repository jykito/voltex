package com.jykito.industrialcore.event;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.nanite.NaniteAtlas;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Атлас видов нанитов уходит клиенту при входе в мир: дальше его обновляет само открытие видов. */
@Mod.EventBusSubscriber(modid = IndustrialCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NaniteAtlasEvents {

    private NaniteAtlasEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) NaniteAtlas.sync(player);
    }
}
