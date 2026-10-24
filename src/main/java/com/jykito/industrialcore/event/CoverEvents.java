package com.jykito.industrialcore.event;


import net.neoforged.fml.common.EventBusSubscriber;
import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.custom.cover.Covers;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * Sneak + right-click with a block on a cable or heat pipe puts the block on as a cover. Vanilla would place the
 * block next to the cable instead (sneaking skips Block.use), so the click is caught here first.
 */
@EventBusSubscriber(modid = IndustrialCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CoverEvents {

    private CoverEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown()) return;
        InteractionResult result = Covers.tryApply(event.getLevel(), event.getPos(), event.getEntity(), event.getItemStack());
        if (result.consumesAction()) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
