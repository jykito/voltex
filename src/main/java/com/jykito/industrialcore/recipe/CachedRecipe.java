package com.jykito.industrialcore.recipe;

import java.util.function.Supplier;

public final class CachedRecipe<T> {

    private static volatile int generation = 0;
    public static void markAllDirty() { generation++; }

    private T value;
    private boolean dirty = true;
    private int gen = -1;

    public void markDirty() { dirty = true; }

    public T get(Supplier<T> scan) {
        if (dirty || gen != generation) {
            value = scan.get();
            dirty = false;
            gen = generation;
        }
        return value;
    }
}
