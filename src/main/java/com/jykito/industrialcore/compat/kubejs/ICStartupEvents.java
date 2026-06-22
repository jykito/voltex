package com.jykito.industrialcore.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public class ICStartupEvents {

    public static final EventGroup GROUP = EventGroup.of("ICStartupEvents");

    public static final EventHandler SOLAR_PANEL =
            GROUP.startup("solarPanel", () -> SolarPanelEventJS.class);

    public static final EventHandler RESTRUCTOR =
            GROUP.startup("restructor", () -> RestructorEventJS.class);
}
