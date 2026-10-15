package com.jykito.industrialcore.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.industrialcore.block.entity.MePortAccess;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.item.upgrade.PortUpgradeItem;
import com.jykito.industrialcore.menu.MePortMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared by the ME ports: an AE2 grid node (needs a channel), the node life cycle, status for the window and
 * exchange with network storage paid with network energy. The exchange itself is in {@link #meWork}, once every {@link #INTERVAL} ticks.
 * <p>
 * The mode is always "items + fluid", no auto-exchange with neighbours: the port works with the network. Tanks up to {@link #maxTanks()}:
 * as many are open as there are open slot rows (9 slots one tank, 18 two, 27 three), each one's volume from the upgrade.
 */
abstract class MePortBlockEntity extends ItemFluidPortBlockEntity implements IInWorldGridNodeHost, IActionHost, MePortAccess {

    protected static final int INTERVAL = 5;

    protected final IManagedGridNode node;
    private final IActionSource source = IActionSource.ofMachine(this);
    private int timer;
    /** On the client the status arrives with the block: there is no node there. */
    private boolean clientOnline;
    /** The second and third tanks; the first is the base port's tank. */
    private final FluidTank[] extra;
    /** Lists of open tanks by their count, so the core does not build a list every tick. */
    private final List<List<FluidTank>> views = new ArrayList<>();

    protected MePortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_PORT_BE.get(), pos, state);
        node = GridHelper.createManagedNode(this, Listener.INSTANCE)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(2)
                .setInWorldNode(true)
                .setVisualRepresentation(state.getBlock())
                .setTagName("me_node");
        initDefaults(MODE_BOTH, false);
        extra = new FluidTank[maxTanks() - 1];
        List<FluidTank> all = new ArrayList<>(List.of(getTank()));
        for (int i = 0; i < extra.length; i++) {
            extra[i] = new FluidTank(PortUpgradeItem.fluid(0)) {
                @Override protected void onContentsChanged() { setChanged(); sync(); }
            };
            all.add(extra[i]);
        }
        for (int n = 0; n <= all.size(); n++) views.add(List.copyOf(all.subList(0, n)));
    }

    protected abstract void meWork(IGrid grid);

    /** Open tanks: one per open slot row. */
    public int openTanks() { return Math.max(1, Math.min(maxTanks(), activeSlots() / 9)); }

    @Override public List<FluidTank> tanks() { return views.get(openTanks()); }

    @Override
    protected void tierChanged(int tier) {
        for (FluidTank t : extra) t.setCapacity(PortUpgradeItem.fluid(tier));
    }

    /** The upgrade can be removed or changed only if the tanks being closed are empty and the others fit the new volume. */
    @Override
    protected boolean extraFits(int t) {
        int open = Math.max(1, Math.min(maxTanks(), PortUpgradeItem.slots(t) / 9));
        for (int i = 0; i < extra.length; i++) {
            if (i + 1 >= open && !extra[i].isEmpty()) return false;
            if (extra[i].getFluidAmount() > PortUpgradeItem.fluid(t)) return false;
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        for (FluidTank t : extra) if (!t.isEmpty()) return false;
        return super.isEmpty();
    }

    /** An ME port's mode is fixed. */
    @Override public boolean toggleMode() { return false; }

    @Override
    protected void extraTick(Level level) {
        if (++timer < INTERVAL) return;
        timer = 0;
        IGrid grid = node.getGrid();
        if (grid != null && node.isActive()) meWork(grid);
    }

    /** Take from the network, paying with network energy; returns how much was actually taken. */
    protected long extract(IGrid grid, AEKey key, long amount) {
        if (amount <= 0) return 0;
        return StorageHelper.poweredExtraction(grid.getEnergyService(), grid.getStorageService().getInventory(), key, amount, source, Actionable.MODULATE);
    }

    /** Put into the network, paying with network energy; returns how much was accepted. */
    protected long insert(IGrid grid, AEKey key, long amount) {
        if (amount <= 0) return 0;
        return StorageHelper.poweredInsert(grid.getEnergyService(), grid.getStorageService().getInventory(), key, amount, source, Actionable.MODULATE);
    }

    // ── window ──

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MePortMenu(id, inv, this);
    }

    @Override
    public int meStatus() {
        boolean online = level != null && !level.isClientSide ? node.isActive() : clientOnline;
        return online ? 2 : 1;
    }

    // ── node ──

    @Override public IGridNode getGridNode(Direction dir) { return node.getNode(); }
    @Override public IGridNode getActionableNode() { return node.getNode(); }

    @Override
    public void onLoad() {
        super.onLoad();
        GridHelper.onFirstTick(this, be -> be.node.create(be.getLevel(), be.getBlockPos()));
    }

    @Override public void setRemoved() { super.setRemoved(); node.destroy(); }
    @Override public void onChunkUnloaded() { super.onChunkUnloaded(); node.destroy(); }

    // ── saving and sync: extra tanks and status ──

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        node.saveToNBT(tag);
        tag.put("meTanks", writeTanks());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        node.loadFromNBT(tag);
        clientOnline = tag.getBoolean("meOnline");
        initDefaults(MODE_BOTH, false);   // the mode is fixed, even if the save says otherwise
        ListTag list = tag.getList("meTanks", Tag.TAG_COMPOUND);
        for (int i = 0; i < extra.length; i++)
            extra[i].readFromNBT(i < list.size() ? list.getCompound(i) : new CompoundTag());
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = super.getUpdateTag();
        t.putBoolean("meOnline", node.isActive());
        t.put("meTanks", writeTanks());
        return t;
    }

    private ListTag writeTanks() {
        ListTag list = new ListTag();
        for (FluidTank t : extra) list.add(t.writeToNBT(new CompoundTag()));
        return list;
    }

    private static final class Listener implements IGridNodeListener<MePortBlockEntity> {
        static final Listener INSTANCE = new Listener();
        @Override public void onSaveChanges(MePortBlockEntity be, IGridNode n) { be.setChanged(); }
        @Override public void onStateChanged(MePortBlockEntity be, IGridNode n, IGridNodeListener.State state) { be.sync(); }
    }
}
