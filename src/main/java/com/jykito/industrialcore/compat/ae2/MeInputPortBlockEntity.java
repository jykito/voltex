package com.jykito.industrialcore.compat.ae2;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import com.jykito.industrialcore.item.upgrade.PortUpgradeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * ME input: like an AE2 interface. Each config cell says what to keep and how many (the count in the ghost stack);
 * the port requests from the network up to that amount and returns the excess and anything foreign to the network.
 * Fluids the same way: each open tank has its own "which fluid and how many mB" setting.
 */
class MeInputPortBlockEntity extends MePortBlockEntity {

    private static final int TANKS = 3;

    private final ItemStackHandler config = new ItemStackHandler(PortUpgradeItem.MAX_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final FluidStack[] fluidConfig = { FluidStack.EMPTY, FluidStack.EMPTY, FluidStack.EMPTY };

    MeInputPortBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override public int maxTanks() { return TANKS; }
    @Override public ItemStackHandler meConfig() { return config; }
    @Override public FluidStack meFluidConfig(int tank) { return tank >= 0 && tank < TANKS ? fluidConfig[tank] : FluidStack.EMPTY; }

    @Override
    public void setMeFluidConfig(int tank, @NotNull FluidStack fluid) {
        if (tank < 0 || tank >= TANKS) return;
        fluidConfig[tank] = fluid.isEmpty() ? FluidStack.EMPTY : new FluidStack(fluid, Math.max(1, fluid.getAmount()));
        setChanged();
        sync();
    }

    @Override
    protected void meWork(IGrid grid) {
        ItemStackHandler items = getItems();
        for (int i = 0; i < activeSlots(); i++) {
            ItemStack want = config.getStackInSlot(i), cur = items.getStackInSlot(i);
            if (!cur.isEmpty() && (want.isEmpty() || !ItemStack.isSameItemSameTags(cur, want))) {
                long back = insert(grid, AEItemKey.of(cur), cur.getCount());
                if (back > 0) items.setStackInSlot(i, cur.copyWithCount(cur.getCount() - (int) back));
                continue;
            }
            if (want.isEmpty()) continue;
            int keep = Math.min(want.getCount(), Math.min(want.getMaxStackSize(), items.getSlotLimit(i)));
            if (cur.getCount() > keep) {   // the setting was lowered: the excess goes to the network
                long back = insert(grid, AEItemKey.of(cur), cur.getCount() - keep);
                if (back > 0) items.setStackInSlot(i, cur.copyWithCount(cur.getCount() - (int) back));
            } else {
                long got = extract(grid, AEItemKey.of(want), keep - cur.getCount());
                if (got > 0) items.setStackInSlot(i, want.copyWithCount(cur.getCount() + (int) got));
            }
        }
        // the port does not touch slots and tanks closed by removing the upgrade: the upgrade can only be removed when they are empty
        List<FluidTank> tanks = tanks();
        for (int t = 0; t < tanks.size(); t++) {
            FluidTank tank = tanks.get(t);
            FluidStack want = fluidConfig[t], cur = tank.getFluid();
            if (!cur.isEmpty() && (want.isEmpty() || !cur.isFluidEqual(want))) {
                long back = insert(grid, AEFluidKey.of(cur), cur.getAmount());
                if (back > 0) tank.drain((int) back, IFluidHandler.FluidAction.EXECUTE);
                continue;
            }
            if (want.isEmpty()) continue;
            int keep = Math.min(want.getAmount(), tank.getCapacity());
            if (cur.getAmount() > keep) {
                long back = insert(grid, AEFluidKey.of(cur), cur.getAmount() - keep);
                if (back > 0) tank.drain((int) back, IFluidHandler.FluidAction.EXECUTE);
            } else {
                long got = extract(grid, AEFluidKey.of(want), keep - cur.getAmount());
                if (got > 0) tank.fill(new FluidStack(want, (int) got), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    // ── saving; the fluid config also goes into the block sync: the window draws it on the client ──

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("meConfig", config.serializeNBT());
        tag.put("meFluidConfig", writeFluids());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("meConfig")) config.deserializeNBT(tag.getCompound("meConfig"));
        ListTag list = tag.getList("meFluidConfig", Tag.TAG_COMPOUND);
        for (int t = 0; t < TANKS; t++)
            fluidConfig[t] = t < list.size() ? FluidStack.loadFluidStackFromNBT(list.getCompound(t)) : FluidStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = super.getUpdateTag();
        t.put("meFluidConfig", writeFluids());
        return t;
    }

    private ListTag writeFluids() {
        ListTag list = new ListTag();
        for (FluidStack f : fluidConfig) list.add(f.writeToNBT(new CompoundTag()));
        return list;
    }
}
