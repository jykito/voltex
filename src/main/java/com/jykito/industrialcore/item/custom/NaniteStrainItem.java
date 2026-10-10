package com.jykito.industrialcore.item.custom;

import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.industrialcore.nanite.NaniteLab;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Штамм нанитов. Выводится в камере эволюции; класс закрепляется кормом первого поколения:
 * добытчик ставится в промышленный шахтёр, переработчик — в промывщик (малый и мультиблок), учёный — в нанолабораторию.
 * Геном — в NBT ({@link NaniteGenome}).
 */
public class NaniteStrainItem extends Item {

    private static final int SHOWN = 4;

    public NaniteStrainItem(Properties props) {
        super(props);
    }

    /** Подпись цели: у учёного — тема, у остальных — предмет, на котором выводили (или корм в слоте), иначе ключ материала. */
    public static Component targetName(NaniteGenome g, String target, @Nullable ItemStack feed) {
        if (g.is(NaniteGenome.Kind.SCIENTIST)) return Component.translatable("nanite_theme.industrial_core." + target);
        if (feed != null && !feed.isEmpty()) return feed.getHoverName();
        Item icon = g.icon(target);
        return icon != null ? icon.getDescription() : Component.literal(target);
    }

    /** Что даёт сродство к цели, словами класса: прибавка руды, шанс второго выхода, изученность темы (и её порог). */
    public static Component effect(NaniteGenome g, String target) {
        float b = g.bonus(target);
        if (g.is(NaniteGenome.Kind.PROCESSOR))
            return Component.translatable("tooltip.industrial_core.nanite_strain.effect.processor", percent(g.secondOutputChance(target)));
        if (g.is(NaniteGenome.Kind.SCIENTIST))
            return Component.translatable("tooltip.industrial_core.nanite_strain.effect.scientist", percent(b), percent(NaniteLab.threshold(target)));
        return Component.translatable("tooltip.industrial_core.nanite_strain.effect.miner", percent(b));
    }

    /** То же коротко, для строки табло рядом со стабильностью. */
    public static Component effectShort(NaniteGenome g, String target) {
        float b = g.bonus(target);
        if (g.is(NaniteGenome.Kind.PROCESSOR))
            return Component.translatable("tooltip.industrial_core.nanite_strain.effect_short.processor", percent(g.secondOutputChance(target)));
        if (g.is(NaniteGenome.Kind.SCIENTIST))
            return Component.translatable("tooltip.industrial_core.nanite_strain.effect_short.scientist", percent(b));
        return Component.translatable("tooltip.industrial_core.nanite_strain.effect_short.miner", percent(b));
    }

    public static String percent(float v) {
        return String.valueOf(Math.round(v * 100));
    }

    @Override
    public Component getName(ItemStack stack) {
        NaniteGenome g = NaniteGenome.read(stack);
        String main = g.dominant();
        if (main == null || g.kind() == null) return super.getName(stack);
        return Component.translatable("item.industrial_core.nanite_strain.named." + g.kind().key(), targetName(g, main, null));
    }

    /** Штамм в инвентаре игрока открывает его вид (и черты) в атласе; раз в 2 с, не каждый тик. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide() && entity instanceof net.minecraft.server.level.ServerPlayer player && level.getGameTime() % 40 == slot % 40)
            com.jykito.industrialcore.nanite.NaniteAtlas.discover(player, NaniteGenome.read(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        NaniteGenome g = NaniteGenome.read(stack);
        if (g.isEmpty() || g.kind() == null) {
            tip.add(Component.translatable("tooltip.industrial_core.nanite_strain.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        tip.add(Component.translatable("tooltip.industrial_core.nanite_strain.kind." + g.kind().key()).withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.translatable("tooltip.industrial_core.nanite_strain.generation", g.generation()).withStyle(ChatFormatting.GRAY));
        g.affinity().entrySet().stream()
                .filter(e -> e.getValue() >= 0.005f)
                .sorted(Map.Entry.<String, Float>comparingByValue(Comparator.reverseOrder()))
                .limit(SHOWN)
                .forEach(e -> tip.add(targetName(g, e.getKey(), null).copy().append("  ").append(effect(g, e.getKey())).withStyle(ChatFormatting.AQUA)));
        if (g.is(NaniteGenome.Kind.MINER))
            tip.add(Component.translatable("tooltip.industrial_core.nanite_strain.fluid",
                    String.format(Locale.ROOT, "%.2f", g.fluid())).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.industrial_core.nanite_strain.stability", percent(g.stability())).withStyle(ChatFormatting.GRAY));
        for (com.jykito.industrialcore.nanite.NaniteTrait t : g.traits())   // редкие черты
            tip.add(Component.translatable("tooltip.industrial_core.nanite_strain.trait",
                    Component.translatable("nanite_trait.industrial_core." + t.key())).withStyle(ChatFormatting.LIGHT_PURPLE));
        tip.add(Component.translatable("tooltip.industrial_core.nanite_strain.hint." + g.kind().key()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
