package com.jykito.industrialcore.block.custom.logistics;

public enum LogisticsConnectorMode {
    PUSH, PULL;

    public LogisticsConnectorMode next() {
        return this == PUSH ? PULL : PUSH;
    }

    public static LogisticsConnectorMode byId(int id) {
        LogisticsConnectorMode[] v = values();
        return v[Math.floorMod(id, v.length)];
    }
}
