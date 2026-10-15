package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.block.custom.StoneGeneratorBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StoneGeneratorBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

    // Base for running without a module. With parallel the tanks grow by themselves, see AdaptiveTank.
    private static final int TANK_CAP = 8000;
    private static final int FE_PER_TICK = 200;

    private final ItemStackHandler itemHandler = new ItemStackHandler(5) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) return false;
            return UpgradeTargets.works(getBlockState().getBlock(), stack);
        }
    };
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

    private final MachineEnergy energyStorage = MachineEnergy.consumer(100_000, 100_000); // intake = buffer, no input cap

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

        MachineUpgrades upg = MachineUpgrades.scan(itemHandler, 1, 4);
        // Both tanks grow: a x64 batch takes 64 times more water and lava.
        boolean resized = AdaptiveTank.resize(waterTank, TANK_CAP, upg.parallel());
        resized        |= AdaptiveTank.resize(lavaTank,  TANK_CAP, upg.parallel());
        if (resized) level.sendBlockUpdated(pos, state, state, 3);

        maxProgress = upg.scaleTime(m.processTicks);
        int fePerTick = upg.scaleEnergy(FE_PER_TICK);

        ItemStack out = itemHandler.getStackInSlot(0);
        int outRoom = out.isEmpty() ? m.result.getDefaultMaxStackSize()
                : (out.is(m.result) ? out.getMaxStackSize() - out.getCount() : 0);
        int waterAvail = m.waterCost > 0 ? waterTank.getFluidAmount() / m.waterCost : Integer.MAX_VALUE;
        int lavaAvail  = m.lavaCost  > 0 ? lavaTank.getFluidAmount()  / m.lavaCost  : Integer.MAX_VALUE;
        int actualParallel = Math.min(upg.parallel(), Math.min(outRoom, Math.min(waterAvail, lavaAvail)));

        boolean working = false;
        if (running && actualParallel >= 1 && energyStorage.getEnergyStored() >= fePerTick) {
            energyStorage.consumeEnergy(fePerTick);
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

        if (upg.fluidPull() != null) MachineIO.pullFluid(level, pos, upg.fluidPull(), waterTank, lavaTank);
        if (upg.itemPush() != null) MachineIO.pushItems(level, pos, upg.itemPush(), itemHandler, 0);

        MachineLit.tick(this, level, pos, state, StoneGeneratorBlock.LIT, working);setChanged();
        if (waterTank.getFluidAmount() != waterBefore || lavaTank.getFluidAmount() != lavaBefore)
            level.sendBlockUpdated(pos, state, state, 3);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.stone_generator"); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new StoneGeneratorMenu(id, inv, this, this.data);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK)        return (T) energyStorage;
        if (cap == Capabilities.ItemHandler.BLOCK)  return (T) automationHandler;
        if (cap == Capabilities.FluidHandler.BLOCK) return (T) fluidHandler;
        return null;
    }

    @Override public void onLoad() {
        super.onLoad();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("waterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("lavaTank", lavaTank.writeToNBT(registries, new CompoundTag()));
        AdaptiveTank.save(tag, "waterTankCapacity", waterTank);
        AdaptiveTank.save(tag, "lavaTankCapacity",  lavaTank);
        tag.putInt("progress", progress);
        tag.putInt("mode", mode);
        tag.putBoolean("running", running);
        super.saveAdditional(tag, registries);
    }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        AdaptiveTank.load(tag, "waterTankCapacity", waterTank, TANK_CAP);
        AdaptiveTank.load(tag, "lavaTankCapacity",  lavaTank,  TANK_CAP);
        waterTank.readFromNBT(registries, tag.getCompound("waterTank"));
        lavaTank.readFromNBT(registries, tag.getCompound("lavaTank"));
        progress = tag.getInt("progress");
        mode = tag.getInt("mode");
        running = tag.getBoolean("running");
    }
}
