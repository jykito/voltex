package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.NuclearReactorBlock;
import com.jykito.industrialcore.block.ReactorChamberBlock;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.item.custom.ReactorComponentItem;
import com.jykito.industrialcore.item.custom.ReactorFuelRodItem;
import com.jykito.industrialcore.item.custom.ReactorSchemeItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.menu.NuclearReactorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NuclearReactorBlockEntity extends BlockEntity implements MenuProvider {

    private static class ReactorEnergyStorage extends EnergyStorage {
        public ReactorEnergyStorage(int cap, int maxE) { super(cap, 0, maxE); }
        @Override public boolean canReceive() { return false; }
        public void addEnergy(int a) { energy = Math.min(capacity, energy + a); }
        public void setEnergy(int a) { energy = Math.max(0, Math.min(capacity, a)); }
    }

    public static final int GRID_COLS        = 5;
    public static final int GRID_ROWS        = 5;
    public static final int GRID_SLOTS       = 25;
    public static final int SLOT_COOLANT_IN  = 25;
    public static final int SLOT_COOLANT_OUT = 26;
    public static final int SLOT_DEPLETED    = 27;
    public static final int SLOT_HOT_IN      = 28;
    public static final int SLOT_HOT_OUT     = 29;
    public static final int SLOT_UPG_START   = 30;
    public static final int SLOT_UPG_END     = 33;
    public static final int SLOT_SCHEME      = 34;
    public static final int SLOT_COUNT       = 35;
    public static final int MAX_ENERGY       = 10_000_000;
    public static final int MAX_EXTRACT      = MAX_ENERGY;
    public static final int MAX_CELL_HEAT    = 50_000;
    public static final int MAX_HULL_HEAT    = 500_000;
    public static final int DANGER_HULL_HEAT = 350_000;
    public static final int TANK_CAPACITY    = 16_000;
    public static final int DATA_COUNT       = 36;

    private static final int NATURAL_HULL_COOLING     = 5;
    private static final int COOLANT_INTERVAL         = 20;
    private static final int COOLANT_DRAIN_MB         = 100;
    private static final int COOLANT_HULL_COOLING     = 5_000;
    private static final int CAPSULE_CAPACITY         = 1000;

    private static final int COMPONENT_OVERHEAT_THRESHOLD = 10_000;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < GRID_SLOTS) {
                ItemStack schemeStack = getStackInSlot(SLOT_SCHEME);
                if (!(schemeStack.getItem() instanceof ReactorSchemeItem schemeItem))
                    return stack.getItem() instanceof ReactorFuelRodItem || stack.getItem() instanceof ReactorComponentItem;
                char c = schemeItem.getScheme().layout().charAt(slot);
                if (c == '.') return false;
                return ReactorSchemeItem.charMatchesStack(c, stack);
            }
            if (slot == SLOT_COOLANT_IN)
                return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(fh -> {
                    FluidStack f = fh.getFluidInTank(0);
                    return !f.isEmpty() && f.getFluid() == ModFluids.SOURCE_COOLANT.get();
                }).orElse(false);
            if (slot == SLOT_HOT_IN)
                return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                        .map(fh -> fh.getFluidInTank(0).isEmpty()).orElse(false);
            if (slot == SLOT_COOLANT_OUT || slot == SLOT_DEPLETED || slot == SLOT_HOT_OUT) return false;
            if (slot >= SLOT_UPG_START && slot <= SLOT_UPG_END)
                return stack.getItem() instanceof DirectionalUpgradeItem;
            if (slot == SLOT_SCHEME) return stack.getItem() instanceof ReactorSchemeItem;
            return false;
        }
    };

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int s, @NotNull ItemStack st) { itemHandler.setStackInSlot(s, st); }
        @Override public int getSlots() { return SLOT_COUNT; }
        @Override public @NotNull ItemStack getStackInSlot(int s) { return itemHandler.getStackInSlot(s); }
        @Override public @NotNull ItemStack insertItem(int s, @NotNull ItemStack stack, boolean sim) {
            if (s == SLOT_SCHEME) return stack;
            if (s == SLOT_COOLANT_OUT || s == SLOT_DEPLETED || s == SLOT_HOT_OUT) return stack;
            if (s >= SLOT_UPG_START && s <= SLOT_UPG_END) return stack;
            return itemHandler.insertItem(s, stack, sim);
        }
        @Override public @NotNull ItemStack extractItem(int s, int amt, boolean sim) {
            if (s < GRID_SLOTS) {

                ItemStack stack = itemHandler.getStackInSlot(s);
                if (stack.isEmpty() || stack.getItem() instanceof ReactorFuelRodItem
                        || stack.getItem() instanceof ReactorComponentItem) return ItemStack.EMPTY;
                return itemHandler.extractItem(s, amt, sim);
            }
            if (s == SLOT_COOLANT_IN || s == SLOT_HOT_IN) return ItemStack.EMPTY;
            if (s >= SLOT_UPG_START && s <= SLOT_UPG_END) return ItemStack.EMPTY;
            if (s == SLOT_SCHEME) return ItemStack.EMPTY;
            return itemHandler.extractItem(s, amt, sim);
        }
        @Override public int getSlotLimit(int s) { return itemHandler.getSlotLimit(s); }
        @Override public boolean isItemValid(int s, @NotNull ItemStack st) { return itemHandler.isItemValid(s, st); }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    private final FluidTank coolantTank = new FluidTank(TANK_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack s) { return s.getFluid() == ModFluids.SOURCE_COOLANT.get(); }
    };
    private final FluidTank hotCoolantTank = new FluidTank(TANK_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack s) { return s.getFluid() == ModFluids.SOURCE_HOT_COOLANT.get(); }
    };

    private final IFluidHandler combinedCoolantHandler = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) {
            return tank == 0 ? coolantTank.getFluid() : hotCoolantTank.getFluid();
        }
        @Override public int getTankCapacity(int tank) {
            return tank == 0 ? coolantTank.getCapacity() : hotCoolantTank.getCapacity();
        }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank == 0 && stack.getFluid() == ModFluids.SOURCE_COOLANT.get();
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (resource.getFluid() != ModFluids.SOURCE_COOLANT.get()) return 0;
            return coolantTank.fill(resource, action);
        }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getFluid() != ModFluids.SOURCE_HOT_COOLANT.get()) return FluidStack.EMPTY;
            return hotCoolantTank.drain(resource, action);
        }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return hotCoolantTank.drain(maxDrain, action);
        }
    };

    private final ReactorEnergyStorage energyStorage = new ReactorEnergyStorage(MAX_ENERGY, MAX_EXTRACT);
    private LazyOptional<IItemHandler>   lazyItemHandler    = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler  = LazyOptional.empty();
    private LazyOptional<IFluidHandler>  lazyCoolantHandler = LazyOptional.empty();

    private final int[] cellHeat = new int[GRID_SLOTS];
    private int hullHeat = 0, energyPerTick = 0, coolantTimer = 0, chamberCheckTimer = 0, upgradeTimer = 0;
    private int schemeIndex = -1;
    private boolean schemeMatch = false;
    private boolean isFormed = false;

    protected final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 ->  energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 ->  hullHeat & 0xFFFF;
                case 3 -> (hullHeat >> 16) & 0xFFFF;
                case 4 ->  coolantTank.getFluidAmount();
                case 5 ->  hotCoolantTank.getFluidAmount();
                case 6 ->  energyPerTick & 0xFFFF;
                case 7 -> (energyPerTick >> 16) & 0xFFFF;
                default -> {
                    if (i == 33) yield schemeIndex;
                    if (i == 34) yield schemeMatch ? 1 : 0;
                    if (i == 35) yield isFormed ? 1 : 0;
                    int ci = i - 8;
                    yield (ci >= 0 && ci < GRID_SLOTS) ? cellHeat[ci] : 0;
                }
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return DATA_COUNT; }
    };

    public NuclearReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUCLEAR_REACTOR_BE.get(), pos, state);
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.industrial_core.nuclear_reactor");
    }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new NuclearReactorMenu(id, inv, this, data);
    }

    public static boolean checkStructure(Level level, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (!(level.getBlockState(center.offset(dx, dy, dz)).getBlock()
                            instanceof com.jykito.industrialcore.block.ReactorChamberBlock))
                        return false;
                }
        return true;
    }

    private void validateScheme() {
        ItemStack schemeStack = itemHandler.getStackInSlot(SLOT_SCHEME);
        if (!(schemeStack.getItem() instanceof ReactorSchemeItem schemeItem)) {
            schemeMatch = false;
            schemeIndex = -1;
            return;
        }
        schemeIndex = schemeItem.getSchemeIndex();
        String layout = schemeItem.getScheme().layout();
        for (int i = 0; i < GRID_SLOTS; i++) {
            if (!ReactorSchemeItem.charMatchesStack(layout.charAt(i), itemHandler.getStackInSlot(i))) {
                schemeMatch = false;
                return;
            }
        }
        schemeMatch = true;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        boolean changed = false;
        if (++chamberCheckTimer >= 20) {
            chamberCheckTimer = 0;
            boolean newFormed = checkStructure(level, pos);
            if (newFormed != isFormed) {
                isFormed = newFormed;
                level.setBlock(pos, getBlockState().setValue(NuclearReactorBlock.FORMED, isFormed), 3);
                setChanged();
                if (level instanceof net.minecraft.server.level.ServerLevel sl)
                    sl.getChunkSource().blockChanged(getBlockPos());
            }
        }
        if (drainCapsuleIntoTank()) changed = true;

        validateScheme();

        int totalEnergy = 0;
        if (isFormed && schemeMatch) {
            for (int i = 0; i < GRID_SLOTS; i++) {
                ItemStack s = itemHandler.getStackInSlot(i);
                if (!(s.getItem() instanceof ReactorFuelRodItem rod)) continue;
                int mult = getNeutronMultiplier(i);
                cellHeat[i] += rod.getHeatPerTick() * mult;
                totalEnergy  += rod.getEnergyPerTick() * mult;
                changed = true;
            }
        }
        energyPerTick = totalEnergy;
        if (totalEnergy > 0) energyStorage.addEnergy(totalEnergy);

        for (int i = 0; i < GRID_SLOTS; i++) {
            ItemStack s = itemHandler.getStackInSlot(i);
            if (!(s.getItem() instanceof ReactorComponentItem rc)) continue;
            if (rc.getComponentType() != ReactorComponentItem.ComponentType.HEAT_EXCHANGER
                    && rc.getComponentType() != ReactorComponentItem.ComponentType.ADVANCED_EXCHANGER) continue;
            for (int adj : getAdjacentSlots(i)) {
                int delta = cellHeat[adj] - cellHeat[i];
                if (delta == 0) continue;
                int t = Math.min(rc.getCoolingRate(), Math.abs(delta) / 2 + 1);
                if (delta > 0) { cellHeat[i] += t; cellHeat[adj] -= t; }
                else           { cellHeat[i] -= t; cellHeat[adj] += t; }
            }
            changed = true;
        }

        for (int i = 0; i < GRID_SLOTS; i++) {
            ItemStack s = itemHandler.getStackInSlot(i);
            if (!(s.getItem() instanceof ReactorComponentItem rc)) continue;
            switch (rc.getComponentType()) {
                case HEAT_VENT, GRAPHITE ->
                    cellHeat[i] = Math.max(0, cellHeat[i] - rc.getCoolingRate());
                case COMPONENT_VENT, INTEGRAL_VENT -> {
                    for (int adj : getAdjacentSlots(i))
                        cellHeat[adj] = Math.max(0, cellHeat[adj] - rc.getCoolingRate());
                }
                case ADVANCED_VENT ->
                    cellHeat[i] = Math.max(0, cellHeat[i] - rc.getCoolingRate());
                case CONDENSATOR -> {
                    if (!rc.isCondensatorFull(s) && cellHeat[i] > 0) {
                        int absorb = Math.min(cellHeat[i], rc.getMaxAbsorb() - rc.getAbsorbedHeat(s));
                        rc.addAbsorbedHeat(s, absorb);
                        cellHeat[i] -= absorb;
                        if (rc.isCondensatorFull(s)) itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
                default -> {}
            }
            changed = true;
        }

        for (int i = 0; i < GRID_SLOTS; i++) {
            if (cellHeat[i] <= COMPONENT_OVERHEAT_THRESHOLD) continue;
            ItemStack s = itemHandler.getStackInSlot(i);
            if (s.isEmpty() || s.getItem() instanceof ReactorFuelRodItem) continue;
            if (s.getItem() instanceof ReactorComponentItem rc
                    && rc.getComponentType() == ReactorComponentItem.ComponentType.ADVANCED_VENT) continue;
            if (!s.isDamageableItem()) continue;
            s.hurt(1, level.random, null);
            if (s.getDamageValue() >= s.getMaxDamage()) itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            changed = true;
        }

        for (int i = 0; i < GRID_SLOTS; i++) {
            if (cellHeat[i] > MAX_CELL_HEAT) {
                hullHeat += cellHeat[i] - MAX_CELL_HEAT;
                cellHeat[i] = MAX_CELL_HEAT;
            }
        }

        if (hullHeat > 0) { hullHeat = Math.max(0, hullHeat - NATURAL_HULL_COOLING); changed = true; }

        if (hullHeat > 10_000) {
            if (++coolantTimer >= COOLANT_INTERVAL) {
                coolantTimer = 0;
                if (coolantTank.getFluidAmount() >= COOLANT_DRAIN_MB) {
                    coolantTank.drain(COOLANT_DRAIN_MB, IFluidHandler.FluidAction.EXECUTE);
                    hullHeat = Math.max(0, hullHeat - COOLANT_HULL_COOLING);
                    hotCoolantTank.fill(
                            new FluidStack(ModFluids.SOURCE_HOT_COOLANT.get(), COOLANT_DRAIN_MB),
                            IFluidHandler.FluidAction.EXECUTE);
                    changed = true;
                }
            }
        } else { coolantTimer = 0; }

        if (hullHeat >= MAX_HULL_HEAT) {
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    20.0f, true, Level.ExplosionInteraction.TNT);
            return;
        }

        if (level.getGameTime() % 20 == 0) {
            for (int i = 0; i < GRID_SLOTS; i++) {
                ItemStack s = itemHandler.getStackInSlot(i);
                if (!(s.getItem() instanceof ReactorFuelRodItem rod)) continue;
                int dmg = s.getDamageValue() + 1;
                if (dmg >= s.getMaxDamage()) {
                    itemHandler.setStackInSlot(i, new ItemStack(rod.getDepletedVariant()));
                } else {
                    ItemStack copy = s.copy(); copy.setDamageValue(dmg);
                    itemHandler.setStackInSlot(i, copy);
                }
                changed = true;
            }
        }

        if (fillHotCapsule()) changed = true;

        boolean isActive = energyPerTick > 0;
        if (state.getValue(NuclearReactorBlock.LIT) != isActive) {
            level.setBlock(pos, state.setValue(NuclearReactorBlock.LIT, isActive), 3);
            if (isActive) com.jykito.industrialcore.compat.DynamicLightsCompat.registerSource(this);
            else          com.jykito.industrialcore.compat.DynamicLightsCompat.removeSource(this);
        }

        if (distributeEnergy()) changed = true;
        if (++upgradeTimer >= 20) { upgradeTimer = 0; processUpgrades(); changed = true; }
        if (changed) setChanged();
    }

    private int getNeutronMultiplier(int slot) {
        int mult = 1;
        for (int adj : getAdjacentSlots(slot)) {
            ItemStack ns = itemHandler.getStackInSlot(adj);
            if (ns.getItem() instanceof ReactorFuelRodItem) mult++;
            else if (ns.getItem() instanceof ReactorComponentItem rc
                     && rc.getComponentType() == ReactorComponentItem.ComponentType.GRAPHITE) mult++;
        }
        return mult;
    }

    public static int[] getAdjacentSlots(int slot) {
        int row = slot / GRID_COLS, col = slot % GRID_COLS;
        int[] buf = new int[4]; int n = 0;
        if (row > 0)           buf[n++] = slot - GRID_COLS;
        if (row < GRID_ROWS-1) buf[n++] = slot + GRID_COLS;
        if (col > 0)           buf[n++] = slot - 1;
        if (col < GRID_COLS-1) buf[n++] = slot + 1;
        int[] r = new int[n]; System.arraycopy(buf, 0, r, 0, n); return r;
    }

    private boolean drainCapsuleIntoTank() {
        if (coolantTank.getSpace() < CAPSULE_CAPACITY) return false;
        ItemStack cap = itemHandler.getStackInSlot(SLOT_COOLANT_IN);
        if (cap.isEmpty()) return false;
        ItemStack single = cap.copyWithCount(1);
        var opt = single.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
        if (!opt.isPresent()) return false;
        var h = opt.resolve().get();
        FluidStack inCap = h.getFluidInTank(0);
        if (inCap.isEmpty() || inCap.getFluid() != ModFluids.SOURCE_COOLANT.get()) return false;
        int toT = Math.min(inCap.getAmount(), coolantTank.getSpace());
        FluidStack xfer = new FluidStack(ModFluids.SOURCE_COOLANT.get(), toT);
        h.drain(xfer, IFluidHandler.FluidAction.EXECUTE);
        coolantTank.fill(xfer, IFluidHandler.FluidAction.EXECUTE);
        cap.shrink(1);
        if (cap.isEmpty()) itemHandler.setStackInSlot(SLOT_COOLANT_IN, ItemStack.EMPTY);
        if (h.getFluidInTank(0).isEmpty()) {
            ItemStack out = itemHandler.getStackInSlot(SLOT_COOLANT_OUT), empty = h.getContainer();
            if (out.isEmpty()) itemHandler.setStackInSlot(SLOT_COOLANT_OUT, empty);
            else if (ItemStack.isSameItemSameTags(out, empty) && out.getCount() < out.getMaxStackSize()) out.grow(1);
        }
        return true;
    }

    private boolean fillHotCapsule() {
        if (hotCoolantTank.getFluidAmount() < CAPSULE_CAPACITY) return false;
        ItemStack cap = itemHandler.getStackInSlot(SLOT_HOT_IN);
        if (cap.isEmpty()) return false;
        ItemStack out = itemHandler.getStackInSlot(SLOT_HOT_OUT);
        if (!out.isEmpty() && (out.getItem() != cap.getItem() || out.getCount() >= out.getMaxStackSize())) return false;
        ItemStack single = cap.copyWithCount(1);
        var opt = single.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
        if (!opt.isPresent()) return false;
        var h = opt.resolve().get();
        if (!h.getFluidInTank(0).isEmpty()) return false;
        int filled = h.fill(new FluidStack(ModFluids.SOURCE_HOT_COOLANT.get(), CAPSULE_CAPACITY), IFluidHandler.FluidAction.EXECUTE);
        if (filled <= 0) return false;
        hotCoolantTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        cap.shrink(1);
        if (cap.isEmpty()) itemHandler.setStackInSlot(SLOT_HOT_IN, ItemStack.EMPTY);
        ItemStack fc = h.getContainer();
        if (out.isEmpty()) itemHandler.setStackInSlot(SLOT_HOT_OUT, fc); else out.grow(1);
        return true;
    }

    private boolean distributeEnergy() {
        if (energyStorage.getEnergyStored() <= 0) return false;
        boolean pushed = false;
        int toGive = Math.min(energyStorage.getEnergyStored(), MAX_EXTRACT);
        outer:
        for (Direction dir : Direction.values()) {
            BlockPos base = worldPosition.relative(dir, 2);
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    BlockPos np = switch (dir.getAxis()) {
                        case X -> base.offset(0, a, b);
                        case Y -> base.offset(a, 0, b);
                        case Z -> base.offset(a, b, 0);
                    };
                    BlockEntity be = level.getBlockEntity(np);
                    if (be == null) continue;
                    var cap = be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
                    if (!cap.isPresent()) continue;
                    IEnergyStorage ne = cap.resolve().get();
                    if (!ne.canReceive()) continue;
                    int accepted = ne.receiveEnergy(toGive, false);
                    if (accepted > 0) {
                        energyStorage.extractEnergy(accepted, false);
                        toGive -= accepted;
                        pushed = true;
                    }
                    if (toGive <= 0) break outer;
                }
            }
        }
        return pushed;
    }

    private void processUpgrades() {
        for (int i = SLOT_UPG_START; i <= SLOT_UPG_END; i++) {
            ItemStack up = itemHandler.getStackInSlot(i);
            if (!(up.getItem() instanceof DirectionalUpgradeItem du)) continue;
            Direction dir = DirectionalUpgradeItem.getStoredDirection(up); if (dir == null) continue;
            BlockPos tp = worldPosition.relative(dir);
            if (level.getBlockState(tp).getBlock() instanceof ReactorChamberBlock) tp = tp.relative(dir);
            BlockEntity be = level.getBlockEntity(tp); if (be == null) continue;
            switch (du.getMode()) {
                case ITEM_PUSH  -> doItemPush(be, dir);
                case ITEM_PULL  -> doItemPull(be, dir);
                case FLUID_PUSH -> doFluidPush(be, dir);
                case FLUID_PULL -> doFluidPull(be, dir);
            }
        }
    }

    private void doItemPush(BlockEntity be, Direction dir) {
        var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
        if (!cap.isPresent()) return;
        var dst = cap.resolve().get();
        for (int s : new int[]{SLOT_COOLANT_OUT, SLOT_DEPLETED, SLOT_HOT_OUT}) {
            ItemStack stack = itemHandler.getStackInSlot(s); if (stack.isEmpty()) continue;
            ItemStack toSend = stack.copyWithCount(Math.min(stack.getCount(), 4));
            for (int di = 0; di < dst.getSlots(); di++) {
                int sent = toSend.getCount() - dst.insertItem(di, toSend, false).getCount();
                if (sent > 0) { itemHandler.extractItem(s, sent, false); break; }
            }
        }
    }

    private void doItemPull(BlockEntity be, Direction dir) {
        var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
        if (!cap.isPresent()) return;
        var src = cap.resolve().get();
        for (int si = 0; si < src.getSlots(); si++) {
            ItemStack avail = src.extractItem(si, 4, true); if (avail.isEmpty()) continue;
            for (int ri = 0; ri < SLOT_COUNT; ri++) {
                int can = avail.getCount() - automationHandler.insertItem(ri, avail, true).getCount();
                if (can > 0) { automationHandler.insertItem(ri, src.extractItem(si, can, false), false); break; }
            }
        }
    }

    private void doFluidPush(BlockEntity be, Direction dir) {
        var cap = be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite());
        if (!cap.isPresent()) return;
        var dst = cap.resolve().get();
        FluidStack toPush = hotCoolantTank.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        if (!toPush.isEmpty()) {
            int a = dst.fill(toPush, IFluidHandler.FluidAction.EXECUTE);
            if (a > 0) hotCoolantTank.drain(a, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void doFluidPull(BlockEntity be, Direction dir) {
        var cap = be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite());
        if (!cap.isPresent()) return;
        var src = cap.resolve().get();
        if (coolantTank.getSpace() <= 0) return;
        FluidStack want = new FluidStack(ModFluids.SOURCE_COOLANT.get(), Math.min(1000, coolantTank.getSpace()));
        FluidStack got  = src.drain(want, IFluidHandler.FluidAction.SIMULATE);
        if (!got.isEmpty() && got.getFluid() == ModFluids.SOURCE_COOLANT.get()) {
            int canFill = coolantTank.fill(got, IFluidHandler.FluidAction.SIMULATE);
            if (canFill > 0) coolantTank.fill(
                    src.drain(new FluidStack(ModFluids.SOURCE_COOLANT.get(), canFill), IFluidHandler.FluidAction.EXECUTE),
                    IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY)        return lazyEnergyHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER)  return lazyItemHandler.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyCoolantHandler.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() {
        super.onLoad();
        lazyItemHandler    = LazyOptional.of(() -> automationHandler);
        lazyEnergyHandler  = LazyOptional.of(() -> energyStorage);
        lazyCoolantHandler = LazyOptional.of(() -> combinedCoolantHandler);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate(); lazyEnergyHandler.invalidate(); lazyCoolantHandler.invalidate();
    }

    public int getHullHeat()         { return hullHeat; }
    public int getCellHeat(int i)    { return (i >= 0 && i < GRID_SLOTS) ? cellHeat[i] : 0; }
    public int getEnergyPerTick()    { return energyPerTick; }
    public int getCoolantAmount()    { return coolantTank.getFluidAmount(); }
    public int getHotCoolantAmount() { return hotCoolantTank.getFluidAmount(); }
    public boolean isFormed()        { return isFormed; }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        if (!isFormed) return super.getRenderBoundingBox();
        net.minecraft.core.BlockPos p = getBlockPos();

        return new net.minecraft.world.phys.AABB(
                p.getX() - 2, p.getY() - 2, p.getZ() - 2,
                p.getX() + 3, p.getY() + 3, p.getZ() + 3);
    }

    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("isFormed", isFormed);
        return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        isFormed = tag.getBoolean("isFormed");
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) isFormed = tag.getBoolean("isFormed");
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("hullHeat", hullHeat);
        tag.putIntArray("cellHeat", cellHeat);
        tag.putInt("coolantTimer", coolantTimer);
        tag.put("coolantTank",    coolantTank.writeToNBT(new CompoundTag()));
        tag.put("hotCoolantTank", hotCoolantTank.writeToNBT(new CompoundTag()));
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        hullHeat     = tag.getInt("hullHeat");
        coolantTimer = tag.getInt("coolantTimer");
        if (tag.contains("cellHeat")) {
            int[] saved = tag.getIntArray("cellHeat");
            for (int i = 0; i < Math.min(saved.length, GRID_SLOTS); i++) cellHeat[i] = saved[i];
        }
        coolantTank.readFromNBT(tag.getCompound("coolantTank"));
        hotCoolantTank.readFromNBT(tag.getCompound("hotCoolantTank"));
    }
}
