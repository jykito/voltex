package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.CultivatorBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.item.upgrade.AcceleratorItem;
import com.jykito.industrialcore.item.upgrade.CultivatorUpgradeItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.industrialcore.item.upgrade.ParallelModuleItem;
import com.jykito.industrialcore.menu.CultivatorMenu;
import com.jykito.industrialcore.recipe.CachedRecipe;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Cultivator. T1 is one line "input -> output", T2 is three independent lines sharing water, energy,
 * module and upgrades (parallel modules apply to each line).
 */
public class CultivatorBlockEntity extends BlockEntity implements MenuProvider {

    public static final TagKey<Item> GROWABLE_PLANTS =
            ItemTags.create(new ResourceLocation("industrial_core", "growable_plants"));

    // Base for running without a module: 8 cycles per line. With parallel the tank grows by itself, see AdaptiveTank.
    private static final int TANK_CAP = 8000;
    private static final int WATER_PER_RECIPE = 1000;
    private static final int ENERGY_CAP = 50_000;

    public static final int T2_LANES = 3;

    /** Cultivator module and upgrades: the T1 slots. */
    public static final int SLOT_MODULE = 2, SLOT_UPG_FIRST = 3, SLOT_UPG_LAST = 6;

    /** Line input: the first uses the T1 slots (0 -> 1), the others come after the upgrades (7 -> 8, 9 -> 10). T1 saves do not shift. */
    public static int inSlot(int lane)  { return lane == 0 ? 0 : SLOT_UPG_LAST - 1 + 2 * lane; }
    public static int outSlot(int lane) { return lane == 0 ? 1 : inSlot(lane) + 1; }
    private static boolean isInput(int slot)  { return slot == 0 || slot > SLOT_UPG_LAST && (slot - SLOT_UPG_LAST) % 2 == 1; }
    private static boolean isOutput(int slot) { return slot == 1 || slot > SLOT_UPG_LAST && (slot - SLOT_UPG_LAST) % 2 == 0; }

    private final int lanes;
    private final int tankBase;
    private final CachedRecipe<GrowingRecipe>[] recipeCache;
    private final int[] outSlots;

