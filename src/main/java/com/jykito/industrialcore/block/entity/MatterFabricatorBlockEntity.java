package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.MatterFabricatorBlock;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.menu.MatterFabricatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MatterFabricatorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int BUFFER = 100_000_000;
    public static final int MAX_RECEIVE = 10_000_000;
    public static final int COST = 50_000_000;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 1 && stack.is(ModItems.ITEM_PUSHER.get());
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? itemHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    private static class FabricatorEnergy extends EnergyStorage {
        FabricatorEnergy(int capacity, int maxReceive) { super(capacity, maxReceive, 0); }
        @Override public boolean canExtract() { return false; }
        void consume(int amount) { this.energy = Math.max(0, this.energy - amount); }
        void setEnergy(int amount) { this.energy = Math.min(getMaxEnergyStored(), Math.max(0, amount)); }
    }
    private final FabricatorEnergy energyStorage = new FabricatorEnergy(BUFFER, MAX_RECEIVE);
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> COST & 0xFFFF;
                case 3 -> (COST >> 16) & 0xFFFF;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 4; }
    };

    public MatterFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_FABRICATOR_BE.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        boolean working = false;
        ItemStack out = itemHandler.getStackInSlot(0);
        boolean hasSpace = out.isEmpty()
                || (out.is(ModItems.EXOTIC_MATTER.get()) && out.getCount() < out.getMaxStackSize());

        if (energyStorage.getEnergyStored() >= COST && hasSpace) {
            energyStorage.consume(COST);
            if (out.isEmpty()) itemHandler.setStackInSlot(0, new ItemStack(ModItems.EXOTIC_MATTER.get()));
            else out.grow(1);
            working = true;
            setChanged();
        }

        ItemStack pusher = itemHandler.getStackInSlot(1);
        if (pusher.is(ModItems.ITEM_PUSHER.get())) {
            Direction dir = DirectionalUpgradeItem.getStoredDirection(pusher);
            ItemStack output = itemHandler.getStackInSlot(0);
            if (dir != null && !output.isEmpty()) {
                final Direction d = dir;
                BlockEntity neighbor = level.getBlockEntity(pos.relative(d));
                if (neighbor != null) {
                    neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).ifPresent(nh -> {
                        ItemStack remaining = ItemHandlerHelper.insertItem(nh, output.copy(), false);
                        if (remaining.getCount() < output.getCount()) {
                            itemHandler.setStackInSlot(0, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
                            setChanged();
                        }
                    });
                }
            }
        }

        if (state.getValue(MatterFabricatorBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(MatterFabricatorBlock.LIT, working), 3);
        }
    }

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.matter_fabricator"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MatterFabricatorMenu(id, inventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY)       return lazyEnergyHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        lazyItemHandler   = LazyOptional.of(() -> automationHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergyHandler.invalidate();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
    }
}
