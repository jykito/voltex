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
 * Одна сборка мультиблока структурной палочкой. В творческом режиме блоки бесплатны,
 * в выживании каждый списывается из инвентаря игрока. Чужие блоки в выживании не ломаются:
 * занятая клетка только засчитывается, чтобы сказать игроку.
 * Уже правильные клетки вызывающий код пропускает сам — критерий у каждого мультиблока свой.
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

    /** Творческий режим: блоки не нужны. */
    public boolean free() { return free; }

    public int placed()                  { return placed; }
    public int blocked()                 { return blocked; }
    public Map<Block, Integer> missing() { return missing; }

    /** Ставит блок в пустую клетку; в выживании сначала берёт его из инвентаря. */
    public void place(Level level, BlockPos p, BlockState state) {
        if (!level.getBlockState(p).canBeReplaced()) { blocked++; return; }
        if (!free && !takeOne(state.getBlock())) { missing.merge(state.getBlock(), 1, Integer::sum); return; }
        level.setBlock(p, state, 3);
        placed++;
    }

    /** Ставит блок поверх чего угодно. Только для творческого режима, где ломать чужое разрешено. */
    public void overwrite(Level level, BlockPos p, BlockState state) {
        level.setBlock(p, state, 3);
        placed++;
    }

    /** Клетка занята тем, что палочка в этом режиме убрать не может. */
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
