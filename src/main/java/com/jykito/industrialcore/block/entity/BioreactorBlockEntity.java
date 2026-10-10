package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.BioreactorBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.heat.HeatCapability;
import com.jykito.industrialcore.heat.IHeatHandler;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import com.jykito.industrialcore.item.upgrade.MachineUpgrades;
import com.jykito.industrialcore.item.upgrade.UpgradeTargets;
import com.jykito.industrialcore.menu.BioreactorMenu;
import com.jykito.industrialcore.recipe.BioreactorRecipe;
import com.jykito.industrialcore.recipe.CachedRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Биореактор (Э3): биомасса + добавка + вода → органика без мобов (слизь, кость, порох, кожа, чернила).
 * Культура живёт только в тепле: тепло приходит по теплосети, как у доменки — уровень растёт от притока
 * и остывает без него; ниже порога рецепта реактор стоит. Устроен как промыватель: те же слоты выходов
 * и апгрейдов, плюс слот добавки.
 * <p>
 * Слоты: 0 — биомасса, 1–3 — выходы, 4–7 — апгрейды, 8 — добавка.
 */
public class BioreactorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0, SLOT_ADDITIVE = 8, MAX_HEAT = 2000;
    private static final int TANK_BASE = 8000;

    private final CachedRecipe<BioreactorRecipe> recipeCache = new CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(9) {
        @Override protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot >= 4 && slot <= 7) return UpgradeTargets.works(getBlockState().getBlock(), stack);
            if (slot == SLOT_INPUT) return stack.is(ModItems.BIOMASS.get());
            if (slot == SLOT_ADDITIVE) return !stack.is(ModItems.BIOMASS.get()) && !(stack.getItem() instanceof MachineUpgradeItem);
            return true;
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    /** Снаружи: класть биомассу и добавку, забирать выходы. */
    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot != SLOT_INPUT && slot != SLOT_ADDITIVE) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 1 || slot > 3) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    private final MachineEnergy energyStorage = MachineEnergy.consumer(64000, 64000);
    private LazyOptional<EnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    public final FluidTank waterTank = new FluidTank(TANK_BASE, fs -> fs.getFluid() == Fluids.WATER) {
        @Override protected void onContentsChanged() {
            recipeCache.markDirty();
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };

    private final IFluidHandler fillOnlyHandler = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return waterTank.getFluid(); }
        @Override public int getTankCapacity(int tank) { return waterTank.getCapacity(); }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return waterTank.isFluidValid(stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return waterTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    // ── тепло: как у доменки, уровень от притока, без него остывает ──
    private int heat;
    private boolean receivingHeat;
    private final IHeatHandler heatHandler = new IHeatHandler() {
        @Override public int getHeat() { return heat; }
        @Override public int getMaxHeat() { return MAX_HEAT; }
        @Override public int receiveHeat(int amount, boolean simulate) {
            int accepted = Math.min(Math.max(0, amount), MAX_HEAT - heat);
            if (!simulate && accepted > 0) { heat += accepted; receivingHeat = true; }
            return accepted;
        }
    };
    private LazyOptional<IHeatHandler> lazyHeat = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0, maxProgress = 400, requiredHeat = 0;

    public BioreactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIOREACTOR_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                    case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    case 6 -> heat;
                    case 7 -> requiredHeat;
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) {
                if (index == 0) progress = value;
                if (index == 1) maxProgress = value;
            }
            @Override public int getCount() { return 8; }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        boolean isWorking = false;

        if (!receivingHeat && heat > 0) heat--;
        receivingHeat = false;

        MachineUpgrades upg = MachineUpgrades.scan(itemHandler, 4, 7);
        if (AdaptiveTank.resize(waterTank, TANK_BASE, upg.parallel())) level.sendBlockUpdated(pos, state, state, 3);

        if (upg.fluidPull() != null && waterTank.getSpace() > 0) {
            MachineIO.neighbor(level, pos, upg.fluidPull(), ForgeCapabilities.FLUID_HANDLER).ifPresent(nh -> {
                FluidStack drained = nh.drain(new FluidStack(Fluids.WATER, Math.min(1000, waterTank.getSpace())), IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) waterTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            });
        }
        if (upg.itemPull() != null && MachineIO.pullItems(level, pos, upg.itemPull(), itemHandler, SLOT_INPUT, SLOT_ADDITIVE)) setChanged();

        BioreactorRecipe recipe = findRecipe(level);
        this.maxProgress = upg.scaleTime(recipe != null ? recipe.getProcessingTime() : 400);
        this.requiredHeat = recipe != null ? recipe.getRequiredHeat() : 0;
        int energy = upg.scaleEnergy(recipe != null ? recipe.getEnergyCostPerTick() : 60);

        // культура в холоде не растёт: прогресс не сбрасывается, просто ждёт тепла
        if (recipe != null && heat >= recipe.getRequiredHeat()) {
            if (energyStorage.getEnergyStored() >= energy) {
                energyStorage.consumeEnergy(energy);
                progress++;
                isWorking = true;
                if (progress >= maxProgress) { craft(recipe); progress = 0; }
            }
        } else if (recipe == null) {
            progress = 0;
        }

        if (upg.itemPush() != null && MachineIO.pushItems(level, pos, upg.itemPush(), itemHandler, 1, 2, 3)) setChanged();
        MachineLit.tick(this, level, pos, state, BioreactorBlock.LIT, isWorking);
        setChanged();
    }

    private BioreactorRecipe findRecipe(Level level) {
        return recipeCache.get(() -> {
            ItemStack in = itemHandler.getStackInSlot(SLOT_INPUT), add = itemHandler.getStackInSlot(SLOT_ADDITIVE);
            if (in.isEmpty()) return null;
            for (BioreactorRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.BIOREACTOR_TYPE.get())) {
                if (!r.matches(in, add)) continue;
                if (waterTank.getFluidAmount() < r.getWaterAmount()) continue;
                if (!fits(r.getResult())) continue;
                return r;
            }
            return null;
        });
    }

    private void craft(BioreactorRecipe r) {
        if (!r.matches(itemHandler.getStackInSlot(SLOT_INPUT), itemHandler.getStackInSlot(SLOT_ADDITIVE))) return;
        if (waterTank.getFluidAmount() < r.getWaterAmount() || !fits(r.getResult())) return;
        itemHandler.extractItem(SLOT_INPUT, r.getInputCount(), false);
        if (r.hasAdditive()) itemHandler.extractItem(SLOT_ADDITIVE, r.getAdditiveCount(), false);
        waterTank.drain(r.getWaterAmount(), IFluidHandler.FluidAction.EXECUTE);
        ItemStack out = r.getResult();
        for (int s = 1; s <= 3 && !out.isEmpty(); s++) out = itemHandler.insertItem(s, out, false);
        setChanged();
    }

    private boolean fits(ItemStack stack) {
        ItemStack p = stack.copy();
        for (int s = 1; s <= 3 && !p.isEmpty(); s++) p = itemHandler.insertItem(s, p, true);
        return p.isEmpty();
    }

    public int getHeat() { return heat; }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.bioreactor"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BioreactorMenu(id, inventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY)        return lazyEnergyHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER)  return lazyItemHandler.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        if (cap == HeatCapability.HEAT)             return lazyHeat.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        lazyItemHandler   = LazyOptional.of(() -> automationHandler);
        lazyFluidHandler  = LazyOptional.of(() -> fillOnlyHandler);
        lazyHeat          = LazyOptional.of(() -> heatHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergyHandler.invalidate();
        lazyItemHandler.invalidate();
        lazyFluidHandler.invalidate();
        lazyHeat.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("waterTank", waterTank.writeToNBT(new CompoundTag()));
        AdaptiveTank.save(tag, "waterTankCapacity", waterTank);
        tag.putInt("progress", progress);
        tag.putInt("heat", heat);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        AdaptiveTank.load(tag, "waterTankCapacity", waterTank, TANK_BASE);
        waterTank.readFromNBT(tag.getCompound("waterTank"));
        progress = tag.getInt("progress");
        heat = tag.getInt("heat");
    }
}
