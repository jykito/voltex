package com.jykito.industrialcore.nanite;

import net.minecraft.nbt.CompoundTag;

/**
 * Клиентская копия атласа видов: что пришло последним {@link com.jykito.industrialcore.networking.AtlasSyncPacket}.
 * Без клиентских классов, поэтому безопасно и на выделенном сервере.
 */
public final class NaniteAtlasClient {

    private NaniteAtlasClient() {}

    private static CompoundTag data = new CompoundTag();
    /** Пришёл ли атлас в этом заходе в мир: первая синхронизация (вход) всплывашек не даёт. */
    private static boolean synced;

    public static void set(CompoundTag tag) { data = tag; synced = true; }
    public static CompoundTag data() { return data; }
    public static boolean synced() { return synced; }
    /** Выход из мира: следующий вход снова начинает с чистой копии. */
    public static void reset() { data = new CompoundTag(); synced = false; }

    /** Рекорд вида в тысячных или −1, если вид не открыт. */
    public static int best(String speciesKey) {
        CompoundTag s = data.getCompound("S");
        return s.contains(speciesKey) ? s.getInt(speciesKey) : -1;
    }

    /** Предмет, на котором вид открыли (id), или пустая строка. */
    public static String icon(String speciesKey) { return data.getCompound("I").getString(speciesKey); }

    public static boolean trait(NaniteTrait t) { return data.getCompound("T").getBoolean(t.key()); }
}
