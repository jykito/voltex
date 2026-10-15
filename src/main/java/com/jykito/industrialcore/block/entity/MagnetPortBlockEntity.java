package com.jykito.industrialcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Port BE on the magnet skeleton blocks. Stores nothing itself and does NOT tick.
 * When forming, the core "adopts" the port (bindCore); if the block is on the input surface (bottom platform),
 * the port exposes the CORE's caps to neighbours: items (input 0-7 / output 8), fluids (fill only), energy (receive only).
 * This way wires, pipes and hubs connect to the platform, while physically reaching into the core 3 blocks away.
 *
 * The caps are live proxies (each call resolves the current core by position),
 * so changing, breaking or moving the core leaves no dangling handlers and loses no items.
 */
public class MagnetPortBlockEntity extends BlockEntity {

    private BlockPos corePos = null;
    private boolean input = false;

    private LazyOptional<IItemHandler>   itemCap   = LazyOptional.empty();
    private LazyOptional<IFluidHandler>  fluidCap  = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.empty();

    public MagnetPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGNET_PORT_BE.get(), pos, state);
    }

    /** Current core (live lookup by position; null if the port is inactive or the core is unloaded/broken). */
    private @Nullable ElectromagnetCoreBlockEntity core() {
        if (!input || corePos == null || level == null) return null;
        return level.getBlockEntity(corePos) instanceof ElectromagnetCoreBlockEntity c && !c.isRemoved() ? c : null;
    }

    // Proxy handlers: stable identity, but every call goes to the live core (or does nothing if there is none).
    private final IItemHandler itemProxy = new IItemHandler() {
        @Override public int getSlots() { var c = core(); return c != null ? c.getExternalItemHandler().getSlots() : 0; }
        @Override public @NotNull ItemStack getStackInSlot(int s) { var c = core(); return c != null ? c.getExternalItemHandler().getStackInSlot(s) : ItemStack.EMPTY; }
        @Override public @NotNull ItemStack insertItem(int s, @NotNull ItemStack st, boolean sim) { var c = core(); return c != null ? c.getExternalItemHandler().insertItem(s, st, sim) : st; }
        @Override public @NotNull ItemStack extractItem(int s, int a, boolean sim) { var c = core(); return c != null ? c.getExternalItemHandler().extractItem(s, a, sim) : ItemStack.EMPTY; }
        @Override public int getSlotLimit(int s) { var c = core(); return c != null ? c.getExternalItemHandler().getSlotLimit(s) : 0; }
        @Override public boolean isItemValid(int s, @NotNull ItemStack st) { var c = core(); return c != null && c.getExternalItemHandler().isItemValid(s, st); }
    };
    private final IFluidHandler fluidProxy = new IFluidHandler() {
        @Override public int getTanks() { var c = core(); return c != null ? c.getExternalFluidHandler().getTanks() : 0; }
        @Override public @NotNull FluidStack getFluidInTank(int t) { var c = core(); return c != null ? c.getExternalFluidHandler().getFluidInTank(t) : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int t) { var c = core(); return c != null ? c.getExternalFluidHandler().getTankCapacity(t) : 0; }
        @Override public boolean isFluidValid(int t, @NotNull FluidStack st) { var c = core(); return c != null && c.getExternalFluidHandler().isFluidValid(t, st); }
        @Override public int fill(FluidStack r, IFluidHandler.FluidAction a) { var c = core(); return c != null ? c.getExternalFluidHandler().fill(r, a) : 0; }
        @Override public @NotNull FluidStack drain(FluidStack r, IFluidHandler.FluidAction a) { var c = core(); return c != null ? c.getExternalFluidHandler().drain(r, a) : FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int m, IFluidHandler.FluidAction a) { var c = core(); return c != null ? c.getExternalFluidHandler().drain(m, a) : FluidStack.EMPTY; }
    };
    private final IEnergyStorage energyProxy = new IEnergyStorage() {
        @Override public int receiveEnergy(int max, boolean sim) { var c = core(); return c != null ? c.getExternalEnergyHandler().receiveEnergy(max, sim) : 0; }
        @Override public int extractEnergy(int max, boolean sim) { var c = core(); return c != null ? c.getExternalEnergyHandler().extractEnergy(max, sim) : 0; }
        @Override public int getEnergyStored() { var c = core(); return c != null ? c.getExternalEnergyHandler().getEnergyStored() : 0; }
        @Override public int getMaxEnergyStored() { var c = core(); return c != null ? c.getExternalEnergyHandler().getMaxEnergyStored() : 0; }
        @Override public boolean canExtract() { var c = core(); return c != null && c.getExternalEnergyHandler().canExtract(); }
        @Override public boolean canReceive() { var c = core(); return c != null && c.getExternalEnergyHandler().canReceive(); }
    };

    /** When forming, the core binds the port and says whether it is the input surface (bottom platform). */
    public void bindCore(BlockPos core, boolean isInput) {
        if (core.equals(corePos) && isInput == input) return; // no change, do not touch the caps
        boolean wasActive = input;
        corePos = core.immutable();
        input = isInput;
        refreshCaps();
        setChanged();
        // the port came alive (form/reform) -> poke neighbours so pipes/wires/hubs rescan caps and reconnect
        if (input && !wasActive) notifyNeighbors();
    }

    /** The same signal as on block placement: makes neighbouring pipes/wires/hubs rescan caps. */
    private void notifyNeighbors() {
        if (level != null && !level.isClientSide())
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    /** Multiblock unformed: the port goes dead, neighbours disconnect. */
    public void unbind() {
        if (corePos == null && !input) return;
        corePos = null;
        input = false;
        refreshCaps();
        setChanged();
    }

    private void refreshCaps() {
        if (input) { // active: keep the caps "alive"
            if (!itemCap.isPresent())   itemCap   = LazyOptional.of(() -> itemProxy);
            if (!fluidCap.isPresent())  fluidCap  = LazyOptional.of(() -> fluidProxy);
            if (!energyCap.isPresent()) energyCap = LazyOptional.of(() -> energyProxy);
        } else {     // inactive: drop them so pipes disconnect
            dropCaps();
        }
    }

    private void dropCaps() {
        itemCap.invalidate();   itemCap   = LazyOptional.empty();
        fluidCap.invalidate();  fluidCap  = LazyOptional.empty();
        energyCap.invalidate(); energyCap = LazyOptional.empty();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (input) {
            if (cap == ForgeCapabilities.ITEM_HANDLER)  return itemCap.cast();
            if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
            if (cap == ForgeCapabilities.ENERGY)        return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        dropCaps();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (input && corePos != null) { // save only meaningful state (input port)
            tag.putLong("core", corePos.asLong());
            tag.putBoolean("in", true);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("core")) {
            corePos = BlockPos.of(tag.getLong("core"));
            input = tag.getBoolean("in");
        } else {
            corePos = null;
            input = false;
        }
        refreshCaps();
    }
}
