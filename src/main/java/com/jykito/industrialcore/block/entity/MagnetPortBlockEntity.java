package com.jykito.industrialcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Порт-BE на скелет-блоках магнита. Сам ничего не хранит и НЕ тикает.
 * При сборке ядро «усыновляет» порт (bindCore); если блок на входной поверхности (нижняя платформа) —
 * порт отдаёт соседям капы ЯДРА: предметы (вход 0-7 / выход 8), жидкости (только налив), энергию (только приём).
 * Так провода/трубы/хабы коннектятся к платформе, а физически лезут в ядро за 3 блока.
 *
 * Капы реализованы через живые прокси (каждый вызов резолвит текущее ядро по позиции) —
 * поэтому смена/поломка/перестановка ядра не оставляет висячих хендлеров и не теряет предметы.
 */
public class MagnetPortBlockEntity extends BlockEntity {

    private BlockPos corePos = null;
    private boolean input = false;

    private LazyOptional<IItemHandler>   itemCap   = LazyOptional.empty();
    private LazyOptional<IFluidHandler>  fluidCap  = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.empty();

    public MagnetPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGNET_PORT_BE.get(), pos, state);
    }

    /** Текущее ядро (живой резол по позиции; null если порт неактивен, ядро выгружено/сломано). */
    private @Nullable ElectromagnetCoreBlockEntity core() {
        if (!input || corePos == null || level == null) return null;
        return level.getBlockEntity(corePos) instanceof ElectromagnetCoreBlockEntity c && !c.isRemoved() ? c : null;
    }

    // Прокси-хендлеры: стабильная идентичность, но каждый вызов бьёт в живое ядро (или no-op, если его нет).
    private final IItemHandler itemProxy = new IItemHandler() {
        @Override public int getSlots() { var c = core(); return c != null ? c.getExternalItemHandler().getSlots() : 0; }
        @Override public @NotNull ItemStack getStackInSlot(int s) { var c = core(); return c != null ? c.getExternalItemHandler().getStackInSlot(s) : ItemStack.EMPTY; }
        @Override public @NotNull ItemStack insertItem(int s, @NotNull ItemStack st, boolean sim) { var c = core(); return c != null ? c.getExternalItemHandler().insertItem(s, st, sim) : st; }
        @Override public @NotNull ItemStack extractItem(int s, int a, boolean sim) { var c = core(); return c != null ? c.getExternalItemHandler().extractItem(s, a, sim) : ItemStack.EMPTY; }
        @Override public int getSlotLimit(int s) { var c = core(); return c != null ? c.getExternalItemHandler().getSlotLimit(s) : 0; }
        @Override public boolean isItemValid(int s, @NotNull ItemStack st) { var c = core(); return c != null && c.getExternalItemHandler().isItemValid(s, st); }
    };
    private final IFluidHandler fluidProxy = new IFluidHandler() {
        @Override public int getTanks() { var c = core(); return c != null ? c.getExternalFluidHandler().getTanks() : 0; }
        @Override public @NotNull FluidStack getFluidInTank(int t) { var c = core(); return c != null ? c.getExternalFluidHandler().getFluidInTank(t) : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int t) { var c = core(); return c != null ? c.getExternalFluidHandler().getTankCapacity(t) : 0; }
        @Override public boolean isFluidValid(int t, @NotNull FluidStack st) { var c = core(); return c != null && c.getExternalFluidHandler().isFluidValid(t, st); }
        @Override public int fill(FluidStack r, IFluidHandler.FluidAction a) { var c = core(); return c != null ? c.getExternalFluidHandler().fill(r, a) : 0; }
        @Override public @NotNull FluidStack drain(FluidStack r, IFluidHandler.FluidAction a) { var c = core(); return c != null ? c.getExternalFluidHandler().drain(r, a) : FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int m, IFluidHandler.FluidAction a) { var c = core(); return c != null ? c.getExternalFluidHandler().drain(m, a) : FluidStack.EMPTY; }
    };
    private final IEnergyStorage energyProxy = new IEnergyStorage() {
        @Override public int receiveEnergy(int max, boolean sim) { var c = core(); return c != null ? c.getExternalEnergyHandler().receiveEnergy(max, sim) : 0; }
        @Override public int extractEnergy(int max, boolean sim) { var c = core(); return c != null ? c.getExternalEnergyHandler().extractEnergy(max, sim) : 0; }
        @Override public int getEnergyStored() { var c = core(); return c != null ? c.getExternalEnergyHandler().getEnergyStored() : 0; }
        @Override public int getMaxEnergyStored() { var c = core(); return c != null ? c.getExternalEnergyHandler().getMaxEnergyStored() : 0; }
        @Override public boolean canExtract() { var c = core(); return c != null && c.getExternalEnergyHandler().canExtract(); }
        @Override public boolean canReceive() { var c = core(); return c != null && c.getExternalEnergyHandler().canReceive(); }
    };

    /** Ядро при сборке привязывает порт и говорит, входная ли это поверхность (нижняя платформа). */
    public void bindCore(BlockPos core, boolean isInput) {
        if (core.equals(corePos) && isInput == input) return; // без изменений — не дёргаем капы
        boolean wasActive = input;
        corePos = core.immutable();
        input = isInput;
        refreshCaps();
        setChanged();
        // порт ожил (сборка/пересборка) → пнуть соседей, чтобы трубы/провода/хабы перескан капу и переконнектились
        if (input && !wasActive) notifyNeighbors();
    }

    /** Тот же сигнал, что и при установке блока: заставляет соседние трубы/провода/хабы перепросканировать капы. */
    private void notifyNeighbors() {
        if (level != null && !level.isClientSide())
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    /** Разборка мультиблока — порт глохнет, соседи отваливаются. */
    public void unbind() {
        if (corePos == null && !input) return;
        corePos = null;
        input = false;
        refreshCaps();
        setChanged();
    }

    private void refreshCaps() {
        if (input) { // активен — держим капы «живыми»
            if (!itemCap.isPresent())   itemCap   = LazyOptional.of(() -> itemProxy);
            if (!fluidCap.isPresent())  fluidCap  = LazyOptional.of(() -> fluidProxy);
            if (!energyCap.isPresent()) energyCap = LazyOptional.of(() -> energyProxy);
        } else {     // неактивен — рвём, чтобы трубы отсоединились
            dropCaps();
        }
    }

    private void dropCaps() {
        itemCap.invalidate();   itemCap   = LazyOptional.empty();
        fluidCap.invalidate();  fluidCap  = LazyOptional.empty();
        energyCap.invalidate(); energyCap = LazyOptional.empty();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (input) {
            if (cap == ForgeCapabilities.ITEM_HANDLER)  return itemCap.cast();
            if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
            if (cap == ForgeCapabilities.ENERGY)        return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        dropCaps();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (input && corePos != null) { // сохраняем только осмысленное состояние (входной порт)
            tag.putLong("core", corePos.asLong());
            tag.putBoolean("in", true);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("core")) {
            corePos = BlockPos.of(tag.getLong("core"));
            input = tag.getBoolean("in");
        } else {
            corePos = null;
            input = false;
        }
        refreshCaps();
    }
}
