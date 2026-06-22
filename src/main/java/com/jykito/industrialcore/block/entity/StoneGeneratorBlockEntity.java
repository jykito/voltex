package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.StoneGeneratorBlock;
import com.jykito.industrialcore.item.upgrade.AcceleratorItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import com.jykito.industrialcore.item.upgrade.ParallelModuleItem;
import com.jykito.industrialcore.menu.StoneGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StoneGeneratorBlockEntity extends BlockEntity implements MenuProvider {

    private static final int TANK_CAP = 64000;
    private static final int FE_PER_TICK = 200;

    private final ItemStackHandler itemHandler = new ItemStackHandler(5) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) return false;
            return stack.getItem() instanceof MachineUpgradeItem;
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return slot == 0 ? itemHandler.extractItem(0, amount, simulate) : ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    };
    public ItemStackHandler getItemHandler() { return itemHandler; }

    private static class GenEnergy extends EnergyStorage {
        GenEnergy(int capacity, int maxReceive) { super(capacity, maxReceive, 0); }
        void consume(int amount) { this.energy = Math.max(0, this.energy - amount); }
        void set(int amount)     { this.energy = Math.min(getMaxEnergyStored(), Math.max(0, amount)); }
    }
    private final GenEnergy energyStorage = new GenEnergy(100_000, 5_000);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    public final FluidTank waterTank = new FluidTank(TANK_CAP, fs -> fs.getFluid() == Fluids.WATER) {
        @Override protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide())
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    public final FluidTank lavaTank = new FluidTank(TANK_CAP, fs -> fs.getFluid() == Fluids.LAVA) {
        @Override protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide())
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };

    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return tank == 0 ? waterTank.getFluid() : lavaTank.getFluid(); }
        @Override public int getTankCapacity(int tank) { return TANK_CAP; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 ? stack.getFluid() == Fluids.WATER : stack.getFluid() == Fluids.LAVA;
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (resource.getFluid() == Fluids.WATER) return waterTank.fill(resource, action);
            if (resource.getFluid() == Fluids.LAVA)  return lavaTank.fill(resource, action);
            return 0;
        }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };
    private LazyOptional<IFluidHandler> lazyFluid = LazyOptional.empty();

    private int progress = 0;
    private int maxProgress = 40;
    private int mode = 0;
    private boolean running = false;
    protected final ContainerData data;

    public StoneGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STONE_GENERATOR_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                    case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    case 6 -> mode;
                    case 7 -> running ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                if (index == 0) progress = value;
                if (index == 1) maxProgress = value;
                if (index == 6) mode = value;
                if (index == 7) running = value != 0;
            }
            @Override public int getCount() { return 8; }
        };
    }

    public int getMode()        { return mode; }
    public boolean isRunning()  { return running; }
    public void cycleMode() {
        mode = StoneGeneratorMode.byId(mode).next().ordinal();
        progress = 0;
        setChanged();
    }
    public void toggleRunning() {
        running = !running;
        if (!running) progress = 0;
        setChanged();
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        int waterBefore = waterTank.getFluidAmount();
        int lavaBefore  = lavaTank.getFluidAmount();

        StoneGeneratorMode m = StoneGeneratorMode.byId(mode);

        float speedMul = 1.0f, energyMul = 1.0f;
        int parallel = 1;
        Direction itemPushDir = null, fluidPullDir = null;
        for (int s = 1; s <= 4; s++) {
            ItemStack up = itemHandler.getStackInSlot(s);
            if (up.getItem() instanceof AcceleratorItem acc) {
                for (int c = 0; c < up.getCount(); c++) { speedMul *= acc.getSpeedMultiplier(); energyMul *= acc.getEnergyMultiplier(); }
            } else if (up.getItem() instanceof ParallelModuleItem pm) {
                parallel = Math.max(parallel, pm.getParallelCount());
            } else if (up.getItem() instanceof com.jykito.industrialcore.item.upgrade.AdvancedParallelModuleItem adv) {
                parallel = Math.max(parallel, adv.getParallelCount());
            } else if (up.getItem() instanceof DirectionalUpgradeItem dir) {
                Direction d = DirectionalUpgradeItem.getStoredDirection(up);
                if (d != null) switch (dir.getMode()) {
                    case ITEM_PUSH  -> itemPushDir = d;
                    case FLUID_PULL -> fluidPullDir = d;
                    default -> {  }
                }
            }
        }
        maxProgress = Math.max(1, (int) (m.processTicks * speedMul));
        int fePerTick = Math.max(1, (int) (FE_PER_TICK * energyMul));

        ItemStack out = itemHandler.getStackInSlot(0);
        int outRoom = out.isEmpty() ? m.result.getMaxStackSize()
                : (out.is(m.result) ? out.getMaxStackSize() - out.getCount() : 0);
        int waterAvail = m.waterCost > 0 ? waterTank.getFluidAmount() / m.waterCost : Integer.MAX_VALUE;
        int lavaAvail  = m.lavaCost  > 0 ? lavaTank.getFluidAmount()  / m.lavaCost  : Integer.MAX_VALUE;
        int actualParallel = Math.min(parallel, Math.min(outRoom, Math.min(waterAvail, lavaAvail)));

        boolean working = false;
        if (running && actualParallel >= 1 && energyStorage.getEnergyStored() >= fePerTick) {
            energyStorage.consume(fePerTick);
            progress++;
            working = true;
            if (progress >= maxProgress) {
                if (m.waterCost > 0) waterTank.drain(m.waterCost * actualParallel, IFluidHandler.FluidAction.EXECUTE);
                if (m.lavaCost  > 0) lavaTank.drain(m.lavaCost  * actualParallel, IFluidHandler.FluidAction.EXECUTE);
                if (out.isEmpty()) itemHandler.setStackInSlot(0, new ItemStack(m.result, actualParallel));
                else out.grow(actualParallel);
                progress = 0;
            }
        } else {
            progress = 0;
        }

        if (fluidPullDir != null) {
            final Direction fpd = fluidPullDir;
            BlockEntity n = level.getBlockEntity(pos.relative(fpd));
            if (n != null) n.getCapability(ForgeCapabilities.FLUID_HANDLER, fpd.getOpposite()).ifPresent(src -> {
                FluidUtil.tryFluidTransfer(waterTank, src, waterTank.getSpace(), true);
                FluidUtil.tryFluidTransfer(lavaTank,  src, lavaTank.getSpace(),  true);
            });
        }
        if (itemPushDir != null && !itemHandler.getStackInSlot(0).isEmpty()) {
            final Direction pd = itemPushDir;
            BlockEntity n = level.getBlockEntity(pos.relative(pd));
            if (n != null) n.getCapability(ForgeCapabilities.ITEM_HANDLER, pd.getOpposite()).ifPresent(dst -> {
                ItemStack rem = ItemHandlerHelper.insertItem(dst, itemHandler.getStackInSlot(0), false);
                itemHandler.setStackInSlot(0, rem);
            });
        }

        if (state.getValue(StoneGeneratorBlock.LIT) != working)
            level.setBlock(pos, state.setValue(StoneGeneratorBlock.LIT, working), 3);

        setChanged();
        if (waterTank.getFluidAmount() != waterBefore || lavaTank.getFluidAmount() != lavaBefore)
            level.sendBlockUpdated(pos, state, state, 3);
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.stone_generator"); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new StoneGeneratorMenu(id, inv, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY)        return lazyEnergy.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER)  return lazyItemHandler.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluid.cast();
        return super.getCapability(cap, side);
    }

    @Override public void onLoad() {
        super.onLoad();
        lazyEnergy = LazyOptional.of(() -> energyStorage);
        lazyItemHandler = LazyOptional.of(() -> automationHandler);
        lazyFluid = LazyOptional.of(() -> fluidHandler);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate();
        lazyItemHandler.invalidate();
        lazyFluid.invalidate();
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("waterTank", waterTank.writeToNBT(new CompoundTag()));
        tag.put("lavaTank", lavaTank.writeToNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putInt("mode", mode);
        tag.putBoolean("running", running);
        super.saveAdditional(tag);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.set(tag.getInt("energy"));
        waterTank.readFromNBT(tag.getCompound("waterTank"));
        lavaTank.readFromNBT(tag.getCompound("lavaTank"));
        progress = tag.getInt("progress");
        mode = tag.getInt("mode");
        running = tag.getBoolean("running");
    }
}
