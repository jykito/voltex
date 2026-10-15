package com.jykito.industrialcore.block.custom.logistics;

public enum HubFaceMode {
    OFF,
    INPUT,   // the hub PUTS into the connected block (limit N + reserve)
    OUTPUT,  // the hub TAKES from the connected block (into the network)
    BOTH;    // both on one face (via shift-click)

    /** Plain cycle on the chip button: BOTH is skipped (it is only set with shift in the config). */
    public HubFaceMode next() {
        return switch (this) {
            case OFF    -> INPUT;
            case INPUT  -> OUTPUT;
            case OUTPUT -> OFF;
            case BOTH   -> OFF;
        };
    }

    public boolean hasInput()  { return this == INPUT  || this == BOTH; }
    public boolean hasOutput() { return this == OUTPUT || this == BOTH; }

    public static HubFaceMode of(boolean in, boolean out) {
        if (in && out) return BOTH;
        if (in)  return INPUT;
        if (out) return OUTPUT;
        return OFF;
    }

    public HubFaceMode withInput(boolean in)   { return of(in, hasOutput()); }
    public HubFaceMode withOutput(boolean out) { return of(hasInput(), out); }
    public HubFaceMode toggleInput()  { return withInput(!hasInput()); }
    public HubFaceMode toggleOutput() { return withOutput(!hasOutput()); }
}
