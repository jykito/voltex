package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.MultiblockPortBlock;
import com.jykito.industrialcore.item.upgrade.PortUpgradeItem;
import com.jykito.industrialcore.menu.ItemFluidPortMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Multiblock input or output port: items, fluid or both; the mode is switched with a button
 * only on an empty port. The number of open slots and the tank volume come from the upgrade.
 * <p>
 * From outside (pipes, hubs, hoppers) the port is visible only in its own mode and only in its own direction:
 * you can insert into an input and extract from an output. The multiblock core works with the contents directly
 * through {@link #getItems()}, {@link #insertOutput} and {@link #getTank()}.
 */
public class ItemFluidPortBlockEntity extends BlockEntity implements MenuProvider {

    private final boolean output;
    /**
     * Items, fluid or both at once. A mixed input is needed by the AE2 pattern provider
     * (it puts the whole pattern in), a mixed output by machines whose result is "item + fluid" into one port.
     */
    public static final int MODE_ITEMS = 0, MODE_FLUIDS = 1, MODE_BOTH = 2;
    private int mode = MODE_ITEMS;
    private int tier;
    /** Auto-exchange with neighbours: the output pushes, the input pulls. On by default. */
    private boolean auto = true;
    private int autoTimer;
    private static final int AUTO_INTERVAL = 10, PULLS_PER_SIDE = 8;
    /** The core puts into the output port; the player and pipes cannot put into it. */
    private boolean internalInsert;

    private final ItemStackHandler items = new ItemStackHandler(PortUpgradeItem.MAX_SLOTS) {
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot < activeSlots() && acceptsItems() && (!output || internalInsert);
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };

    private final ItemStackHandler upgrade = new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return stack.getItem() instanceof PortUpgradeItem; }
        /** The upgrade can be removed only if whatever closes afterwards is empty. */
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return fitsTier(0) ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override protected void onContentsChanged(int slot) { applyTier(); setChanged(); sync(); }
    };

    private final FluidTank tank = new FluidTank(PortUpgradeItem.fluid(0)) {
        @Override public boolean isFluidValid(FluidStack stack) { return acceptsFluids(); }
        @Override protected void onContentsChanged() { setChanged(); sync(); }
    };

    /** Outside access: only in the port's direction. */
    private final IItemHandler itemAccess = new IItemHandler() {
        @Override public int getSlots() { return items.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return output ? stack : items.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return output ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return !output && items.isItemValid(slot, stack); }
    };

    private final IFluidHandler fluidAccess = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int t) { return tank.getFluid(); }
        @Override public int getTankCapacity(int t) { return tank.getCapacity(); }
        @Override public boolean isFluidValid(int t, @NotNull FluidStack stack) { return !output && tank.isFluidValid(stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return output ? 0 : tank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return output ? tank.drain(resource, action) : FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return output ? tank.drain(maxDrain, action) : FluidStack.EMPTY; }
    };

    private LazyOptional<IItemHandler> lazyItems = LazyOptional.empty();
    private LazyOptional<IFluidHandler> lazyFluids = LazyOptional.empty();

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            int amount = tank.getFluidAmount();
            return switch (i) {
                case 0 -> mode;
                case 1 -> tier;
                case 2 -> amount & 0xFFFF;
                case 3 -> (amount >>> 16) & 0xFFFF;
                case 4 -> auto ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return DATA_COUNT; }
    };

    /** Window data cells: mode, tier, tank (2), auto-exchange. */
    public static final int DATA_COUNT = 5;

    /** Core of the structure the port belongs to; set by the core itself during the structure check, not saved. */
    @Nullable private BlockPos corePos;

    public ItemFluidPortBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ITEM_FLUID_PORT_BE.get(), pos, state);
    }

    /** For ME ports: their own block entity type, direction also from the block. */
    public ItemFluidPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.output = state.getBlock() instanceof MultiblockPortBlock b && b.kind() == MultiblockPortBlock.Kind.OUTPUT;
    }

    // ── extension points for subclasses (ME ports in compat/ae2); a regular port leaves them empty ──

    /** Subclass tick before auto-exchange. */
    protected void extraTick(Level level) {}
    /** Initial mode and auto-exchange for subclasses (ME ports: items + fluid, no auto-exchange). */
    protected void initDefaults(int mode, boolean auto) { this.mode = mode; this.auto = auto; }
    /** The upgrade changed or loaded: the subclass fits its tanks. */
    protected void tierChanged(int tier) {}
    /** Whether the subclass contents (extra tanks, patterns) fit into a port of tier {@code t}. */
    protected boolean extraFits(int t) { return true; }

    // ── for the multiblock core ──

    public boolean isOutput()    { return output; }
    public int mode()              { return mode; }
    public boolean acceptsItems()  { return mode != MODE_FLUIDS; }
    public boolean acceptsFluids() { return mode != MODE_ITEMS; }
    public int activeSlots()     { return PortUpgradeItem.slots(tier); }
    public int tier()            { return tier; }
    /** Raw port slots: the core reads and takes from here without direction limits. */
    public ItemStackHandler getItems() { return items; }
    public FluidTank getTank()         { return tank; }
    /** All open tanks of the port, the first is {@link #getTank()}. A regular port has one, an ME input and a pattern port up to three. */
    public List<FluidTank> tanks()     { return List.of(tank); }
    public ItemStackHandler getUpgrade() { return upgrade; }

    /** The core marks its ports during the structure check: the pattern port is labelled in the AE2 terminal with the machine name. */
    public void linkCore(BlockPos core) { corePos = core; }
    @Nullable public BlockPos corePos() { return corePos; }

    /** Put a result into the output port. Returns what did not fit. */
    public ItemStack insertOutput(ItemStack stack, boolean simulate) {
        internalInsert = true;
        try { return ItemHandlerHelper.insertItemStacked(items, stack, simulate); }
        finally { internalInsert = false; }
    }

    // ── mode and upgrade ──

    public boolean isEmpty() {
        if (!tank.isEmpty()) return false;
        for (int i = 0; i < items.getSlots(); i++) if (!items.getStackInSlot(i).isEmpty()) return false;
        return true;
    }

    /** Items <-> fluid. A non-empty port does not switch, otherwise the contents would get stuck. */
    public boolean toggleMode() {
        if (!isEmpty()) return false;
        // items -> fluid -> items + fluid
        mode = (mode + 1) % 3;
        refreshCaps();
        if (level != null) level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        setChanged();
        sync();
        return true;
    }

    public boolean isAuto() { return auto; }

    public void toggleAuto() { auto = !auto; setChanged(); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemFluidPortBlockEntity be) {
        be.extraTick(level);
        if (!be.auto || ++be.autoTimer < AUTO_INTERVAL) return;
        be.autoTimer = 0;
        int[] slots = new int[be.activeSlots()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        for (Direction side : Direction.values()) {
            // skip neighbouring ports: otherwise the output of the same structure would cycle items around
            if (level.getBlockState(pos.relative(side)).getBlock() instanceof MultiblockPortBlock) continue;
            if (be.acceptsFluids()) {
                if (be.output) MachineIO.pushFluid(level, pos, side, be.tank);
                else MachineIO.pullFluid(level, pos, side, be.tank);
            }
            if (be.acceptsItems()) {
                if (be.output) MachineIO.pushItems(level, pos, side, be.items, slots);
                else for (int n = 0; n < PULLS_PER_SIDE && MachineIO.pullItems(level, pos, side, be.items, slots); n++) {}
            }
        }
    }

    /** Whether the contents fit into a port of tier {@code t}. */
    private boolean fitsTier(int t) {
        if (tank.getFluidAmount() > PortUpgradeItem.fluid(t)) return false;
        for (int i = PortUpgradeItem.slots(t); i < items.getSlots(); i++)
            if (!items.getStackInSlot(i).isEmpty()) return false;
        return extraFits(t);
    }

    private void applyTier() {
        tier = PortUpgradeItem.tierOf(upgrade.getStackInSlot(0));
        tank.setCapacity(PortUpgradeItem.fluid(tier));
        tierChanged(tier);
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++)
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i));
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), upgrade.getStackInSlot(0));
    }

    // ── capabilities: the port is visible only in its own mode ──

    @Override public void onLoad() { super.onLoad(); refreshCaps(); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyItems.invalidate(); lazyFluids.invalidate(); }

    /** Recreate the capabilities so that pipes that cached the old mode let go of it. */
    private void refreshCaps() {
        lazyItems.invalidate();
        lazyFluids.invalidate();
        lazyItems = LazyOptional.of(() -> itemAccess);
        lazyFluids = LazyOptional.of(() -> fluidAccess);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && acceptsItems()) return lazyItems.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER && acceptsFluids()) return lazyFluids.cast();
        return super.getCapability(cap, side);
    }

    // ── GUI ──

    @Override public Component getDisplayName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ItemFluidPortMenu(id, inv, this, data);
    }

    // ── saving and tank sync to the client (the GUI draws the fluid from the client copy) ──

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("items", items.serializeNBT());
        writeShared(tag);
    }

    /** Shared by save and sync: mode, upgrade, tank. Items are not sent to the client by the block, the menu carries them. */
    private void writeShared(CompoundTag tag) {
        tag.putInt("mode", mode);
        tag.putBoolean("auto", auto);
        tag.put("upgrade", upgrade.serializeNBT());
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("items")) items.deserializeNBT(tag.getCompound("items"));
        mode = tag.contains("mode") ? tag.getInt("mode") : tag.getBoolean("fluidMode") ? MODE_FLUIDS : MODE_ITEMS;
        auto = !tag.contains("auto") || tag.getBoolean("auto");
        upgrade.deserializeNBT(tag.getCompound("upgrade"));
        tier = PortUpgradeItem.tierOf(upgrade.getStackInSlot(0));
        tank.setCapacity(PortUpgradeItem.fluid(tier));
        tank.readFromNBT(tag.getCompound("tank"));
        tierChanged(tier);
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); writeShared(t); return t; }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    protected void sync() {
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
}
