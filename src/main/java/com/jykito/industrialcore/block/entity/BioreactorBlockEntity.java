package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

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
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bioreactor (E3): biomass + additive + water -> organics without mobs (slime, bone, gunpowder, leather, ink).
 * The culture only lives in heat: heat comes over the heat network, as with the blast furnace; the level rises with inflow
 * and cools without it; below the recipe threshold the reactor stops. Built like the ore washer: the same output
 * and upgrade slots, plus an additive slot.
 * <p>
 * Slots: 0 biomass, 1-3 outputs, 4-7 upgrades, 8 additive.
 */
public class BioreactorBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

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

    /** From outside: insert biomass and additive, extract outputs. */
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

    // ── heat: as with the blast furnace, the level follows inflow and cools without it ──
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
            MachineIO.neighbor(level, pos, upg.fluidPull(), Caps.FLUID_HANDLER).ifPresent(nh -> {
                FluidStack drained = nh.drain(new FluidStack(Fluids.WATER, Math.min(1000, waterTank.getSpace())), IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) waterTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            });
        }
        if (upg.itemPull() != null && MachineIO.pullItems(level, pos, upg.itemPull(), itemHandler, SLOT_INPUT, SLOT_ADDITIVE)) setChanged();

        BioreactorRecipe recipe = findRecipe(level);
        this.maxProgress = upg.scaleTime(recipe != null ? recipe.getProcessingTime() : 400);
        this.requiredHeat = recipe != null ? recipe.getRequiredHeat() : 0;
        int energy = upg.scaleEnergy(recipe != null ? recipe.getEnergyCostPerTick() : 60);

        // the culture does not grow in the cold: progress is not reset, it just waits for heat
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
            for (BioreactorRecipe r : com.jykito.industrialcore.recipe.RecipeLookup.all(level, ModRecipes.BIOREACTOR_TYPE.get())) {
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

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.bioreactor"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BioreactorMenu(id, inventory, this, this.data);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK)        return (T) energyStorage;
        if (cap == Capabilities.ItemHandler.BLOCK)  return (T) automationHandler;
        if (cap == Capabilities.FluidHandler.BLOCK) return (T) fillOnlyHandler;
        if (cap == HeatCapability.HEAT)             return (T) heatHandler;
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
        tag.put("waterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        AdaptiveTank.save(tag, "waterTankCapacity", waterTank);
        tag.putInt("progress", progress);
        tag.putInt("heat", heat);
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        AdaptiveTank.load(tag, "waterTankCapacity", waterTank, TANK_BASE);
        waterTank.readFromNBT(registries, tag.getCompound("waterTank"));
        progress = tag.getInt("progress");
        heat = tag.getInt("heat");
    }
}
