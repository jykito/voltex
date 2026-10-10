package com.jykito.industrialcore.block.entity;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * То, что окну МЭ-порта нужно от блок-сущности. Реализуют МЭ-порты в compat/ae2; сам интерфейс без ссылок на AE2,
 * поэтому меню и окно грузятся и без AE2. Что порту не нужно, остаётся пустым по умолчанию.
 */
public interface MePortAccess {

    /** Сколько баков у порта всего (открыто — по числу открытых рядов, см. {@link ItemFluidPortBlockEntity#tanks()}). */
    int maxTanks();

    /** 1 — нет сети, 2 — в сети. */
    int meStatus();

    /** МЭ-вход: что держать, по клетке на слот; число в стопке — сколько держать. */
    @Nullable default ItemStackHandler meConfig() { return null; }
    /** МЭ-вход: жидкость бака {@code tank} и сколько её держать. */
    default FluidStack meFluidConfig(int tank) { return FluidStack.EMPTY; }
    default void setMeFluidConfig(int tank, FluidStack fluid) {}

    /** Порт шаблонов: шаблоны обработки. */
    @Nullable default ItemStackHandler mePatterns() { return null; }
    default boolean meBlocking() { return false; }
    default void toggleMeBlocking() {}
    default boolean meShowInTerminal() { return true; }
    default void toggleMeShowInTerminal() {}
    default int mePriority() { return 0; }
    default void setMePriority(int priority) {}
}
