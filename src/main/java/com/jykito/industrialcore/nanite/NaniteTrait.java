package com.jykito.industrialcore.nanite;

import org.jetbrains.annotations.Nullable;

/**
 * Редкие черты штамма — редкие виды атласа (юзер 2026-10-07: «Атлас видов в информационной панели»). Появляются только
 * при особых условиях отбора в камере эволюции ({@link NaniteTraits}), остаются навсегда и дают особенность в работе.
 * Числа предварительные, баланс позже.
 */
public enum NaniteTrait {
    /** 20 поколений в холоде: камера вплотную к блоку криоветки. Тепло больше не снижает стабильность. */
    CRYOPHILE(0xFF8FE6FF),
    /** Потолок сродства, выведенный на тепле от 90% со стабильностью от 50%. Сила эффекта ×1.2. */
    PYRONITE(0xFFFF7A3A),
    /** 20 поколений в поле: ядро магнита-ускорителя в 8 блоках от камеры. 15% шанс, что эффект сработает дважды. */
    RESONATOR(0xFFB46BFF),
    /** Сродство от четверти потолка к трём целям сразу. На чужой цели работает в половину своего лучшего сродства. */
    CHIMERA(0xFF7DFF9A),
    /** 100 поколений подряд на полной стабильности. Не изнашивается. */
    SELF_ASSEMBLER(0xFFF2F6F8);

    private final int color;
    NaniteTrait(int color) { this.color = color; }
    /** Цвет паука на иконке редкого вида (атлас, всплывашка). */
    public int color() { return color; }

    public static final int COLD_GENERATIONS = 20, FIELD_GENERATIONS = 20, STABLE_STREAK = 100;
    public static final float PYRONITE_POWER = 1.2f, RESONATOR_CHANCE = 0.15f, CHIMERA_SHARE = 0.5f;

    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }

    public static @Nullable NaniteTrait byKey(String k) {
        for (NaniteTrait t : values()) if (t.key().equals(k)) return t;
        return null;
    }
}
