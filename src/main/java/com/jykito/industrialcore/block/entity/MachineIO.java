package com.jykito.industrialcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Обмен машины с соседом по стороне, которую задал направленный апгрейд
 * (затягиватель / выталкиватель предметов и жидкостей).
 * <p>
 * Предметам передаётся внутренний обработчик машины, а не тот, что отдаётся трубам: машина
 * сама кладёт во входы и забирает из выходов, запреты автоматизации к ней не относятся.
 * Проверку «можно ли в слот» делает сам обработчик через isItemValid.
 */
public final class MachineIO {

    private MachineIO() {}

    /** Capability соседа со стороны {@code side}, повёрнутая к нам. */
    public static <T> LazyOptional<T> neighbor(Level level, BlockPos pos, Direction side, Capability<T> cap) {
        BlockEntity be = level.getBlockEntity(pos.relative(side));
        return be == null ? LazyOptional.empty() : be.getCapability(cap, side.getOpposite());
    }

    /**
     * Затянуть одну пачку предметов: первый слот соседа, содержимое которого хоть частично
     * помещается в один из {@code targets} (проверяются по порядку). До 64 штук за вызов.
     *
     * @return true, если что-то переложено
     */
    public static boolean pullItems(Level level, BlockPos pos, Direction side, IItemHandler into, int... targets) {
        IItemHandler src = neighbor(level, pos, side, ForgeCapabilities.ITEM_HANDLER).orElse(null);
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
     * Вытолкнуть соседу содержимое слотов {@code slots}, сколько он примет.
     *
     * @return true, если что-то ушло
     */
    public static boolean pushItems(Level level, BlockPos pos, Direction side, IItemHandlerModifiable from, int... slots) {
        IItemHandler dst = neighbor(level, pos, side, ForgeCapabilities.ITEM_HANDLER).orElse(null);
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

    /** Долить баки из соседа, каждый сколько влезет. Баки заполняются по порядку. */
    public static void pullFluid(Level level, BlockPos pos, Direction side, FluidTank... into) {
        neighbor(level, pos, side, ForgeCapabilities.FLUID_HANDLER).ifPresent(src -> {
            for (FluidTank tank : into)
                if (tank.getSpace() > 0) FluidUtil.tryFluidTransfer(tank, src, tank.getSpace(), true);
        });
    }

    /** Слить бак соседу, сколько он примет. */
    public static void pushFluid(Level level, BlockPos pos, Direction side, FluidTank from) {
        if (from.isEmpty()) return;
        neighbor(level, pos, side, ForgeCapabilities.FLUID_HANDLER).ifPresent(dst ->
                FluidUtil.tryFluidTransfer(dst, from, from.getFluidAmount(), true));
    }
}
