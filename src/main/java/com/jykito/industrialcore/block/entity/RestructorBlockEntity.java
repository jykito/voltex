package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.api.RestructorRecipeRegistry;
import com.jykito.industrialcore.block.custom.RestructorBlock;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.custom.CatalystItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.menu.RestructorMenu;
import com.jykito.industrialcore.recipe.MachineRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
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
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RestructorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int MAX_RECEIVE = 10_000_000;

    public static final int SLOT_COUNT  = 5;

    private int currentRecipeCost = 10_000_000;
    private int currentRecipeTime = 100;
    private int energyAccumulated = 0;

    private final com.jykito.industrialcore.recipe.CachedRecipe<RestructorRecipeRegistry.RestructorRecipeData> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> !(stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem);
                case 1 -> false;
                case 2 -> stack.getItem() instanceof CatalystItem;
                case 3, 4 -> stack.getItem() == ModItems.ITEM_PUSHER.get()
                          || stack.getItem() == ModItems.ITEM_PULLER.get();
                default -> false;
            };
        }
    };

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return SLOT_COUNT; }
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

    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (energyAccumulated >= currentRecipeCost) return 0;
            if (itemHandler.getStackInSlot(0).isEmpty()) return 0;

            int toReceive = Math.min(maxReceive, currentRecipeCost - energyAccumulated);
            if (toReceive <= 0) return 0;
            if (!simulate) { energyAccumulated += toReceive; setChanged(); }
            return toReceive;
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energyAccumulated; }
        @Override public int getMaxEnergyStored() { return currentRecipeCost; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() {
            return energyAccumulated < currentRecipeCost && !itemHandler.getStackInSlot(0).isEmpty();
        }
    };

    private LazyOptional<IItemHandler>   lazyItemHandler   = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyAccumulated  & 0xFFFF;
                case 1 -> (energyAccumulated  >> 16) & 0xFFFF;
                case 2 -> currentRecipeCost  & 0xFFFF;
                case 3 -> (currentRecipeCost >> 16) & 0xFFFF;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 4; }
    };

    public RestructorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESTRUCTOR_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.restructor");
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RestructorMenu(id, inventory, this, this.data);
    }

    private Optional<RestructorRecipeRegistry.RestructorRecipeData> getRecipe(Level level) {
        return Optional.ofNullable(recipeCache.get(() -> {

            SimpleContainer c = new SimpleContainer(2);
            c.setItem(0, itemHandler.getStackInSlot(0));
            c.setItem(1, itemHandler.getStackInSlot(2));
            Optional<MachineRecipe> mcRecipe = level.getRecipeManager()
                    .getRecipeFor(ModRecipes.MOLECULAR_TRANSFORMING_TYPE.get(), c, level);
            if (mcRecipe.isPresent()) {
                MachineRecipe r = mcRecipe.get();
                currentRecipeTime = Math.max(1, r.getProcessingTime());
                return new RestructorRecipeRegistry.RestructorRecipeData(
                        r.getInputCount(),
                        r.getResultItem(level.registryAccess()),
                        r.getEnergyCost()
                );
            }

            currentRecipeTime = 100;
            return RestructorRecipeRegistry.findMatchData(
                    itemHandler.getStackInSlot(0),
                    itemHandler.getStackInSlot(2)
            ).orElse(null);
        }));
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        boolean changed = false;

        Optional<RestructorRecipeRegistry.RestructorRecipeData> recipe = getRecipe(level);
        if (recipe.isPresent()) {
            currentRecipeCost = recipe.get().energyCost();
            if (energyAccumulated >= currentRecipeCost) {
                ItemStack result = recipe.get().result();
                ItemStack output = itemHandler.getStackInSlot(1);
                if (output.isEmpty() || (output.is(result.getItem()) && output.getCount() < output.getMaxStackSize())) {
                    itemHandler.extractItem(0, recipe.get().inputCount(), false);
                    if (output.isEmpty()) {
                        itemHandler.setStackInSlot(1, result.copy());
                    } else {
                        output.grow(result.getCount());
                    }
                    energyAccumulated = 0;
                    changed = true;
                }
            }
        }

        processUpgrades(level, pos);

        boolean working = energyAccumulated > 0 && recipe.isPresent();
        if (state.getValue(RestructorBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(RestructorBlock.LIT, working), 3);
        }

        if (changed) setChanged();
    }

    private void processUpgrades(Level level, BlockPos pos) {
        for (int i = 3; i <= 4; i++) {
            ItemStack upgrade = itemHandler.getStackInSlot(i);
            if (!(upgrade.getItem() instanceof DirectionalUpgradeItem du)) continue;
            Direction dir = DirectionalUpgradeItem.getStoredDirection(upgrade);
            if (dir == null) continue;
            BlockEntity target = level.getBlockEntity(pos.relative(dir));
            if (target == null) continue;
            switch (du.getMode()) {
                case ITEM_PUSH -> doItemPush(target, dir);
                case ITEM_PULL -> doItemPull(target, dir);
                default -> {}
            }
        }
    }

    private void doItemPush(BlockEntity target, Direction dir) {
        var cap = target.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
        if (!cap.isPresent()) return;
        IItemHandler dst = cap.resolve().get();
        ItemStack stack = itemHandler.getStackInSlot(1);
        if (stack.isEmpty()) return;
        ItemStack leftover = ItemHandlerHelper.insertItem(dst, stack.copy(), false);
        if (leftover.getCount() < stack.getCount())
            itemHandler.setStackInSlot(1, leftover.isEmpty() ? ItemStack.EMPTY : leftover);
    }

    private void doItemPull(BlockEntity target, Direction dir) {
        var cap = target.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
        if (!cap.isPresent()) return;
        IItemHandler src = cap.resolve().get();
        for (int si = 0; si < src.getSlots(); si++) {
            ItemStack avail = src.extractItem(si, 64, true);
            if (avail.isEmpty()) continue;
            ItemStack rem = automationHandler.insertItem(0, avail, true);
            int canTake = avail.getCount() - rem.getCount();
            if (canTake > 0) {
                ItemStack taken = src.extractItem(si, canTake, false);
                automationHandler.insertItem(0, taken, false);
                break;
            }
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        if (cap == ForgeCapabilities.ENERGY)       return lazyEnergyHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler   = LazyOptional.of(() -> automationHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energyAccumulated", energyAccumulated);
        tag.putInt("recipeCost", currentRecipeCost);
        tag.putInt("recipeTime", currentRecipeTime);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag inv = tag.getCompound("inventory");

        if (inv.contains("Size") && inv.getInt("Size") < SLOT_COUNT) {
            inv.putInt("Size", SLOT_COUNT);
        }
        itemHandler.deserializeNBT(inv);
        energyAccumulated = tag.getInt("energyAccumulated");

        if (tag.contains("recipeCost")) currentRecipeCost = tag.getInt("recipeCost");
        if (tag.contains("recipeTime")) currentRecipeTime = Math.max(1, tag.getInt("recipeTime"));
    }

    public void drops(Level level, BlockPos pos) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemHandler.getStackInSlot(i));
        }
    }
}
