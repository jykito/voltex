package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.block.custom.FluidGeneratorBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.item.upgrade.AcceleratorItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidGeneratorBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

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

    private final MachineEnergy energyStorage;

    public final FluidTank outputTank;

    private int progress = 0;
    private int maxProgress;
    protected final ContainerData data;

    public FluidGeneratorBlockEntity(BlockEntityType<?> beType, FluidGeneratorType type, BlockPos pos, BlockState state) {
        super(beType, pos, state);
        this.type = type;
        this.maxProgress = type.processTicks;
        this.energyStorage = MachineEnergy.consumer(type.feBuffer, type.feMaxReceive);
        this.outputTank = new FluidTank(type.tankCapacity, fs -> fs.getFluid() == type.fluid) {
            @Override protected void onContentsChanged() {
                setChanged();
                if (level != null && !level.isClientSide())
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
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

        MachineUpgrades upg = MachineUpgrades.scan(itemHandler, 0, 1);
        maxProgress = upg.scaleTime(type.processTicks);
        int fePerTick = upg.scaleEnergy(type.fePerTick);

        boolean working = false;
        if (energyStorage.getEnergyStored() >= fePerTick && outputTank.getSpace() >= type.mbPerCycle) {
            energyStorage.consumeEnergy(fePerTick);
            progress++;
            working = true;
            if (progress >= maxProgress) {
                outputTank.fill(new FluidStack(type.fluid, type.mbPerCycle), IFluidHandler.FluidAction.EXECUTE);
                progress = 0;
            }
        } else {
            progress = 0;
        }

        if (upg.fluidPush() != null) MachineIO.pushFluid(level, pos, upg.fluidPush(), outputTank);

        MachineLit.tick(this, level, pos, state, FluidGeneratorBlock.LIT, working);setChanged();
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public Component getDisplayName() { return Component.translatable(type.nameKey); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FluidGeneratorMenu(id, inv, this, this.data);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK)        return (T) energyStorage;
        if (cap == Capabilities.ItemHandler.BLOCK)  return (T) automationHandler;
        if (cap == Capabilities.FluidHandler.BLOCK) return (T) outputTank;
        return null;
    }

    @Override public void onLoad() {
        super.onLoad();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("outputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("progress", progress);
        super.saveAdditional(tag, registries);
    }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        outputTank.readFromNBT(registries, tag.getCompound("outputTank"));
        progress = tag.getInt("progress");
    }

    public FluidStack getFluidInTank() { return outputTank.getFluid(); }
    public int getTankCapacity()       { return outputTank.getCapacity(); }
}
