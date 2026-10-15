package com.jykito.industrialcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.jykito.industrialcore.capability.Caps;
import java.util.Optional;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Exchange between the machine and its neighbour on the side set by a directional upgrade
 * (item and fluid puller / pusher).
 * <p>
 * Items get the machine's internal handler, not the one exposed to pipes: the machine
 * puts into its inputs and takes from its outputs itself, automation restrictions do not apply to it.
 * The "can this go into the slot" check is done by the handler itself through isItemValid.
 */
public final class MachineIO {

    private MachineIO() {}

    /** The neighbour's capability on side {@code side}, facing us. */
    public static <T> Optional<T> neighbor(Level level, BlockPos pos, Direction side, Caps.Kind<T> kind) {
        if (kind.block() == null) return Optional.empty();
        return Optional.ofNullable(level.getCapability(kind.block(), pos.relative(side), side.getOpposite()));
    }

    /**
     * Pull one stack of items: the first neighbour slot whose contents at least partly
     * fit into one of {@code targets} (checked in order). Up to 64 items per call.
     *
     * @return true if something was moved
     */
    public static boolean pullItems(Level level, BlockPos pos, Direction side, IItemHandler into, int... targets) {
        IItemHandler src = neighbor(level, pos, side, Caps.ITEM_HANDLER).orElse(null);
        if (src == null) return false;
        for (int i = 0; i < src.getSlots(); i++) {
            ItemStack sim = src.extractItem(i, 64, true);
            if (sim.isEmpty()) continue;
            for (int target : targets) {
                int accepted = sim.getCount() - into.insertItem(target, sim, true).getCount();
                if (accepted > 0) {
                    into.insertItem(target, src.extractItem(i, accepted, false), false);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Push the contents of slots {@code slots} to the neighbour, as much as it accepts.
     *
     * @return true if something went out
     */
    public static boolean pushItems(Level level, BlockPos pos, Direction side, IItemHandlerModifiable from, int... slots) {
        IItemHandler dst = neighbor(level, pos, side, Caps.ITEM_HANDLER).orElse(null);
        if (dst == null) return false;
        boolean moved = false;
        for (int slot : slots) {
            ItemStack out = from.getStackInSlot(slot);
            if (out.isEmpty()) continue;
            ItemStack left = ItemHandlerHelper.insertItem(dst, out.copy(), false);
            if (left.getCount() < out.getCount()) {
                from.setStackInSlot(slot, left.isEmpty() ? ItemStack.EMPTY : left);
                moved = true;
            }
        }
        return moved;
    }

    /** Top up the tanks from the neighbour, each as much as fits. Tanks are filled in order. */
    public static void pullFluid(Level level, BlockPos pos, Direction side, FluidTank... into) {
        neighbor(level, pos, side, Caps.FLUID_HANDLER).ifPresent(src -> {
            for (FluidTank tank : into)
                if (tank.getSpace() > 0) FluidUtil.tryFluidTransfer(tank, src, tank.getSpace(), true);
        });
    }

    /** Drain the tank into the neighbour, as much as it accepts. */
    public static void pushFluid(Level level, BlockPos pos, Direction side, FluidTank from) {
        if (from.isEmpty()) return;
        neighbor(level, pos, side, Caps.FLUID_HANDLER).ifPresent(dst ->
                FluidUtil.tryFluidTransfer(dst, from, from.getFluidAmount(), true));
    }
}
