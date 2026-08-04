package com.jykito.industrialcore.block.custom;

public enum SolarPanelTier {

    T1(1,   3,  8,          2,          8_000),
    T2(2,   3,  64,         16,         64_000),
    T3(3,   3,  512,        128,        512_000),
    T4(4,   3,  4096,       1024,       4_096_000),
    T5(5,   3,  32768,      8192,       32_768_000),
    T6(6,   3,  128_000,    32_000,     128_000_000),
    T7(7,   3,  512_000,    128_000,    512_000_000),
    T8(8,   3,  2_500_000,  625_000,    250_000_000),
    T9(9,   3,  10_000_000, 2_500_000,  1_000_000_000),
    T10(10, 3,  50_000_000, 12_500_000, 2_000_000_000);

    public final int tierNum;
    public final int level;
    public final int dayGen;
    public final int nightGen;
    public final int bufferSize;

    SolarPanelTier(int tierNum, int level, int dayGen, int nightGen, int bufferSize) {
        this.tierNum    = tierNum;
        this.level      = level;
        this.dayGen     = dayGen;
        this.nightGen   = nightGen;
        this.bufferSize = bufferSize;
    }
}
