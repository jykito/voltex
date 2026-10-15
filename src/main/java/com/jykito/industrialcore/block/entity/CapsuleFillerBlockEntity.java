package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.block.custom.CapsuleFillerBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CapsuleFillerBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

    // Base for running without a module: 8 capsules. With parallel the tank grows by itself, see AdaptiveTank.
    private static final int TANK_CAP = 8000;
    private static final int FLUID_PER_CAPSULE = 1000;
    private static final int BASE_TIME = 40;
    private static final int BASE_FE_PER_TICK = 80;

    private final ItemStackHandler itemHandler = new ItemStackHandler(6) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {

                case 0 -> Caps.get(stack, Caps.FLUID_HANDLER_ITEM).map(h -> {
                    for (int t = 0; t < h.getTanks(); t++)
                        if (h.getFluidInTank(t).getAmount() < h.getTankCapacity(t)) return true;
                    return false;
                }).orElse(false);
                case 1 -> false;
                case 2, 3, 4, 5 -> UpgradeTargets.works(getBlockState().getBlock(), stack);
                default -> false;
            };
        }
    };
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

    private final MachineEnergy energyStorage = MachineEnergy.consumer(50_000, 50_000); // intake = buffer, no input cap

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

        MachineUpgrades upg = MachineUpgrades.scan(itemHandler, 2, 5);
        if (AdaptiveTank.resize(inputTank, TANK_CAP, upg.parallel()))
            level.sendBlockUpdated(pos, state, state, 3); // the tank gauge is computed from capacity, the client needs the new one

        maxProgress = upg.scaleTime(BASE_TIME);
        int fePerTick = upg.scaleEnergy(BASE_FE_PER_TICK);

        ItemStack input  = itemHandler.getStackInSlot(0);
        ItemStack output = itemHandler.getStackInSlot(1);

        boolean working = false;
        FillResult fr = simulateFill(input);
        if (fr != null) {
            int outRoom = output.isEmpty()
                    ? fr.result().getMaxStackSize()
                    : (ItemStack.isSameItemSameComponents(output, fr.result()) ? output.getMaxStackSize() - output.getCount() : 0);
            int actualParallel = Math.min(upg.parallel(), Math.min(input.getCount(),
                    Math.min(inputTank.getFluidAmount() / fr.consumed(), outRoom)));

            if (actualParallel >= 1 && energyStorage.getEnergyStored() >= fePerTick) {
                energyStorage.consumeEnergy(fePerTick);
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

        if (upg.itemPull() != null) MachineIO.pullItems(level, pos, upg.itemPull(), itemHandler, 0);
        if (upg.itemPush() != null) MachineIO.pushItems(level, pos, upg.itemPush(), itemHandler, 1);
        if (upg.fluidPull() != null) MachineIO.pullFluid(level, pos, upg.fluidPull(), inputTank);
        if (upg.fluidPush() != null) MachineIO.pushFluid(level, pos, upg.fluidPush(), inputTank);

        MachineLit.tick(this, level, pos, state, CapsuleFillerBlock.LIT, working);setChanged();
    }

    private record FillResult(ItemStack result, int consumed) {}

    private FillResult simulateFill(ItemStack containerStack) {
        if (containerStack.isEmpty()) return null;
        if (inputTank.getFluidAmount() < FLUID_PER_CAPSULE) return null;
        if (!Caps.get(containerStack, Caps.FLUID_HANDLER_ITEM).isPresent()) return null;

        ItemStack single = containerStack.copyWithCount(1);
        FluidActionResult sim = FluidUtil.tryFillContainer(single, inputTank, FLUID_PER_CAPSULE, null, false);
        if (!sim.isSuccess()) return null;

        ItemStack result = sim.getResult();
        int consumed = fluidContent(result) - fluidContent(single);
        if (consumed <= 0) return null;
        return new FillResult(result, consumed);
    }

    private static int fluidContent(ItemStack stack) {
        return Caps.get(stack, Caps.FLUID_HANDLER_ITEM).map(h -> {
            int total = 0;
            for (int t = 0; t < h.getTanks(); t++) total += h.getFluidInTank(t).getAmount();
            return total;
        }).orElse(0);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.capsule_filler"); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CapsuleFillerMenu(id, inv, this, this.data);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK)        return (T) energyStorage;
        if (cap == Capabilities.ItemHandler.BLOCK)  return (T) automationHandler;
        if (cap == Capabilities.FluidHandler.BLOCK) return (T) fillOnlyHandler;
        return null;
    }

    @Override public void onLoad() {
        super.onLoad();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("inputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        AdaptiveTank.save(tag, "inputTankCapacity", inputTank);
        tag.putInt("progress", progress);
        super.saveAdditional(tag, registries);
    }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        AdaptiveTank.load(tag, "inputTankCapacity", inputTank, TANK_CAP);
        inputTank.readFromNBT(registries, tag.getCompound("inputTank"));
        progress = tag.getInt("progress");
    }

    public FluidStack getFluidInTank() { return inputTank.getFluid(); }
    public int getTankCapacity()       { return inputTank.getCapacity(); }
}
