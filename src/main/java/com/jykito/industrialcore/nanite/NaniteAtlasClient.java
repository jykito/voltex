package com.jykito.industrialcore.nanite;

import net.minecraft.nbt.CompoundTag;

/**
 * Client copy of the species atlas: what the last {@link com.jykito.industrialcore.networking.AtlasSyncPacket} brought.
 * No client classes, so it is safe on a dedicated server too.
 */
public final class NaniteAtlasClient {

    private NaniteAtlasClient() {}

    private static CompoundTag data = new CompoundTag();
    /** Whether the atlas arrived during this session: the first sync (on join) gives no toasts. */
    private static boolean synced;

    public static void set(CompoundTag tag) { data = tag; synced = true; }
    public static CompoundTag data() { return data; }
    public static boolean synced() { return synced; }
    /** Leaving the world: the next join starts from a clean copy again. */
    public static void reset() { data = new CompoundTag(); synced = false; }

    /** Species record in thousandths, or −1 if the species is not unlocked. */
    public static int best(String speciesKey) {
        CompoundTag s = data.getCompound("S");
        return s.contains(speciesKey) ? s.getInt(speciesKey) : -1;
    }

    /** The item the species was unlocked on (id), or an empty string. */
    public static String icon(String speciesKey) { return data.getCompound("I").getString(speciesKey); }

    public static boolean trait(NaniteTrait t) { return trait(t.key()); }
    public static boolean trait(String key) { return data.getCompound("T").getBoolean(key); }
}
