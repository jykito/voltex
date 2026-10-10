package com.jykito.industrialcore.compat.ae2;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * МЭ-выход: всё, что ядро положило в слоты и бак, уходит в сеть. Заказ автокрафта AE2 ловит свои
 * результаты при вставке в сеть, поэтому пара «порт шаблонов + МЭ-выход» закрывает цикл крафта.
 * Если сеть не принимает (нет места или энергии), продукт ждёт в порте.
 */
class MeOutputPortBlockEntity extends MePortBlockEntity {

    MeOutputPortBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    /** Продукт сразу уходит в сеть, одного бака под очередь хватает. */
    @Override public int maxTanks() { return 1; }

    @Override
    protected void meWork(IGrid grid) {
        ItemStackHandler items = getItems();
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.isEmpty()) continue;
            long put = insert(grid, AEItemKey.of(s), s.getCount());
            if (put > 0) items.setStackInSlot(i, s.copyWithCount(s.getCount() - (int) put));
        }
        FluidStack f = getTank().getFluid();
        if (!f.isEmpty()) {
            long put = insert(grid, AEFluidKey.of(f), f.getAmount());
            if (put > 0) getTank().drain((int) put, IFluidHandler.FluidAction.EXECUTE);
        }
    }
}
