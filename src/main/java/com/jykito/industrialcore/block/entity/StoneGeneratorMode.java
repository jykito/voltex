package com.jykito.industrialcore.block.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum StoneGeneratorMode {

    COBBLESTONE(Items.COBBLESTONE,   40,    0,      0),
    GRAVEL     (Items.GRAVEL,        60,    0,      0),
    SAND       (Items.SAND,          40,  100,      0),
    OBSIDIAN   (Items.OBSIDIAN,     160, 1000,   1000);

    public final Item result;
    public final int processTicks;
    public final int waterCost;
    public final int lavaCost;

    StoneGeneratorMode(Item result, int processTicks, int waterCost, int lavaCost) {
        this.result = result;
        this.processTicks = processTicks;
        this.waterCost = waterCost;
        this.lavaCost = lavaCost;
    }

    private static final StoneGeneratorMode[] VALUES = values();
    public static StoneGeneratorMode byId(int id) { return VALUES[Math.floorMod(id, VALUES.length)]; }
    public StoneGeneratorMode next() { return byId(this.ordinal() + 1); }
}