    private final ItemStackHandler itemHandler;

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
            if (!isInput(slot)) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!isOutput(slot)) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };
    public ItemStackHandler getItemHandler() { return itemHandler; }

    private final MachineEnergy energyStorage; // intake = buffer, no input cap
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    public final FluidTank waterTank;

    private final IFluidHandler fillOnlyHandler = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return waterTank.getFluid(); }
        @Override public int getTankCapacity(int tank) { return tankBase; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return stack.getFluid() == Fluids.WATER; }
        @Override public int fill(FluidStack resource, FluidAction action) { return waterTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };
    private LazyOptional<IFluidHandler> lazyFluid = LazyOptional.empty();

    private final int[] progress;
    private final int[] maxProgress;
    protected final ContainerData data;

    public CultivatorBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CULTIVATOR_BE.get(), pos, state, 1);
    }

    /** T2: three lines; the tank and buffer are three times larger, so each line gets as much as T1. */
    public static CultivatorBlockEntity t2(BlockPos pos, BlockState state) {
        return new CultivatorBlockEntity(ModBlockEntities.CULTIVATOR_T2_BE.get(), pos, state, T2_LANES);
    }

    @SuppressWarnings("unchecked")
    protected CultivatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int lanes) {
        super(type, pos, state);
        this.lanes = lanes;
        this.tankBase = TANK_CAP * lanes;
        this.progress = new int[lanes];
        this.maxProgress = new int[lanes];
        Arrays.fill(maxProgress, 100);
        this.recipeCache = new CachedRecipe[lanes];
        this.outSlots = new int[lanes];
        for (int i = 0; i < lanes; i++) { recipeCache[i] = new CachedRecipe<>(); outSlots[i] = outSlot(i); }
        this.energyStorage = MachineEnergy.consumer(ENERGY_CAP * lanes, ENERGY_CAP * lanes);
        this.waterTank = new FluidTank(tankBase, fs -> fs.getFluid() == Fluids.WATER) {
            @Override protected void onContentsChanged() {
                setChanged();
                if (level != null && !level.isClientSide())
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        };
        this.itemHandler = new ItemStackHandler(Math.max(SLOT_UPG_LAST + 1, outSlot(lanes - 1) + 1)) {   // T1 has all 7 slots
            @Override protected void onContentsChanged(int slot) {
                for (CachedRecipe<GrowingRecipe> c : recipeCache) c.markDirty();
                setChanged();
            }
            @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (slot == SLOT_MODULE) return stack.getItem() instanceof CultivatorUpgradeItem;
                if (slot >= SLOT_UPG_FIRST && slot <= SLOT_UPG_LAST) return isDefaultUpgrade(stack);
                if (isInput(slot)) return !(stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem);
                return false;
            }
        };
        // 0, 1: progress of the first line, 2..5: energy (as on T1), then a "progress, max" pair per line
        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> progress[0];
                    case 1 -> maxProgress[0];
                    case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                    case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    default -> index < getCount() ? ((index & 1) == 0 ? progress[index / 2 - 2] : maxProgress[index / 2 - 2]) : 0;
                };
            }
            @Override public void set(int index, int value) {
                if (index == 0) progress[0] = value;
                if (index == 1) maxProgress[0] = value;
            }
            @Override public int getCount() { return 4 + 2 * lanes; }
        };
    }

    public int getLanes() { return lanes; }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        int waterBefore = waterTank.getFluidAmount();

        MachineUpgrades upg = MachineUpgrades.scan(itemHandler, SLOT_UPG_FIRST, SLOT_UPG_LAST);

        if (AdaptiveTank.resize(waterTank, tankBase, upg.parallel()))
            level.sendBlockUpdated(pos, state, state, 3); // the tank gauge is computed from capacity, the client needs the new one

        boolean working = false;
        ItemStack cultUp = itemHandler.getStackInSlot(SLOT_MODULE);
        if (cultUp.getItem() instanceof CultivatorUpgradeItem up) {
            for (int lane = 0; lane < lanes; lane++)
                working |= tickLane(level, lane, up.getCategory(), upg);
        } else {
            Arrays.fill(progress, 0);
        }

        if (upg.fluidPull() != null) MachineIO.pullFluid(level, pos, upg.fluidPull(), waterTank);
        if (upg.itemPush() != null) MachineIO.pushItems(level, pos, upg.itemPush(), itemHandler, outSlots);

        MachineLit.tick(this, level, pos, state, CultivatorBlock.LIT, working);

        setChanged();
        if (waterTank.getFluidAmount() != waterBefore)
            level.sendBlockUpdated(pos, state, state, 3);
    }

    /** One tick of a line; water and energy are shared, so the lines take them in turn. */
    private boolean tickLane(Level level, int lane, GrowingRecipe.Category category, MachineUpgrades upg) {
        int in = inSlot(lane), out = outSlot(lane);
        ItemStack input = itemHandler.getStackInSlot(in);

        ItemStack result = ItemStack.EMPTY;
        int time = 100, energyPerTick = 200, inputCount = 1;
        boolean keepInput = true;

        GrowingRecipe recipe = findRecipe(level, lane, input, category);
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

        if (result.isEmpty()) { progress[lane] = 0; return false; }

        maxProgress[lane] = upg.scaleTime(time);
        int fePerTick = upg.scaleEnergy(energyPerTick);
        ItemStack output = itemHandler.getStackInSlot(out);
        int outRoom = output.isEmpty()
                ? result.getMaxStackSize() / result.getCount()
                : (ItemStack.isSameItemSameTags(output, result)
                    ? (output.getMaxStackSize() - output.getCount()) / result.getCount() : 0);
        int inputAvail = keepInput ? upg.parallel() : input.getCount() / Math.max(1, inputCount);
        int waterAvail = waterTank.getFluidAmount() / WATER_PER_RECIPE;
        int actualParallel = Math.min(upg.parallel(), Math.min(outRoom, Math.min(inputAvail, waterAvail)));

        if (actualParallel < 1 || energyStorage.getEnergyStored() < fePerTick) { progress[lane] = 0; return false; }

        energyStorage.consumeEnergy(fePerTick);
        if (++progress[lane] >= maxProgress[lane]) {
            waterTank.drain(WATER_PER_RECIPE * actualParallel, IFluidHandler.FluidAction.EXECUTE);
            if (!keepInput) itemHandler.extractItem(in, inputCount * actualParallel, false);
            int produced = result.getCount() * actualParallel;
            if (output.isEmpty()) {
                ItemStack o = result.copy(); o.setCount(produced);
                itemHandler.setStackInSlot(out, o);
            } else {
                output.grow(produced);
            }
            progress[lane] = 0;
        }
        return true;
    }

    @Nullable
    private GrowingRecipe findRecipe(Level level, int lane, ItemStack input, GrowingRecipe.Category category) {
        return recipeCache[lane].get(() -> {
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

    @Override public Component getDisplayName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return lanes == 1 ? new CultivatorMenu(id, inv, this, this.data) : CultivatorMenu.t2(id, inv, this, this.data);
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

    /** Progress key of a line: the first one matches T1. */
    private static String progressKey(int lane) { return lane == 0 ? "progress" : "progress" + lane; }

    @Override protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("waterTank", waterTank.writeToNBT(new CompoundTag()));
        AdaptiveTank.save(tag, "waterTankCapacity", waterTank);
        for (int i = 0; i < lanes; i++) tag.putInt(progressKey(i), progress[i]);
        super.saveAdditional(tag);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        // a smaller save (from the 10-08 jar, where T1 had 2 slots) is raised to the full size, otherwise the size is read from NBT
        CompoundTag inv = tag.getCompound("inventory");
        if (inv.getInt("Size") < itemHandler.getSlots()) inv.putInt("Size", itemHandler.getSlots());
        itemHandler.deserializeNBT(inv);
        energyStorage.setEnergy(tag.getInt("energy"));
        AdaptiveTank.load(tag, "waterTankCapacity", waterTank, tankBase);
        waterTank.readFromNBT(tag.getCompound("waterTank"));
        for (int i = 0; i < lanes; i++) progress[i] = tag.getInt(progressKey(i));
    }

    public FluidStack getFluidInTank() { return waterTank.getFluid(); }
    public int getTankCapacity()       { return waterTank.getCapacity(); }
}
