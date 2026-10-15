package com.jykito.industrialcore.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One multiblock build with the structure wand. In creative the blocks are free,
 * in survival each one is taken from the player inventory. In survival other blocks are not broken:
 * an occupied cell is only counted, to tell the player.
 * The calling code skips already correct cells itself; each multiblock has its own criterion.
 */
public final class WandBuild {

    private final Player player;
    private final boolean free;
    private int placed, blocked;
    private final Map<Block, Integer> missing = new LinkedHashMap<>();

    public WandBuild(Player player) {
        this.player = player;
        this.free = player.getAbilities().instabuild;
    }

    /** Creative mode: no blocks needed. */
    public boolean free() { return free; }

    public int placed()                  { return placed; }
    public int blocked()                 { return blocked; }
    public Map<Block, Integer> missing() { return missing; }

    /** Places a block into an empty cell; in survival takes it from the inventory first. */
    public void place(Level level, BlockPos p, BlockState state) {
        if (!level.getBlockState(p).canBeReplaced()) { blocked++; return; }
        if (!free && !takeOne(state.getBlock())) { missing.merge(state.getBlock(), 1, Integer::sum); return; }
        level.setBlock(p, state, 3);
        placed++;
    }

    /** Places a block over anything. Only for creative mode, where breaking other blocks is allowed. */
    public void overwrite(Level level, BlockPos p, BlockState state) {
        level.setBlock(p, state, 3);
        placed++;
    }

    /** The cell is occupied by something the wand cannot remove in this mode. */
    public void markBlocked() { blocked++; }

    private boolean takeOne(Block block) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(block.asItem())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
