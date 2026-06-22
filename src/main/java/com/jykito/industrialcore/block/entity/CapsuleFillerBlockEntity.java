package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.CapsuleFillerBlock;
import com.jykito.industrialcore.item.upgrade.AcceleratorItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.item.upgrade.ParallelModuleItem;
import com.jykito.industrialcore.menu.CapsuleFillerMenu;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CapsuleFillerBlockEntity extends BlockEntity implements MenuProvider {

    private static final int TANK_CAP = 128000;
    private static final int FLUID_PER_CAPSULE = 1000;
    private static final int BASE_TIME = 40;
    private static final int BASE_FE_PER_TICK = 80;

    private final ItemStackHandler itemHandler = new ItemStackHandler(6) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {

                case 0 -> stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(h -> {
                    for (int t = 0; t < h.getTanks(); t++)
                        if (h.getFluidInTank(t).getAmount() < h.getTankCapacity(t)) return true;
                    return false;
                }).orElse(false);
                case 1 -> false;
                case 2, 3, 4, 5 -> stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
                default -> false;
            };
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot != 0) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 1) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };
    public ItemStackHandler getItemHandler() { return itemHandler; }

    private static class FillerEnergy extends EnergyStorage {
        FillerEnergy(int capacity, int maxReceive) { super(capacity, maxReceive, 0); }
        void consume(int amount) { this.energy = Math.max(0, this.energy - amount); }
        void set(int amount)     { this.energy = Math.min(getMaxEnergyStored(), Math.max(0, amount)); }
    }
    private final FillerEnergy energyStorage = new FillerEnergy(50_000, 2_000);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    public final FluidTank inputTank = new FluidTank(TANK_CAP) {
        @Override protected void onContentsChanged() {
            setChanged();

            if (level != null && !level.isClientSide())
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };

    private final IFluidHandler fillOnlyHandler = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return inputTank.getFluid(); }
        @Override public int getTankCapacity(int tank) { return TANK_CAP; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return true; }
        @Override public int fill(FluidStack resource, FluidAction action) { return inputTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };
    private LazyOptional<IFluidHandler> lazyFluid = LazyOptional.empty();

    private int progress = 0;
    private int maxProgress = BASE_TIME;
    protected final ContainerData data;

    public CapsuleFillerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAPSULE_FILLER_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                    case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                if (index == 0) progress = value;
                if (index == 1) maxProgress = value;
            }
            @Override public int getCount() { return 6; }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        float speedMul = 1.0f, energyMul = 1.0f;
        int parallel = 1;
        Direction itemPushDir = null, itemPullDir = null, fluidPushDir = null, fluidPullDir = null;
        for (int s = 2; s <= 5; s++) {
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
                    case ITEM_PUSH  -> itemPushDir  = d;
                    case ITEM_PULL  -> itemPullDir  = d;
                    case FLUID_PUSH -> fluidPushDir = d;
                    case FLUID_PULL -> fluidPullDir = d;
                }
            }
        }
        maxProgress = Math.max(1, (int) (BASE_TIME * speedMul));
        int fePerTick = Math.max(1, (int) (BASE_FE_PER_TICK * energyMul));

        ItemStack input  = itemHandler.getStackInSlot(0);
        ItemStack output = itemHandler.getStackInSlot(1);

        boolean working = false;
        FillResult fr = simulateFill(input);
        if (fr != null) {
            int outRoom = output.isEmpty()
                    ? fr.result().getMaxStackSize()
                    : (ItemStack.isSameItemSameTags(output, fr.result()) ? output.getMaxStackSize() - output.getCount() : 0);
            int actualParallel = Math.min(parallel, Math.min(input.getCount(),
                    Math.min(inputTank.getFluidAmount() / fr.consumed(), outRoom)));

            if (actualParallel >= 1 && energyStorage.getEnergyStored() >= fePerTick) {
                energyStorage.consume(fePerTick);
                progress++;
                working = true;
                if (progress >= maxProgress) {
                    itemHandler.extractItem(0, actualParallel, false);
                    inputTank.drain(fr.consumed() * actualParallel, IFluidHandler.FluidAction.EXECUTE);
                    if (output.isEmpty()) {
                        ItemStack out = fr.result().copy();
                        out.setCount(actualParallel);
                        itemHandler.setStackInSlot(1, out);
                    } else {
                        output.grow(actualParallel);
                    }
                    progress = 0;
                }
            } else {
                progress = 0;
            }
        } else {
            progress = 0;
        }

        if (itemPullDir != null) {
            final Direction d = itemPullDir;
            BlockEntity n = level.getBlockEntity(pos.relative(d));
            if (n != null) n.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).ifPresent(src -> {
                for (int i = 0; i < src.getSlots(); i++) {
                    ItemStack sim = src.extractItem(i, 64, true);
                    if (sim.isEmpty()) continue;
                    int accepted = sim.getCount() - itemHandler.insertItem(0, sim, true).getCount();
                    if (accepted > 0) {
                        itemHandler.insertItem(0, src.extractItem(i, accepted, false), false);
                        break;
                    }
                }
            });
        }
        if (itemPushDir != null && !itemHandler.getStackInSlot(1).isEmpty()) {
            final Direction d = itemPushDir;
            BlockEntity n = level.getBlockEntity(pos.relative(d));
            if (n != null) n.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).ifPresent(dst ->
                    itemHandler.setStackInSlot(1, ItemHandlerHelper.insertItem(dst, itemHandler.getStackInSlot(1), false)));
        }
        if (fluidPullDir != null) {
            final Direction d = fluidPullDir;
            BlockEntity n = level.getBlockEntity(pos.relative(d));
            if (n != null) n.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite()).ifPresent(src ->
                    FluidUtil.tryFluidTransfer(inputTank, src, inputTank.getSpace(), true));
        }
        if (fluidPushDir != null && !inputTank.isEmpty()) {
            final Direction d = fluidPushDir;
            BlockEntity n = level.getBlockEntity(pos.relative(d));
            if (n != null) n.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite()).ifPresent(dst ->
                    FluidUtil.tryFluidTransfer(dst, inputTank, inputTank.getFluidAmount(), true));
        }

        if (state.getValue(CapsuleFillerBlock.LIT) != working)
            level.setBlock(pos, state.setValue(CapsuleFillerBlock.LIT, working), 3);

        setChanged();
    }

    private record FillResult(ItemStack result, int consumed) {}

    private FillResult simulateFill(ItemStack containerStack) {
        if (containerStack.isEmpty()) return null;
        if (inputTank.getFluidAmount() < FLUID_PER_CAPSULE) return null;
        if (!containerStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) return null;

        ItemStack single = ItemHandlerHelper.copyStackWithSize(containerStack, 1);
        FluidActionResult sim = FluidUtil.tryFillContainer(single, inputTank, FLUID_PER_CAPSULE, null, false);
        if (!sim.isSuccess()) return null;

        ItemStack result = sim.getResult();
        int consumed = fluidContent(result) - fluidContent(single);
        if (consumed <= 0) return null;
        return new FillResult(result, consumed);
    }

    private static int fluidContent(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(h -> {
            int total = 0;
            for (int t = 0; t < h.getTanks(); t++) total += h.getFluidInTank(t).getAmount();
            return total;
        }).orElse(0);
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.capsule_filler"); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CapsuleFillerMenu(id, inv, this, this.data);
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
        lazyFluid = LazyOptional.of(() -> fillOnlyHandler);
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
        tag.put("inputTank", inputTank.writeToNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        super.saveAdditional(tag);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.set(tag.getInt("energy"));
        inputTank.readFromNBT(tag.getCompound("inputTank"));
        progress = tag.getInt("progress");
    }

    public FluidStack getFluidInTank() { return inputTank.getFluid(); }
    public int getTankCapacity()       { return inputTank.getCapacity(); }
}
