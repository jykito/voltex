package com.jykito.industrialcore.reactor;

public class ReactorSchemes {

    public record Scheme(String nameKey, String layout, int energyPerTick) {}

    public static final Scheme[] SCHEMES = {

        new Scheme("reactor_scheme.uranium_1",
                ".V.V." +
                "VXVXV" +
                ".U.U." +
                "VXVXV" +
                ".V.V.",
                20_000),

        new Scheme("reactor_scheme.uranium_2",
                ".GUI." +
                "..I.." +
                ".GUI." +
                "..I.." +
                ".GUI.",
                60_000),

        new Scheme("reactor_scheme.uranium_3",
                ".I.I." +
                "GUGUG" +
                ".I.I." +
                "GUGUG" +
                ".I.I.",
                120_000),

        new Scheme("reactor_scheme.plutonium_1",
                "CPCPC" +
                "CCCCC" +
                "CPCPC" +
                "CCCCC" +
                "CPCPC",
                150_000),

        new Scheme("reactor_scheme.plutonium_2",
                "VCVCV" +
                "GPCPG" +
                "VCVCV" +
                "GPCPG" +
                "VCVCV",
                200_000),

        new Scheme("reactor_scheme.plutonium_3",
                "VGVGV" +
                "CPCPC" +
                "VGVGV" +
                "CPCPC" +
                "VGVGV",
                300_000),

        new Scheme("reactor_scheme.plutonium_4",
                "CGPGC" +
                "CPCPC" +
                "CPXPC" +
                "CPCPC" +
                "CGPGC",
                600_000),

        new Scheme("reactor_scheme.mox_1",
                "AAAAA" +
                "AYAYA" +
                "AMGMA" +
                "AYAYA" +
                "AAAAA",
                400_000),

        new Scheme("reactor_scheme.mox_2",
                "AYAYA" +
                "YMYMY" +
                "AGAGA" +
                "YMYMY" +
                "AYAYA",
                800_000),

        new Scheme("reactor_scheme.mox_3",
                "AYAYA" +
                "GMYMG" +
                "AGAGA" +
                "GMYMG" +
                "AYAYA",
                1_200_000),

        new Scheme("reactor_scheme.mox_4",
                "CMCMC" +
                "MYMMC" +
                "MCXCM" +
                "CMMYM" +
                "CMCMC",
                2_400_000),
    };
}
