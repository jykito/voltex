package com.jykito.industrialcore.block.entity;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What the ME port window needs from the block entity. Implemented by the ME ports in compat/ae2; the interface itself has no AE2 references,
 * so the menu and window load without AE2. Whatever a port does not need stays empty by default.
 */
public interface MePortAccess {

    /** Total number of tanks of the port (open ones by number of open rows, see {@link ItemFluidPortBlockEntity#tanks()}). */
    int maxTanks();

    /** 1 no network, 2 on a network. */
    int meStatus();

    /** ME input: what to keep stocked, one cell per slot; the stack size is how many to keep. */
    @Nullable default ItemStackHandler meConfig() { return null; }
    /** ME input: fluid of tank {@code tank} and how much of it to keep. */
    default FluidStack meFluidConfig(int tank) { return FluidStack.EMPTY; }
    default void setMeFluidConfig(int tank, FluidStack fluid) {}

    /** Pattern port: processing patterns. */
    @Nullable default ItemStackHandler mePatterns() { return null; }
    default boolean meBlocking() { return false; }
    default void toggleMeBlocking() {}
    default boolean meShowInTerminal() { return true; }
    default void toggleMeShowInTerminal() {}
    default int mePriority() { return 0; }
    default void setMePriority(int priority) {}
}
