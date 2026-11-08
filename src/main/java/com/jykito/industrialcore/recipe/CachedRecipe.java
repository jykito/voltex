package com.jykito.industrialcore.recipe;

import java.util.function.Supplier;

public final class CachedRecipe<T> {

    /**
     * While no recipe fits, the slots are looked at again this often (ticks). A stack can grow without telling the
     * machine: a shift-click onto a stack already in its slot only changes the count and never reaches
     * onContentsChanged, so a cached "no recipe" would stay until the slot changed some other way.
     */
    public static final int RESCAN_TICKS = 20;

    private static volatile int generation = 0;
    public static void markAllDirty() { generation++; }

    private T value;
    private boolean dirty = true;
    private int gen = -1;
    private int idle = 0;

    public void markDirty() { dirty = true; }

    public T get(Supplier<T> scan) {
        if (dirty || gen != generation || (value == null && ++idle >= RESCAN_TICKS)) {
            value = scan.get();
            dirty = false;
            gen = generation;
            idle = 0;
        }
        return value;
    }
}
