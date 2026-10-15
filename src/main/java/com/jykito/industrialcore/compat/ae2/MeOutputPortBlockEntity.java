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
 * ME output: everything the core put into the slots and tank goes to the network. An AE2 autocraft job catches its
 * results when they are inserted into the network, so the "pattern port + ME output" pair closes the crafting loop.
 * If the network does not accept (no room or energy), the product waits in the port.
 */
class MeOutputPortBlockEntity extends MePortBlockEntity {

    MeOutputPortBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    /** The product goes to the network right away, one tank is enough for the queue. */
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
