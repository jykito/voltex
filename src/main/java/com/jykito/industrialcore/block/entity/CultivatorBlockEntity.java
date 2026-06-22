package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.CultivatorBlock;
import com.jykito.industrialcore.item.upgrade.AcceleratorItem;
import com.jykito.industrialcore.item.upgrade.CultivatorUpgradeItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.item.upgrade.ParallelModuleItem;
import com.jykito.industrialcore.menu.CultivatorMenu;
import com.jykito.industrialcore.recipe.GrowingRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
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
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CultivatorBlockEntity extends BlockEntity implements MenuProvider {

    public static final TagKey<Item> GROWABLE_PLANTS =
            ItemTags.create(new ResourceLocation("industrial_core", "growable_plants"));

    private static final int TANK_CAP = 128000;
    private static final int WATER_PER_RECIPE = 1000;

    private final com.jykito.industrialcore.recipe.CachedRecipe<GrowingRecipe> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(7) {
        @Override protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> !(stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem);
                case 1 -> false;
                case 2 -> stack.getItem() instanceof CultivatorUpgradeItem;
                case 3, 4, 5, 6 -> isDefaultUpgrade(stack);
                default -> false;
            };
        }
    };

    private static boolean isDefaultUpgrade(ItemStack stack) {
        if (stack.getItem() instanceof AcceleratorItem) return true;
        if (stack.getItem() instanceof ParallelModuleItem) return true;
        if (stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.AdvancedParallelModuleItem) return true;
        if (stack.getItem() instanceof DirectionalUpgradeItem dir)
            return dir.getMode() == DirectionalUpgradeItem.Mode.FLUID_PULL
                || dir.getMode() == DirectionalUpgradeItem.Mode.ITEM_PUSH;
        return false;
    }
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

    private static class CultEnergy extends EnergyStorage {
        CultEnergy(int capacity, int maxReceive) { super(capacity, maxReceive, 0); }
        void consume(int amount) { this.energy = Math.max(0, this.energy - amount); }
        void set(int amount)     { this.energy = Math.min(getMaxEnergyStored(), Math.max(0, amount)); }
    }
    private final CultEnergy energyStorage = new CultEnergy(50_000, 2_000);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    public final FluidTank waterTank = new FluidTank(TANK_CAP, fs -> fs.getFluid() == Fluids.WATER) {
        @Override protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide())
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };

    private final IFluidHandler fillOnlyHandler = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return waterTank.getFluid(); }
        @Override public int getTankCapacity(int tank) { return TANK_CAP; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return stack.getFluid() == Fluids.WATER; }
        @Override public int fill(FluidStack resource, FluidAction action) { return waterTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };
    private LazyOptional<IFluidHandler> lazyFluid = LazyOptional.empty();

    private int progress = 0;
    private int maxProgress = 100;
    protected final ContainerData data;

    public CultivatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CULTIVATOR_BE.get(), pos, state);
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

        int waterBefore = waterTank.getFluidAmount();

        float speedMul = 1.0f, energyMul = 1.0f;
        int parallel = 1;
        Direction fluidPullDir = null, itemPushDir = null;
        for (int s = 3; s <= 6; s++) {
            ItemStack u = itemHandler.getStackInSlot(s);
            if (u.getItem() instanceof AcceleratorItem acc) {
                for (int c = 0; c < u.getCount(); c++) { speedMul *= acc.getSpeedMultiplier(); energyMul *= acc.getEnergyMultiplier(); }
            } else if (u.getItem() instanceof ParallelModuleItem pm) {
                parallel = Math.max(parallel, pm.getParallelCount());
            } else if (u.getItem() instanceof com.jykito.industrialcore.item.upgrade.AdvancedParallelModuleItem adv) {
                parallel = Math.max(parallel, adv.getParallelCount());
            } else if (u.getItem() instanceof DirectionalUpgradeItem dir) {
                Direction d = DirectionalUpgradeItem.getStoredDirection(u);
                if (d != null) {
                    if (dir.getMode() == DirectionalUpgradeItem.Mode.FLUID_PULL) fluidPullDir = d;
                    else if (dir.getMode() == DirectionalUpgradeItem.Mode.ITEM_PUSH) itemPushDir = d;
                }
            }
        }

        boolean working = false;
        ItemStack cultUp = itemHandler.getStackInSlot(2);
        if (cultUp.getItem() instanceof CultivatorUpgradeItem up) {
            GrowingRecipe.Category category = up.getCategory();
            ItemStack input = itemHandler.getStackInSlot(0);

            ItemStack result = ItemStack.EMPTY;
            int time = 100, energyPerTick = 200, inputCount = 1;
            boolean keepInput = true;

            GrowingRecipe recipe = findRecipe(level, input, category);
            if (recipe != null) {
                result = recipe.getOutput().copy();
                time = Math.max(1, recipe.getTime());
                energyPerTick = Math.max(1, recipe.getEnergy());
                inputCount = recipe.getInputCount();
                keepInput = recipe.keepInput();
            } else if (category == GrowingRecipe.Category.PLANT && !input.isEmpty() && input.is(GROWABLE_PLANTS)) {
                result = input.copy();
                result.setCount(1);
            }

            if (!result.isEmpty()) {
                maxProgress = Math.max(1, (int) (time * speedMul));
                int fePerTick = Math.max(1, (int) (energyPerTick * energyMul));
                ItemStack output = itemHandler.getStackInSlot(1);
                int outRoom = output.isEmpty()
                        ? result.getMaxStackSize() / result.getCount()
                        : (ItemStack.isSameItemSameTags(output, result)
                            ? (output.getMaxStackSize() - output.getCount()) / result.getCount() : 0);
                int inputAvail = keepInput ? parallel : input.getCount() / Math.max(1, inputCount);
                int waterAvail = waterTank.getFluidAmount() / WATER_PER_RECIPE;
                int actualParallel = Math.min(parallel, Math.min(outRoom, Math.min(inputAvail, waterAvail)));

                if (actualParallel >= 1 && energyStorage.getEnergyStored() >= fePerTick) {
                    energyStorage.consume(fePerTick);
                    progress++;
                    working = true;
                    if (progress >= maxProgress) {
                        waterTank.drain(WATER_PER_RECIPE * actualParallel, IFluidHandler.FluidAction.EXECUTE);
                        if (!keepInput) itemHandler.extractItem(0, inputCount * actualParallel, false);
                        int produced = result.getCount() * actualParallel;
                        if (output.isEmpty()) {
                            ItemStack o = result.copy(); o.setCount(produced);
                            itemHandler.setStackInSlot(1, o);
                        } else {
                            output.grow(produced);
                        }
                        progress = 0;
                    }
                } else {
                    progress = 0;
                }
            } else {
                progress = 0;
            }
        } else {
            progress = 0;
        }

        if (fluidPullDir != null) {
            final Direction d = fluidPullDir;
            BlockEntity n = level.getBlockEntity(pos.relative(d));
            if (n != null) n.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite()).ifPresent(src ->
                    FluidUtil.tryFluidTransfer(waterTank, src, waterTank.getSpace(), true));
        }
        if (itemPushDir != null && !itemHandler.getStackInSlot(1).isEmpty()) {
            final Direction d = itemPushDir;
            BlockEntity n = level.getBlockEntity(pos.relative(d));
            if (n != null) n.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).ifPresent(dst ->
                    itemHandler.setStackInSlot(1, ItemHandlerHelper.insertItem(dst, itemHandler.getStackInSlot(1), false)));
        }

        if (state.getValue(CultivatorBlock.LIT) != working)
            level.setBlock(pos, state.setValue(CultivatorBlock.LIT, working), 3);

        setChanged();
        if (waterTank.getFluidAmount() != waterBefore)
            level.sendBlockUpdated(pos, state, state, 3);
    }

    @Nullable
    private GrowingRecipe findRecipe(Level level, ItemStack input, GrowingRecipe.Category category) {
        return recipeCache.get(() -> {
            if (input.isEmpty()) return null;
            SimpleContainer c = new SimpleContainer(1);
            c.setItem(0, input);
            for (GrowingRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.GROWING_TYPE.get())) {
                if (r.getCategory() == category && r.matches(c, level)) return r;
            }
            return null;
        });
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.cultivator"); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CultivatorMenu(id, inv, this, this.data);
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
        tag.put("waterTank", waterTank.writeToNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        super.saveAdditional(tag);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.set(tag.getInt("energy"));
        waterTank.readFromNBT(tag.getCompound("waterTank"));
        progress = tag.getInt("progress");
    }

    public FluidStack getFluidInTank() { return waterTank.getFluid(); }
    public int getTankCapacity()       { return waterTank.getCapacity(); }
}
