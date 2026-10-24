package com.jykito.industrialcore.api;

import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * Shared Voltex upgrades in add-on machines. After registering, the machine's upgrade slots should accept what
 * {@link UpgradeTargets#works} allows, and the machine applies the effect itself (for example with
 * {@code item.upgrade.MachineUpgrades.scan}). The upgrade tooltips list the machine by themselves. Call during setup.
 */
public final class VoltexUpgrades {

    private VoltexUpgrades() {}

    public static void allow(UpgradeTargets.Shared upgrade, Supplier<Block> machine) {
        UpgradeTargets.register(upgrade, machine);
    }
}
