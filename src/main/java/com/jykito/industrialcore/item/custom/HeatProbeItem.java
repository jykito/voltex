package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.block.entity.HeatPipeBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Heat probe: shows the picture of the WHOLE heat network, not one block.
 * <p>
 * It came from a specific pain. Jade can read the block under the crosshair, but when a machine gets
 * 482 instead of the expected 820, the culprit is not the block but the route, and there was no way to see it. The probe answers
 * three questions at once: how much heat is in the network, how much it hands out and <b>what exactly limits it</b>.
 * <p>
 * The diagnosis comes from the ratio of the pool, the delivery and the connection throughput:
 * <ul>
 *   <li>the pool is empty, delivery is below what is possible: not enough <b>generation</b>;</li>
 *   <li>the pool is full, delivery hit the throughput: too few <b>connections</b> to consumers;</li>
 *   <li>the pool is full and delivery is below throughput: <b>consumers are satisfied</b>, the network is fine.</li>
 * </ul>
 * It reads the ready summary the network builds during its tick, so it walks nothing itself.
 */
public class HeatProbeItem extends Item {

    public HeatProbeItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (level.isClientSide() || player == null) return InteractionResult.sidedSuccess(level.isClientSide());

        HeatPipeBlockEntity pipe = findPipe(level, ctx.getClickedPos());
        if (pipe == null) {
            player.displayClientMessage(Component.translatable("msg.industrial_core.probe.no_network")
                    .withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.SUCCESS;
        }

        HeatPipeBlockEntity.Survey s = pipe.getSurvey();
        player.displayClientMessage(Component.translatable("msg.industrial_core.probe.header", s.pipes(), s.sinks())
                .withStyle(ChatFormatting.AQUA), false);
        player.displayClientMessage(Component.translatable("msg.industrial_core.probe.flow",
                s.delivered(), s.capacity()).withStyle(ChatFormatting.WHITE), false);
        player.displayClientMessage(Component.translatable("msg.industrial_core.probe.pool", s.pool())
                .withStyle(ChatFormatting.GRAY), false);
        player.displayClientMessage(diagnosis(s), false);
        return InteractionResult.SUCCESS;
    }

    /** The main line: not numbers but the conclusion from them. */
    private static Component diagnosis(HeatPipeBlockEntity.Survey s) {
        if (s.sinks() == 0)
            return Component.translatable("msg.industrial_core.probe.no_sinks").withStyle(ChatFormatting.RED);
        if (s.capacity() > 0 && s.delivered() >= s.capacity())
            return Component.translatable("msg.industrial_core.probe.limit_links").withStyle(ChatFormatting.GOLD);
        // The pool is almost empty, so everything that came in went right out: limited by generation.
        if (s.pool() <= s.delivered())
            return Component.translatable("msg.industrial_core.probe.limit_supply").withStyle(ChatFormatting.GOLD);
        return Component.translatable("msg.industrial_core.probe.limit_demand").withStyle(ChatFormatting.GREEN);
    }

    /** The probe works both on a pipe and on a machine next to a pipe: aiming at a thin pipe is awkward. */
    @Nullable
    private static HeatPipeBlockEntity findPipe(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof HeatPipeBlockEntity p) return p;
        for (Direction d : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(d));
            if (be instanceof HeatPipeBlockEntity p) return p;
        }
        return null;
    }
}
