package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.MinerCasingBlock;
import com.jykito.industrialcore.block.custom.MinerCoreBlock;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.item.custom.MinerLensItem;
import com.jykito.industrialcore.item.upgrade.AcceleratorItem;
import com.jykito.industrialcore.item.upgrade.ParallelModuleItem;
import com.jykito.industrialcore.item.upgrade.AdvancedParallelModuleItem;
import com.jykito.industrialcore.menu.MinerCoreMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MinerCoreBlockEntity extends BlockEntity implements MenuProvider {

    public static final int BASE_PROGRESS   = 200;
    public static final int ENERGY_PER_TICK = 1000;
    public static final int FLUID_PER_ORE   = 1000;
    public static final int ENERGY_CAPACITY = 100_000;
    // База под работу без модуля: 8 руд. Под параллели бак растёт сам, см. AdaptiveTank.
    public static final int FLUID_CAPACITY  = 8_000;

    private static final class DrainableEnergyStorage extends EnergyStorage {
        DrainableEnergyStorage(int capacity, int maxReceive) { super(capacity, maxReceive, 0); }
        @Override public boolean canExtract() { return false; }
        public void consume(int amount) { this.energy = Math.max(0, this.energy - amount); }
        public void setStored(int amount) { this.energy = Math.min(capacity, Math.max(0, amount)); }
    }

    private final ItemStackHandler itemHandler = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0          -> stack.getItem() instanceof MinerLensItem;
                case 1, 2, 3, 4 -> stack.getItem() instanceof AcceleratorItem
                        || stack.getItem() instanceof ParallelModuleItem
                        || stack.getItem() instanceof AdvancedParallelModuleItem;
                default         -> false;
            };
        }
    };

    private final ItemStackHandler outputHandler = new ItemStackHandler(8) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    private final IItemHandler outputHandlerExternal = new IItemHandler() {
        @Override public int getSlots() { return outputHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return outputHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return outputHandler.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return outputHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    };

    private final DrainableEnergyStorage energyStorage = new DrainableEnergyStorage(ENERGY_CAPACITY, 131_072);

    public final FluidTank fluidTank = new FluidTank(FLUID_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ModFluids.SOURCE_DRILL_FLUID.get();
        }
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide())
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };

    private LazyOptional<IItemHandler>   lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergy      = LazyOptional.empty();
    private LazyOptional<IFluidHandler>  lazyFluid       = LazyOptional.empty();

    private int progress            = 0;
    private int maxProgress         = BASE_PROGRESS;
    private int structureValid      = 0;
    private boolean isStructureValid       = false;
    private int     structureCheckCooldown = 0;
    private int     validityCheckCooldown  = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0  -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1  -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2  -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 3  -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4  -> fluidTank.getFluidAmount() & 0xFFFF;
                case 5  -> (fluidTank.getFluidAmount() >> 16) & 0xFFFF;
                case 6  -> fluidTank.getCapacity() & 0xFFFF;
                case 7  -> (fluidTank.getCapacity() >> 16) & 0xFFFF;
                case 8  -> progress;
                case 9  -> maxProgress;
                case 10 -> structureValid;
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) {
            switch (index) {
                case 8  -> progress       = value;
                case 9  -> maxProgress    = value;
                case 10 -> structureValid = value;
            }
        }
        @Override
        public int getCount() { return 11; }
    };

    public MinerCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINER_CORE_BE.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        if (isStructureValid) {

            if (--validityCheckCooldown <= 0) {
                validityCheckCooldown = 5;
                if (!checkStillValid(level, pos)) {
                    isStructureValid = false;
                    structureValid = 0;

                    level.setBlock(pos, state.setValue(MinerCoreBlock.FORMED, false)
                            .setValue(MinerCoreBlock.LIT, false), 3);
                    setCasingFormed(level, pos, false);
                    setChanged();
                    return;
                }
            }
        } else {

            if (--structureCheckCooldown <= 0) {
                structureCheckCooldown = 40;
                if (checkStructure(level, pos)) {
                    isStructureValid = true;
                    structureValid = 1;
                    validityCheckCooldown = 5;
                    state = state.setValue(MinerCoreBlock.FORMED, true);
                    level.setBlock(pos, state, 3);
                    setCasingFormed(level, pos, true);
                    setChanged();
                }
            }
        }

        float timeMultiplier = 1.0f;
        int moduleParallel = 1;
        for (int s = 1; s <= 4; s++) {
            ItemStack upg = itemHandler.getStackInSlot(s);
            Item it = upg.getItem();
            if (it instanceof AcceleratorItem acc) {
                for (int c = 0; c < upg.getCount(); c++) timeMultiplier *= acc.getSpeedMultiplier();
            } else if (it instanceof ParallelModuleItem pm) {
                moduleParallel = Math.max(moduleParallel, pm.getParallelCount());
            } else if (it instanceof AdvancedParallelModuleItem adv) {
                moduleParallel = Math.max(moduleParallel, adv.getParallelCount());
            }
        }
        if (AdaptiveTank.resize(fluidTank, FLUID_CAPACITY, moduleParallel))
            level.sendBlockUpdated(pos, state, state, 3); // шкала бака считается от ёмкости, клиенту нужна новая

        int effectiveMax = Math.max(1, (int)(BASE_PROGRESS * timeMultiplier));
        this.maxProgress = effectiveMax;

        pushToAdjacentInventories(level, pos);

        ItemStack lensStack = itemHandler.getStackInSlot(0);
        boolean canWork = isStructureValid
                && !lensStack.isEmpty()
                && lensStack.getItem() instanceof MinerLensItem
                && energyStorage.getEnergyStored() >= ENERGY_PER_TICK
                && !isOutputFull();

        boolean hasChanges = false;
        boolean working = false;
        if (canWork) {
            // параллель, которую тянет энергия в этот тик (не больше модуля, не больше запаса)
            int energyParallel = Math.max(1, Math.min(moduleParallel, energyStorage.getEnergyStored() / ENERGY_PER_TICK));
            energyStorage.consume(energyParallel * ENERGY_PER_TICK);
            progress++;
            working = true;
            hasChanges = true;

            if (progress >= effectiveMax) {
                // на завершении параллель ограничена ещё и запасом жижи; сколько реально влезло в вывод — столько и списываем
                int p = Math.min(energyParallel, fluidTank.getFluidAmount() / FLUID_PER_ORE);
                int produced = p >= 1 ? generateOre(level, (MinerLensItem) lensStack.getItem(), p) : 0;
                if (produced > 0) {
                    fluidTank.drain(produced * FLUID_PER_ORE, IFluidHandler.FluidAction.EXECUTE);
                    progress = 0;
                } else {
                    progress = effectiveMax;
                }
            }
        } else if (progress > 0) {
            progress = 0;
            hasChanges = true;
        }

        if (state.getValue(MinerCoreBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(MinerCoreBlock.LIT, working), 3);
        }

        if (hasChanges) setChanged();
    }

    public static void setCasingFormed(Level level, BlockPos center, boolean formed) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState bs = level.getBlockState(p);
                    if (bs.getBlock() instanceof MinerCasingBlock)
                        level.setBlock(p, bs.setValue(MinerCasingBlock.FORMED, formed), 3);
                }
    }

    public static boolean checkStructure(Level level, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockState bs = level.getBlockState(center.offset(dx, dy, dz));
                    if (!(bs.getBlock() instanceof MinerCasingBlock)) return false;
                    if (bs.getValue(MinerCasingBlock.FORMED)) return false;
                }
        return true;
    }

    private static boolean checkStillValid(Level level, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (!(level.getBlockState(center.offset(dx, dy, dz)).getBlock() instanceof MinerCasingBlock))
                        return false;
                }
        return true;
    }

    private int generateOre(Level level, MinerLensItem lens, int max) {
        List<Item> pool = MinerLensItem.buildWeightedPool(lens.getLensType());
        if (pool.isEmpty()) return 0;
        int produced = 0;
        for (int n = 0; n < max; n++) {
            ItemStack ore = new ItemStack(pool.get(level.random.nextInt(pool.size())));
            for (int i = 0; i < outputHandler.getSlots() && !ore.isEmpty(); i++) {
                ore = outputHandler.insertItem(i, ore, false);
            }
            if (ore.isEmpty()) produced++;
            else break;  // вывод забит — дальше не мельчим
        }
        return produced;
    }

    private boolean isOutputFull() {
        for (int i = 0; i < outputHandler.getSlots(); i++) {
            ItemStack s = outputHandler.getStackInSlot(i);
            if (s.isEmpty() || s.getCount() < s.getMaxStackSize()) return false;
        }
        return true;
    }

    private void pushToAdjacentInventories(Level level, BlockPos center) {
        // выход пуст → нечего толкать, не сканируем 26×6 соседей (поведение идентично)
        boolean anyOut = false;
        for (int s = 0; s < outputHandler.getSlots(); s++)
            if (!outputHandler.getStackInSlot(s).isEmpty()) { anyOut = true; break; }
        if (!anyOut) return;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos casingPos = center.offset(dx, dy, dz);
                    for (Direction dir : Direction.values()) {
                        BlockPos neighborPos = casingPos.relative(dir);

                        int nx = neighborPos.getX() - center.getX();
                        int ny = neighborPos.getY() - center.getY();
                        int nz = neighborPos.getZ() - center.getZ();
                        if (Math.abs(nx) <= 1 && Math.abs(ny) <= 1 && Math.abs(nz) <= 1) continue;
                        BlockEntity neighbor = level.getBlockEntity(neighborPos);
                        if (neighbor == null) continue;
                        var optInv = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).resolve();
                        if (optInv.isEmpty()) continue;
                        IItemHandler inv = optInv.get();
                        for (int outSlot = 0; outSlot < outputHandler.getSlots(); outSlot++) {
                            ItemStack stack = outputHandler.getStackInSlot(outSlot);
                            if (stack.isEmpty()) continue;
                            for (int inSlot = 0; inSlot < inv.getSlots(); inSlot++) {
                                ItemStack remaining = inv.insertItem(inSlot, stack, false);
                                int moved = stack.getCount() - remaining.getCount();
                                if (moved > 0) {
                                    outputHandler.extractItem(outSlot, moved, false);
                                    stack = outputHandler.getStackInSlot(outSlot);
                                    setChanged();
                                    if (stack.isEmpty()) break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.industrial_core.miner_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MinerCoreMenu(id, inventory, this, data);
    }

    public ItemStackHandler getItemHandler()   { return itemHandler; }
    public ItemStackHandler getOutputHandler() { return outputHandler; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY)        return lazyEnergy.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluid.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER)  return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        if (!getBlockState().getValue(MinerCoreBlock.FORMED)) return super.getRenderBoundingBox();
        BlockPos p = getBlockPos();
        return new net.minecraft.world.phys.AABB(
                p.getX() - 2, p.getY() - 2, p.getZ() - 2,
                p.getX() + 3, p.getY() + 3, p.getZ() + 3);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> outputHandlerExternal);
        lazyEnergy      = LazyOptional.of(() -> energyStorage);
        lazyFluid       = LazyOptional.of(() -> fluidTank);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergy.invalidate();
        lazyFluid.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.put("output", outputHandler.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("fluidTank", fluidTank.writeToNBT(new CompoundTag()));
        AdaptiveTank.save(tag, "fluidTankCapacity", fluidTank);
        tag.putBoolean("isStructureValid", isStructureValid);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        outputHandler.deserializeNBT(tag.getCompound("output"));
        progress = tag.getInt("progress");
        energyStorage.setStored(tag.getInt("energy"));
        AdaptiveTank.load(tag, "fluidTankCapacity", fluidTank, FLUID_CAPACITY);
        fluidTank.readFromNBT(tag.getCompound("fluidTank"));
        isStructureValid = tag.getBoolean("isStructureValid");
        if (isStructureValid) {
            structureValid = 1;
            validityCheckCooldown = 5;
        }
    }
}
