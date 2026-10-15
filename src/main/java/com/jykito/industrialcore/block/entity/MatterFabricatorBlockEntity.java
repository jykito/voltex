package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.block.custom.MatterFabricatorBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MatterFabricatorBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

    public static final int BUFFER = 100_000_000;
    public static final int MAX_RECEIVE = 10_000_000;
    public static final int COST = 50_000_000;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 1 && stack.is(ModItems.ITEM_PUSHER.get());
        }
    };

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

    private final MachineEnergy energyStorage = MachineEnergy.consumer(BUFFER, MAX_RECEIVE);

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
            energyStorage.consumeEnergy(COST);
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
                    Caps.get(neighbor, Caps.ITEM_HANDLER, d.getOpposite()).ifPresent(nh -> {
                        ItemStack remaining = ItemHandlerHelper.insertItem(nh, output.copy(), false);
                        if (remaining.getCount() < output.getCount()) {
                            itemHandler.setStackInSlot(0, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
                            setChanged();
                        }
                    });
                }
            }
        }

        MachineLit.tick(this, level, pos, state, MatterFabricatorBlock.LIT, working);
    }

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.matter_fabricator"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MatterFabricatorMenu(id, inventory, this, this.data);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK)       return (T) energyStorage;
        if (cap == Capabilities.ItemHandler.BLOCK) return (T) automationHandler;
        return null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
    }
}
