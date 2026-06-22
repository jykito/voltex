package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.SolarPanelBlock;
import com.jykito.industrialcore.block.custom.SolarPanelTier;
import com.jykito.industrialcore.menu.PanelCombinerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
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
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PanelCombinerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int PANEL_SLOTS = 18;
    public static final int BUFFER_SIZE = 1_000_000_000;
    public static final int MAX_OUTPUT  = Integer.MAX_VALUE;

    private final ItemStackHandler itemHandler = new ItemStackHandler(PANEL_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof SolarPanelBlock;
        }
        @Override public int getSlotLimit(int slot) { return 64; }
    };

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack s) { itemHandler.setStackInSlot(slot, s); }
        @Override public int getSlots() { return PANEL_SLOTS; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack s) { return itemHandler.isItemValid(slot, s); }
    };

    private final EnergyBuffer buffer = new EnergyBuffer(BUFFER_SIZE, MAX_OUTPUT);
    private LazyOptional<IItemHandler>   lazyItems  = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    private int currentGeneration = 0;
    private int currentOutput     = 0;

    protected final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 ->  buffer.getEnergyStored()    & 0xFFFF;
                case 1 -> (buffer.getEnergyStored()    >> 16) & 0xFFFF;
                case 2 ->  buffer.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (buffer.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 ->  currentGeneration           & 0xFFFF;
                case 5 -> (currentGeneration           >> 16) & 0xFFFF;
                case 6 ->  currentOutput               & 0xFFFF;
                case 7 -> (currentOutput               >> 16) & 0xFFFF;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 8; }
    };

    public PanelCombinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PANEL_COMBINER_BE.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() { return itemHandler; }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        boolean changed = false;

        boolean canSee = level.canSeeSky(pos.above());
        boolean isDay  = level.isDay();
        int total = 0;

        if (canSee) {
            for (int i = 0; i < PANEL_SLOTS; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (!(stack.getItem() instanceof BlockItem bi)) continue;
                if (!(bi.getBlock() instanceof SolarPanelBlock sp)) continue;
                SolarPanelTier tier = sp.tier;
                int gen = isDay ? tier.dayGen : tier.nightGen;
                total += gen * stack.getCount();
            }
        }

        currentGeneration = total;
        if (total > 0) {
            buffer.addEnergy(total);
            changed = true;
        }

        int totalOutput = 0;
        if (buffer.getEnergyStored() > 0) {
            int toGive = buffer.getEnergyStored();
            for (Direction dir : Direction.values()) {
                if (toGive <= 0) break;
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor == null) continue;
                var cap = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
                if (!cap.isPresent()) continue;
                IEnergyStorage target = cap.resolve().get();
                if (!target.canReceive()) continue;
                int accepted = target.receiveEnergy(toGive, false);
                if (accepted > 0) {
                    buffer.extractEnergy(accepted, false);
                    toGive      -= accepted;
                    totalOutput += accepted;
                    changed      = true;
                }
            }
        }
        currentOutput = totalOutput;

        if (changed) setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.panel_combiner");
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PanelCombinerMenu(id, inv, this, data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItems.cast();
        if (cap == ForgeCapabilities.ENERGY)       return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItems  = LazyOptional.of(() -> automationHandler);
        lazyEnergy = LazyOptional.of(() -> buffer);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItems.invalidate();
        lazyEnergy.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", buffer.getEnergyStored());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        buffer.setEnergy(tag.getInt("energy"));
    }

    private static class EnergyBuffer extends EnergyStorage {
        EnergyBuffer(int capacity, int maxTransfer) { super(capacity, maxTransfer, maxTransfer); }
        @Override public boolean canReceive() { return false; }
        void addEnergy(int amount) { energy = (int) Math.min((long) capacity, (long) energy + amount); }
        void setEnergy(int amount) { energy = Math.max(0, Math.min(capacity, amount)); }
    }
}
