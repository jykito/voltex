package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.logistics.ItemConnectorBlock;
import com.jykito.industrialcore.block.custom.logistics.LogisticsConnectorMode;
import com.jykito.industrialcore.item.upgrade.ConnectorUpgradeItem;
import com.jykito.industrialcore.menu.ItemConnectorMenu;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ItemConnectorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int FILTER_SLOTS = 15;

    public final Set<BlockPos> linkedPositions = new HashSet<>();
    public LogisticsConnectorMode mode = LogisticsConnectorMode.PUSH;

    public final ItemStackHandler filterHandler = new ItemStackHandler(FILTER_SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };

    public boolean filterWhitelist  = true;
    public boolean filterMatchNbt   = false;
    public boolean filterMatchDamage = false;

    public byte transferState = 0;
    public long itemsTransferred = 0;

    public final ItemStackHandler upgradeHandler = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof ConnectorUpgradeItem;
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };

    private int tickCounter = 0;
    private static final int INTERVAL    = 20;
    private static final int MAX_TRANSFER = 8;

    public ItemConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_CONNECTOR_BE.get(), pos, state);
    }

    public void addLink(BlockPos other)    { linkedPositions.add(other);    setChanged(); sync(); }
    public void removeLink(BlockPos other) { linkedPositions.remove(other); setChanged(); sync(); }

    public void clearAllLinks(Level level) {
        for (BlockPos linked : new HashSet<>(linkedPositions)) {
            BlockEntity be = level.getBlockEntity(linked);
            if (be instanceof ItemConnectorBlockEntity c)  c.removeLink(worldPosition);
            else if (be instanceof LogisticsNodeBlockEntity n) n.removeLink(worldPosition);
        }
        linkedPositions.clear();
        setChanged(); sync();
    }

    public void cycleMode() {
        mode = mode.next();
        if (level != null && !level.isClientSide())
            level.setBlock(worldPosition,
                    getBlockState().setValue(ItemConnectorBlock.PULL, mode == LogisticsConnectorMode.PULL), 3);
        setChanged(); sync();
    }

    public void sync() {
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public boolean matchesFilter(ItemStack stack) {
        boolean hasFilter = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack f = filterHandler.getStackInSlot(i);
            if (f.isEmpty()) continue;
            hasFilter = true;
            if (itemMatches(stack, f)) return filterWhitelist;
        }
        if (!hasFilter) return true;
        return !filterWhitelist;
    }

    private boolean itemMatches(ItemStack stack, ItemStack filter) {
        if (stack.getItem() != filter.getItem()) return false;
        if (filterMatchNbt && !ItemStack.isSameItemSameTags(stack, filter)) return false;
        if (filterMatchDamage && stack.getDamageValue() != filter.getDamageValue()) return false;
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemConnectorBlockEntity be) {

        ItemStack upStack = be.upgradeHandler.getStackInSlot(0);
        int interval    = INTERVAL;
        int maxTransfer = MAX_TRANSFER;
        if (!upStack.isEmpty() && upStack.getItem() instanceof ConnectorUpgradeItem u) {
            interval    = u.getInterval();
            maxTransfer = u.getMaxItems();
        }

        be.tickCounter++;
        if (be.tickCounter < interval) return;
        be.tickCounter = 0;
        if (be.mode != LogisticsConnectorMode.PUSH || be.linkedPositions.isEmpty()) return;

        Direction facing = state.getValue(ItemConnectorBlock.FACING);
        BlockPos sourcePos = pos.relative(facing);
        BlockEntity sourceBe = level.getBlockEntity(sourcePos);
        if (sourceBe == null) return;

        var capOpt = sourceBe.getCapability(ForgeCapabilities.ITEM_HANDLER, facing.getOpposite());
        if (!capOpt.isPresent()) return;
        IItemHandler source = capOpt.resolve().get();

        List<ItemConnectorBlockEntity> pulls = be.findPullConnectors(level);
        if (pulls.isEmpty()) { be.setIdle(); return; }

        for (int si = 0; si < source.getSlots(); si++) {
            ItemStack peek = source.getStackInSlot(si);
            if (peek.isEmpty() || !be.matchesFilter(peek)) continue;

            ItemStack simExtracted = source.extractItem(si, maxTransfer, true);
            if (simExtracted.isEmpty()) continue;

            int canInsert = simulateDistribute(level, pulls, simExtracted);
            if (canInsert <= 0) continue;

            ItemStack real = source.extractItem(si, Math.min(canInsert, maxTransfer), false);
            if (real.isEmpty()) continue;

            distribute(level, pulls, real);
            be.itemsTransferred += real.getCount();
            be.setActive();
            return;
        }
        be.setIdle();
    }

    private void setActive() { if (transferState != 1) { transferState = 1; sync(); } }
    private void setIdle()   { if (transferState != 0) { transferState = 0; sync(); } }

    private List<ItemConnectorBlockEntity> findPullConnectors(Level level) {
        List<ItemConnectorBlockEntity> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>(linkedPositions);
        linkedPositions.forEach(visited::add);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            if (!level.isLoaded(cur)) continue;
            BlockEntity be = level.getBlockEntity(cur);
            if (be instanceof ItemConnectorBlockEntity c) {
                if (c.mode == LogisticsConnectorMode.PULL) result.add(c);
            } else if (be instanceof LogisticsNodeBlockEntity node) {
                for (BlockPos lp : node.linkedPositions) {
                    if (visited.add(lp)) queue.add(lp);
                }
            }
        }
        return result;
    }

    private static int simulateDistribute(Level level, List<ItemConnectorBlockEntity> pulls, ItemStack stack) {
        ItemStack rem = stack.copy();
        for (ItemConnectorBlockEntity pull : pulls) {
            if (rem.isEmpty()) break;
            if (!pull.matchesFilter(rem)) continue;
            BlockPos dest = pull.getBlockPos().relative(pull.getBlockState().getValue(ItemConnectorBlock.FACING));
            BlockEntity destBe = level.getBlockEntity(dest);
            if (destBe == null) continue;
            var cap = destBe.getCapability(ForgeCapabilities.ITEM_HANDLER,
                    pull.getBlockState().getValue(ItemConnectorBlock.FACING).getOpposite());
            if (!cap.isPresent()) continue;
            rem = insertAll(cap.resolve().get(), rem, true);
        }
        return stack.getCount() - rem.getCount();
    }

    private static void distribute(Level level, List<ItemConnectorBlockEntity> pulls, ItemStack stack) {
        ItemStack rem = stack.copy();
        for (ItemConnectorBlockEntity pull : pulls) {
            if (rem.isEmpty()) break;
            if (!pull.matchesFilter(rem)) continue;
            BlockPos dest = pull.getBlockPos().relative(pull.getBlockState().getValue(ItemConnectorBlock.FACING));
            BlockEntity destBe = level.getBlockEntity(dest);
            if (destBe == null) continue;
            var cap = destBe.getCapability(ForgeCapabilities.ITEM_HANDLER,
                    pull.getBlockState().getValue(ItemConnectorBlock.FACING).getOpposite());
            if (!cap.isPresent()) continue;
            rem = insertAll(cap.resolve().get(), rem, false);
        }
    }

    private static ItemStack insertAll(IItemHandler h, ItemStack stack, boolean sim) {
        ItemStack rem = stack.copy();
        for (int i = 0; i < h.getSlots(); i++) {
            rem = h.insertItem(i, rem, sim);
            if (rem.isEmpty()) return ItemStack.EMPTY;
        }
        return rem;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        long[] arr = linkedPositions.stream().mapToLong(BlockPos::asLong).toArray();
        tag.putLongArray("links", arr);
        tag.putByte("mode", (byte) mode.ordinal());
        tag.put("filter", filterHandler.serializeNBT());
        tag.put("upgrade", upgradeHandler.serializeNBT());
        tag.putByte("state", transferState);
        tag.putBoolean("filterWhitelist",   filterWhitelist);
        tag.putBoolean("filterMatchNbt",    filterMatchNbt);
        tag.putBoolean("filterMatchDamage", filterMatchDamage);
        tag.putLong("itemsTransferred", itemsTransferred);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedPositions.clear();
        for (long l : tag.getLongArray("links")) linkedPositions.add(BlockPos.of(l));
        mode = LogisticsConnectorMode.byId(tag.getByte("mode"));
        filterHandler.deserializeNBT(tag.getCompound("filter"));
        if (tag.contains("upgrade")) upgradeHandler.deserializeNBT(tag.getCompound("upgrade"));
        transferState = tag.getByte("state");
        filterWhitelist   = !tag.contains("filterWhitelist") || tag.getBoolean("filterWhitelist");
        filterMatchNbt    = tag.getBoolean("filterMatchNbt");
        filterMatchDamage = tag.getBoolean("filterMatchDamage");
        itemsTransferred  = tag.getLong("itemsTransferred");
    }

    @Override public CompoundTag getUpdateTag() { CompoundTag t = new CompoundTag(); saveAdditional(t); return t; }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.item_connector"); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ItemConnectorMenu(id, inv, this, new net.minecraft.world.inventory.ContainerData() {
            @Override public int get(int i) {
                return switch (i) {
                    case 0 -> mode.ordinal();
                    case 1 -> filterWhitelist   ? 0 : 1;
                    case 2 -> filterMatchNbt    ? 1 : 0;
                    case 3 -> filterMatchDamage ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) {
                switch (i) {
                    case 0 -> mode = LogisticsConnectorMode.byId(v);
                    case 1 -> filterWhitelist   = (v == 0);
                    case 2 -> filterMatchNbt    = (v == 1);
                    case 3 -> filterMatchDamage = (v == 1);
                }
            }
            @Override public int getCount() { return 4; }
        });
    }
}
