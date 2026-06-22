package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.FluidGeneratorBlock;
import com.jykito.industrialcore.item.upgrade.AcceleratorItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.menu.FluidGeneratorMenu;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidGeneratorBlockEntity extends BlockEntity implements MenuProvider {

    private final FluidGeneratorType type;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (stack.getItem() instanceof AcceleratorItem) return true;
            if (stack.getItem() instanceof DirectionalUpgradeItem dir)
                return dir.getMode() == DirectionalUpgradeItem.Mode.FLUID_PUSH;
            return false;
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    };
    public ItemStackHandler getItemHandler() { return itemHandler; }

    private static class GenEnergy extends EnergyStorage {
        GenEnergy(int capacity, int maxReceive) { super(capacity, maxReceive, 0); }
        void consume(int amount) { this.energy = Math.max(0, this.energy - amount); }
        void set(int amount)     { this.energy = Math.min(getMaxEnergyStored(), Math.max(0, amount)); }
    }
    private final GenEnergy energyStorage;
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    public final FluidTank outputTank;
    private LazyOptional<IFluidHandler> lazyFluid = LazyOptional.empty();

    private int progress = 0;
    private int maxProgress;
    protected final ContainerData data;

    public FluidGeneratorBlockEntity(BlockEntityType<?> beType, FluidGeneratorType type, BlockPos pos, BlockState state) {
        super(beType, pos, state);
        this.type = type;
        this.maxProgress = type.processTicks;
        this.energyStorage = new GenEnergy(type.feBuffer, type.feMaxReceive);
        this.outputTank = new FluidTank(type.tankCapacity, fs -> fs.getFluid() == type.fluid) {
            @Override protected void onContentsChanged() { setChanged(); }
        };
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

    public FluidGeneratorType getGeneratorType() { return type; }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        int fluidBefore = outputTank.getFluidAmount();

        float speedMul = 1.0f, energyMul = 1.0f;
        Direction fluidPushDir = null;
        for (int s = 0; s <= 1; s++) {
            ItemStack up = itemHandler.getStackInSlot(s);
            if (up.getItem() instanceof AcceleratorItem acc) {
                for (int c = 0; c < up.getCount(); c++) { speedMul *= acc.getSpeedMultiplier(); energyMul *= acc.getEnergyMultiplier(); }
            } else if (up.getItem() instanceof DirectionalUpgradeItem dir
                    && dir.getMode() == DirectionalUpgradeItem.Mode.FLUID_PUSH) {
                Direction d = DirectionalUpgradeItem.getStoredDirection(up);
                if (d != null) fluidPushDir = d;
            }
        }
        maxProgress = Math.max(1, (int) (type.processTicks * speedMul));
        int fePerTick = Math.max(1, (int) (type.fePerTick * energyMul));

        boolean working = false;
        if (energyStorage.getEnergyStored() >= fePerTick && outputTank.getSpace() >= type.mbPerCycle) {
            energyStorage.consume(fePerTick);
            progress++;
            working = true;
            if (progress >= maxProgress) {
                outputTank.fill(new FluidStack(type.fluid, type.mbPerCycle), IFluidHandler.FluidAction.EXECUTE);
                progress = 0;
            }
        } else {
            progress = 0;
        }

        if (fluidPushDir != null && !outputTank.isEmpty()) {
            final Direction fpd = fluidPushDir;
            BlockEntity n = level.getBlockEntity(pos.relative(fpd));
            if (n != null) n.getCapability(ForgeCapabilities.FLUID_HANDLER, fpd.getOpposite()).ifPresent(dst ->
                    FluidUtil.tryFluidTransfer(dst, outputTank, outputTank.getFluidAmount(), true));
        }

        if (state.getValue(FluidGeneratorBlock.LIT) != working)
            level.setBlock(pos, state.setValue(FluidGeneratorBlock.LIT, working), 3);

        setChanged();
        if (outputTank.getFluidAmount() != fluidBefore)
            level.sendBlockUpdated(pos, state, state, 3);
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public Component getDisplayName() { return Component.translatable(type.nameKey); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FluidGeneratorMenu(id, inv, this, this.data);
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
        lazyFluid  = LazyOptional.of(() -> outputTank);
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
        tag.put("outputTank", outputTank.writeToNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        super.saveAdditional(tag);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.set(tag.getInt("energy"));
        outputTank.readFromNBT(tag.getCompound("outputTank"));
        progress = tag.getInt("progress");
    }

    public FluidStack getFluidInTank() { return outputTank.getFluid(); }
    public int getTankCapacity()       { return outputTank.getCapacity(); }
}
