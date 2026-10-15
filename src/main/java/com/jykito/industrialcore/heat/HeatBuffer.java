package com.jykito.industrialcore.heat;

/** A simple heat buffer for pipes/machines. */
public class HeatBuffer implements IHeatHandler {

    private int heat;
    private int max;

    public HeatBuffer(int max) { this.max = max; }

    @Override public int getHeat() { return heat; }
    @Override public int getMaxHeat() { return max; }

    @Override
    public int receiveHeat(int amount, boolean simulate) {
        int accepted = Math.min(Math.max(0, amount), max - heat);
        if (!simulate) heat += accepted;
        return accepted;
    }

    public void setHeat(int h) { heat = Math.max(0, Math.min(max, h)); }
    public void setMax(int m) { this.max = Math.max(0, m); if (heat > max) heat = max; }  // dynamic cap (by tower height)
    public void extract(int amount) { heat = Math.max(0, heat - amount); }
}
