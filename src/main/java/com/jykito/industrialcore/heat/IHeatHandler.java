package com.jykito.industrialcore.heat;

/**
 * Heat as a resource: level (nominal temperature) 0..max.
 * Transfer only by "push" (receiveHeat) from hot to cold, like heat conduction.
 */
public interface IHeatHandler {

    /** Current heat level. */
    int getHeat();

    /** Maximum. */
    int getMaxHeat();

    /**
     * Accept heat. Returns how much was actually accepted (limited by capacity).
     * @param simulate if true, only estimate, do not change state.
     */
    int receiveHeat(int amount, boolean simulate);
}
