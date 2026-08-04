package com.jykito.industrialcore.block.custom.logistics;

public enum HubFaceMode {
    OFF,
    INPUT,   // хаб КЛАДЁТ в подключённый блок (лимит N + резерв)
    OUTPUT,  // хаб ЗАБИРАЕТ из подключённого блока (в сеть)
    BOTH;    // и то, и другое на одной грани (через shift-клик)

    /** Плейн-цикл по кнопке-чипу: BOTH пропускается (задаётся только shift-ом в конфиге). */
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
