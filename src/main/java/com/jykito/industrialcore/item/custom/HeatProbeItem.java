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
 * Тепловой щуп: показывает картину ВСЕЙ теплосети, а не одного блока.
 * <p>
 * Появился из конкретной боли. Jade умеет читать блок под прицелом, но когда в машину приходит
 * 482 вместо ожидаемых 820, виноват не блок, а трасса — и увидеть это было нечем. Щуп отвечает
 * на три вопроса разом: сколько тепла в сети, сколько она раздаёт и <b>что именно её держит</b>.
 * <p>
 * Диагноз ставится по соотношению котла, раздачи и пропускной способности соединений:
 * <ul>
 *   <li>котёл пуст, раздача меньше возможной — не хватает <b>выработки</b>;</li>
 *   <li>котёл полон, раздача упёрлась в пропуск — мало <b>подключений</b> к потребителям;</li>
 *   <li>котёл полон, а раздача ниже пропуска — <b>потребители сыты</b>, сеть ни при чём.</li>
 * </ul>
 * Читает готовую сводку, которую сеть складывает по ходу своего тика, поэтому сам ничего не обходит.
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

    /** Главная строка: не числа, а вывод из них. */
    private static Component diagnosis(HeatPipeBlockEntity.Survey s) {
        if (s.sinks() == 0)
            return Component.translatable("msg.industrial_core.probe.no_sinks").withStyle(ChatFormatting.RED);
        if (s.capacity() > 0 && s.delivered() >= s.capacity())
            return Component.translatable("msg.industrial_core.probe.limit_links").withStyle(ChatFormatting.GOLD);
        // Котёл почти пуст — значит всё, что пришло, тут же ушло: упор в выработку.
        if (s.pool() <= s.delivered())
            return Component.translatable("msg.industrial_core.probe.limit_supply").withStyle(ChatFormatting.GOLD);
        return Component.translatable("msg.industrial_core.probe.limit_demand").withStyle(ChatFormatting.GREEN);
    }

    /** Щуп работает и по трубе, и по машине рядом с трубой — целиться в тонкую трубу неудобно. */
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
