package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.logistics.FluidConnectorBlock;
import com.jykito.industrialcore.block.custom.logistics.LogisticsConnectorMode;
import com.jykito.industrialcore.item.upgrade.ConnectorUpgradeItem;
import net.minecraftforge.items.ItemStackHandler;
import com.jykito.industrialcore.menu.FluidConnectorMenu;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FluidConnectorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int FILTER_SLOTS = 5;

    public final Set<BlockPos> linkedPositions = new HashSet<>();
    public LogisticsConnectorMode mode = LogisticsConnectorMode.PUSH;
    public byte transferState = 0;

    public final FluidStack[] fluidFilters = new FluidStack[FILTER_SLOTS];
    public boolean filterWhitelist = true;
    public boolean filterMatchNbt  = false;

    public final ItemStackHandler upgradeHandler = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            return stack.getItem() instanceof ConnectorUpgradeItem;
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };

    private int tickCounter = 0;
    private static final int INTERVAL = 20;
    private static final int MAX_MB   = 500;

    public FluidConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_CONNECTOR_BE.get(), pos, state);
        Arrays.fill(fluidFilters, FluidStack.EMPTY);
    }

    public void addLink(BlockPos other) { linkedPositions.add(other); setChanged(); sync(); }
    public void removeLink(BlockPos other) { linkedPositions.remove(other); setChanged(); sync(); }

    public void setFluidFilter(int slot, FluidStack fluid) {
        fluidFilters[slot] = fluid;
        setChanged(); sync();
    }

    public void clearAllLinks(Level level) {
        for (BlockPos linked : new HashSet<>(linkedPositions)) {
            BlockEntity be = level.getBlockEntity(linked);
            if (be instanceof FluidConnectorBlockEntity c) c.removeLink(worldPosition);
            else if (be instanceof LogisticsNodeBlockEntity n) n.removeLink(worldPosition);
        }
        linkedPositions.clear();
        setChanged(); sync();
    }

    public void cycleMode() {
        mode = mode.next();
        if (level != null && !level.isClientSide())
            level.setBlock(worldPosition,
                    getBlockState().setValue(FluidConnectorBlock.PULL,
                            mode == LogisticsConnectorMode.PULL), 3);
        setChanged(); sync();
    }

    public void sync() {
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public boolean matchesFilter(FluidStack fluid) {
        boolean hasFilter = false;
        for (FluidStack f : fluidFilters) {
            if (f.isEmpty()) continue;
            hasFilter = true;
            if (fluidMatches(fluid, f)) return filterWhitelist;
        }
        if (!hasFilter) return true;
        return !filterWhitelist;
    }

    private boolean fluidMatches(FluidStack fluid, FluidStack filter) {
        if (fluid.getFluid() != filter.getFluid()) return false;
        if (filterMatchNbt) {
            CompoundTag ft = filter.getTag(), st = fluid.getTag();
            if (ft == null && st != null) return false;
            if (ft != null && !ft.equals(st)) return false;
        }
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidConnectorBlockEntity be) {

        net.minecraft.world.item.ItemStack upStack = be.upgradeHandler.getStackInSlot(0);
        int interval = INTERVAL;
        int maxMb    = MAX_MB;
        if (!upStack.isEmpty() && upStack.getItem() instanceof ConnectorUpgradeItem u) {
            interval = u.getInterval();
            maxMb    = u.getMaxMb();
        }

        be.tickCounter++;
        if (be.tickCounter < interval) return;
        be.tickCounter = 0;
        if (be.mode != LogisticsConnectorMode.PUSH || be.linkedPositions.isEmpty()) return;

        Direction facing = state.getValue(FluidConnectorBlock.FACING);
        BlockPos sourcePos = pos.relative(facing);
        BlockEntity sourceBe = level.getBlockEntity(sourcePos);
        if (sourceBe == null) return;

        var capOpt = sourceBe.getCapability(ForgeCapabilities.FLUID_HANDLER, facing.getOpposite());
        if (!capOpt.isPresent()) return;
        IFluidHandler source = capOpt.resolve().get();

        List<FluidConnectorBlockEntity> pulls = be.findPullConnectors(level);
        if (pulls.isEmpty()) { be.setIdle(); return; }

        FluidStack simDrained = tryDrainFiltered(source, maxMb, IFluidHandler.FluidAction.SIMULATE, be);
        if (simDrained.isEmpty()) { be.setIdle(); return; }

        int canFill = simulateDistribute(level, pulls, simDrained);
        if (canFill <= 0) { be.setIdle(); return; }

        FluidStack real = tryDrainFiltered(source, Math.min(canFill, maxMb), IFluidHandler.FluidAction.EXECUTE, be);
        if (real.isEmpty()) { be.setIdle(); return; }

        distribute(level, pulls, real);
        be.setActive();
    }

    private void setActive() { if (transferState != 1) { transferState = 1; sync(); } }
    private void setIdle()   { if (transferState != 0) { transferState = 0; sync(); } }

    private List<FluidConnectorBlockEntity> findPullConnectors(Level level) {
        List<FluidConnectorBlockEntity> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>(linkedPositions);
        linkedPositions.forEach(visited::add);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            if (!level.isLoaded(cur)) continue;
            BlockEntity be = level.getBlockEntity(cur);
            if (be instanceof FluidConnectorBlockEntity c) {
                if (c.mode == LogisticsConnectorMode.PULL) result.add(c);
            } else if (be instanceof LogisticsNodeBlockEntity node) {
                for (BlockPos lp : node.linkedPositions) {
                    if (visited.add(lp)) queue.add(lp);
                }
            }
        }
        return result;
    }

    private static FluidStack tryDrainFiltered(IFluidHandler h, int maxMb,
                                               IFluidHandler.FluidAction action,
                                               FluidConnectorBlockEntity be) {
        for (int i = 0; i < h.getTanks(); i++) {
            FluidStack f = h.getFluidInTank(i);
            if (f.isEmpty()) continue;
            if (!be.matchesFilter(f)) continue;
            FluidStack d = h.drain(new FluidStack(f.getFluid(), maxMb), action);
            if (!d.isEmpty()) return d;
        }
        return FluidStack.EMPTY;
    }

    private static int simulateDistribute(Level level, List<FluidConnectorBlockEntity> pulls, FluidStack fluid) {
        FluidStack rem = fluid.copy();
        for (FluidConnectorBlockEntity pull : pulls) {
            if (rem.isEmpty()) break;
            if (!pull.matchesFilter(rem)) continue;
            BlockPos dest = pull.getBlockPos().relative(pull.getBlockState().getValue(FluidConnectorBlock.FACING));
            BlockEntity destBe = level.getBlockEntity(dest);
            if (destBe == null) continue;
            var cap = destBe.getCapability(ForgeCapabilities.FLUID_HANDLER,
                    pull.getBlockState().getValue(FluidConnectorBlock.FACING).getOpposite());
            if (!cap.isPresent()) continue;
            int filled = cap.resolve().get().fill(rem, IFluidHandler.FluidAction.SIMULATE);
            if (filled > 0) rem = new FluidStack(rem.getFluid(), rem.getAmount() - filled);
        }
        return fluid.getAmount() - rem.getAmount();
    }

    private static void distribute(Level level, List<FluidConnectorBlockEntity> pulls, FluidStack fluid) {
        FluidStack rem = fluid.copy();
        for (FluidConnectorBlockEntity pull : pulls) {
            if (rem.isEmpty()) break;
            if (!pull.matchesFilter(rem)) continue;
            BlockPos dest = pull.getBlockPos().relative(pull.getBlockState().getValue(FluidConnectorBlock.FACING));
            BlockEntity destBe = level.getBlockEntity(dest);
            if (destBe == null) continue;
            var cap = destBe.getCapability(ForgeCapabilities.FLUID_HANDLER,
                    pull.getBlockState().getValue(FluidConnectorBlock.FACING).getOpposite());
            if (!cap.isPresent()) continue;
            int filled = cap.resolve().get().fill(rem, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) rem = new FluidStack(rem.getFluid(), rem.getAmount() - filled);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        long[] arr = linkedPositions.stream().mapToLong(BlockPos::asLong).toArray();
        tag.putLongArray("links", arr);
        tag.putByte("mode", (byte) mode.ordinal());
        tag.put("upgrade", upgradeHandler.serializeNBT());
        tag.putByte("state", transferState);
        tag.putBoolean("filterWhitelist", filterWhitelist);
        tag.putBoolean("filterMatchNbt",  filterMatchNbt);
        CompoundTag ft = new CompoundTag();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (!fluidFilters[i].isEmpty())
                ft.put("f" + i, fluidFilters[i].writeToNBT(new CompoundTag()));
        }
        tag.put("fluidFilters", ft);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedPositions.clear();
        for (long l : tag.getLongArray("links")) linkedPositions.add(BlockPos.of(l));
        mode = LogisticsConnectorMode.byId(tag.getByte("mode"));
        if (tag.contains("upgrade")) upgradeHandler.deserializeNBT(tag.getCompound("upgrade"));
        transferState = tag.getByte("state");
        filterWhitelist = !tag.contains("filterWhitelist") || tag.getBoolean("filterWhitelist");
        filterMatchNbt  = tag.getBoolean("filterMatchNbt");
        Arrays.fill(fluidFilters, FluidStack.EMPTY);
        CompoundTag ft = tag.getCompound("fluidFilters");
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (ft.contains("f" + i))
                fluidFilters[i] = FluidStack.loadFluidStackFromNBT(ft.getCompound("f" + i));
        }
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = new CompoundTag(); saveAdditional(t); return t; }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.fluid_connector"); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FluidConnectorMenu(id, inv, this, new net.minecraft.world.inventory.ContainerData() {
            @Override public int get(int i) {
                return switch (i) {
                    case 0 -> mode.ordinal();
                    case 1 -> filterWhitelist ? 0 : 1;
                    case 2 -> filterMatchNbt  ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) {
                switch (i) {
                    case 0 -> mode = LogisticsConnectorMode.byId(v);
                    case 1 -> filterWhitelist = (v == 0);
                    case 2 -> filterMatchNbt  = (v == 1);
                }
            }
            @Override public int getCount() { return 3; }
        });
    }
}
