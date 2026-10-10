package com.jykito.industrialcore.nanite;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Переработчик в промывщике (малом и мультиблоке): на каждую операцию с шансом {@link NaniteGenome#secondOutputChance} (до 30%)
 * основной продукт выходит ещё раз. Материал берётся со входа рецепта (сырьё, руда). Время не трогает — это работа ускорителей.
 * Износ как у добытчика, но реже: операций у промывщика намного больше, чем циклов у шахтёра.
 */
public final class NaniteProcessing {

    private NaniteProcessing() {}

    public static final float WEAR_CHANCE = 0.01f, WEAR = 0.97f;

    /** Сколько дополнительных продуктов дали {@code ops} операций (у резонатора иногда два на операцию). */
    public static int extra(Level level, ItemStack strain, Item input, int ops) {
        if (strain.isEmpty() || ops <= 0) return 0;
        NaniteGenome g = NaniteGenome.read(strain);
        if (!g.is(NaniteGenome.Kind.PROCESSOR)) return 0;
        float chance = g.secondOutputChance(OreMaterial.any(input));
        if (chance <= 0) return 0;
        int n = 0;
        boolean resonator = g.has(NaniteTrait.RESONATOR);
        for (int i = 0; i < ops; i++) if (level.random.nextFloat() < chance) {
            n++;
            if (resonator && level.random.nextFloat() < NaniteTrait.RESONATOR_CHANCE) n++;   // резонанс: ещё один
        }
        return n;
    }

    /** Штамм после {@code ops} операций: тот же предмет, если износа не случилось, иначе изношенная копия. */
    public static ItemStack wear(Level level, ItemStack strain, int ops) {
        if (strain.isEmpty() || ops <= 0) return strain;
        NaniteGenome g = NaniteGenome.read(strain);
        if (!g.is(NaniteGenome.Kind.PROCESSOR) || !g.wears()) return strain;
        float chance = (1f - g.stability()) * WEAR_CHANCE;
        boolean worn = false;
        for (int i = 0; i < ops; i++) if (level.random.nextFloat() < chance) { g.decay(WEAR); worn = true; }
        if (!worn) return strain;
        ItemStack out = strain.copy();
        g.write(out);
        return out;
    }
}
