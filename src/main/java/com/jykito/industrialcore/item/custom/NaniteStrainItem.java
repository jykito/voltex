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
 * Nanite strain. Bred in the evolution chamber; the class is fixed by the feed of the first generation:
 * a miner goes into the industrial miner, a processor into the ore washer (small and multiblock), a scientist into the nanite lab.
 * The genome is in NBT ({@link NaniteGenome}).
 */
public class NaniteStrainItem extends Item {

    private static final int SHOWN = 4;

    public NaniteStrainItem(Properties props) {
        super(props);
    }

    /** Target label: for a scientist the topic, for the others the item it was bred on (or the feed in the slot), otherwise the material key. */
    public static Component targetName(NaniteGenome g, String target, @Nullable ItemStack feed) {
        if (g.is(NaniteGenome.Kind.SCIENTIST)) return Component.translatable("nanite_theme.industrial_core." + target);
        if (feed != null && !feed.isEmpty()) return feed.getHoverName();
        Item icon = g.icon(target);
        return icon != null ? icon.getDescription() : Component.literal(target);
    }

    /** What affinity to the target gives, in the class's terms: ore bonus, second output chance, topic knowledge (and its threshold). */
    public static Component effect(NaniteGenome g, String target) {
        float b = g.bonus(target);
        if (g.is(NaniteGenome.Kind.PROCESSOR))
            return Component.translatable("tooltip.industrial_core.nanite_strain.effect.processor", percent(g.secondOutputChance(target)));
        if (g.is(NaniteGenome.Kind.SCIENTIST))
            return Component.translatable("tooltip.industrial_core.nanite_strain.effect.scientist", percent(b), percent(NaniteLab.threshold(target)));
        return Component.translatable("tooltip.industrial_core.nanite_strain.effect.miner", percent(b));
    }

    /** The same, short, for the display line next to stability. */
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

    /** A strain in the player's inventory unlocks its species (and traits) in the atlas; every 2 s, not every tick. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide() && entity instanceof net.minecraft.server.level.ServerPlayer player && level.getGameTime() % 40 == slot % 40)
            com.jykito.industrialcore.nanite.NaniteAtlas.discover(player, NaniteGenome.read(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tip, TooltipFlag flag) {
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
        for (com.jykito.industrialcore.nanite.NaniteTrait t : g.traits())   // rare traits
            tip.add(Component.translatable("tooltip.industrial_core.nanite_strain.trait",
                    Component.translatable("nanite_trait.industrial_core." + t.key())).withStyle(ChatFormatting.LIGHT_PURPLE));
        tip.add(Component.translatable("tooltip.industrial_core.nanite_strain.hint." + g.kind().key()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
