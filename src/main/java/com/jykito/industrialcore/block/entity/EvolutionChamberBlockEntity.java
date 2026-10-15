package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.block.custom.EvolutionChamberBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.heat.HeatCapability;
import com.jykito.industrialcore.heat.IHeatHandler;
import com.jykito.industrialcore.item.custom.NaniteStrainItem;
import com.jykito.industrialcore.menu.EvolutionChamberMenu;
import com.jykito.industrialcore.nanite.NaniteEvolution;
import com.jykito.industrialcore.nanite.NaniteFeed;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.industrialcore.nanite.NaniteTraits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Evolution chamber: the nanite strain is selected by the feed in the second slot ({@link NaniteFeed}): ore makes a miner,
 * dust a processor, a research sample a scientist. The class is fixed from the first generation, a strain of another class
 * is not evolved by the chamber. A generation is {@link #GENERATION_TICKS} ticks and eats one ore or dust (the sample is not consumed);
 * energy is drawn per tick. Heat from the heat network speeds generations up to {@link #HEAT_SPEEDUP} times
 * and makes selection steps larger, but lowers the strain stability ({@link NaniteEvolution}).
 * <p>
 * For the graph in the window the chamber remembers the affinity to the target ore over the last {@link #HISTORY} generations.
 * Numbers are preliminary, balance comes later.
 */
public class EvolutionChamberBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

    public static final int SLOT_STRAIN = 0, SLOT_FEED = 1;
    public static final int GENERATION_TICKS = 400;
    public static final float HEAT_SPEEDUP = 4f;
    public static final int ENERGY_PER_TICK = 1_000;
    public static final int MAX_HEAT = 10_000, HEAT_PER_TICK = 5, HEAT_LOSS = 2;
    public static final int HISTORY = 96;
    public static final int FIELD_RADIUS = 8;
    private static final int PROGRESS_SCALE = 100;

    /** Add new values only at the end: the ordinal is sent to the client. */
    public enum Status { WORKING, NO_STRAIN, NO_FEED, NO_ENERGY, WRONG_KIND }

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == SLOT_STRAIN ? stack.getItem() instanceof NaniteStrainItem : NaniteFeed.of(stack) != null;
        }
        @Override public int getSlotLimit(int slot) { return slot == SLOT_STRAIN ? 1 : 64; }
    };

    /** From outside things are only inserted: ore and strains. The strain is taken out by hand, otherwise a hopper would pull it mid-selection. */
    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return items.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return items.insertItem(slot, stack, simulate); }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return items.isItemValid(slot, stack); }
    };

    private final MachineEnergy energy = MachineEnergy.consumer(400_000, 16_384);

    private int heat;
    private boolean heated;
    private final IHeatHandler heatHandler = new IHeatHandler() {
        @Override public int getHeat() { return heat; }
        @Override public int getMaxHeat() { return MAX_HEAT; }
        @Override public int receiveHeat(int amount, boolean simulate) {
            int accepted = Math.min(Math.max(0, amount), MAX_HEAT - heat);
            if (!simulate && accepted > 0) { heat += accepted; heated = true; }
            return accepted;
        }
    };


    private int progress;
    private Status status = Status.NO_STRAIN;

    /** Affinity to the target ore per generation (thousandths) and which ore is the target. Synced to the client for the graph. */
    private final List<Integer> history = new ArrayList<>();
    private @Nullable String historyTarget;
    private @Nullable Item historyIcon;

    public EvolutionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EVOLUTION_CHAMBER_BE.get(), pos, state);
    }

    public ItemStackHandler getItems() { return items; }
    public List<Integer> getHistory() { return history; }
    public @Nullable Item getHistoryIcon() { return historyIcon; }

    private float heatFraction() { return heat / (float) MAX_HEAT; }

    // ───────── tick ─────────

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!heated && heat > 0) heat = Math.max(0, heat - HEAT_LOSS);
        heated = false;

        ItemStack strain = items.getStackInSlot(SLOT_STRAIN), feed = items.getStackInSlot(SLOT_FEED);
        NaniteFeed food = NaniteFeed.of(feed);
        if (strain.isEmpty() && !history.isEmpty()) { history.clear(); historyTarget = null; historyIcon = null; sync(); }
        NaniteGenome.Kind kind = strain.isEmpty() ? null : NaniteGenome.kindOf(strain);

        status = strain.isEmpty() ? Status.NO_STRAIN
                : food == null ? Status.NO_FEED
                : kind != null && kind != food.kind() ? Status.WRONG_KIND
                : energy.getEnergyStored() < ENERGY_PER_TICK ? Status.NO_ENERGY : Status.WORKING;
        boolean working = status == Status.WORKING;
        if (working) {
            float h = heatFraction();
            energy.consumeEnergy(ENERGY_PER_TICK);
            heat = Math.max(0, heat - HEAT_PER_TICK);
            progress += Math.round(PROGRESS_SCALE * (1 + (HEAT_SPEEDUP - 1) * h));
            if (progress >= GENERATION_TICKS * PROGRESS_SCALE) {
                progress = 0;
                generation(level, strain, feed, food, h);
            }
        }
        MachineLit.tick(this, level, pos, state, EvolutionChamberBlock.LIT, working);
        setChanged();
    }

    private void generation(Level level, ItemStack strain, ItemStack feed, NaniteFeed food, float heat) {
        NaniteGenome parent = NaniteGenome.read(strain);
        String target = food.target();
        if (!target.equals(historyTarget)) {
            history.clear();
            historyTarget = target;
            historyIcon = feed.getItem();
            history.add(permille(parent.bonus(target)));
        }
        NaniteGenome next = NaniteEvolution.step(parent, food.kind(), target, feed.getItem(), heat, level.random);
        NaniteTraits.update(next, food.kind(), target, new NaniteTraits.Env(cold(level), field(level), heat));
        ItemStack out = strain.copy();
        next.write(out);
        items.setStackInSlot(SLOT_STRAIN, out);
        if (food.consumed()) items.extractItem(SLOT_FEED, 1, false);
        history.add(permille(next.bonus(target)));
        while (history.size() > HISTORY) history.remove(0);
        sync();
    }

    private static int permille(float v) { return Math.round(v * 1000); }

    /** Cold for a cryophile: a cryo branch block right next to the chamber (cryo casing, cryo glass, cryo chamber core...). */
    private boolean cold(Level level) {
        for (Direction d : Direction.values()) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(worldPosition.relative(d)).getBlock());
            if (id != null && id.getNamespace().equals(com.jykito.industrialcore.IndustrialCore.MODID) && id.getPath().startsWith("cryo")) return true;
        }
        return false;
    }

    /** Field for a resonator: a magnet-accelerator core within {@link #FIELD_RADIUS} blocks. Once per generation, not every tick. */
    private boolean field(Level level) {
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-FIELD_RADIUS, -FIELD_RADIUS, -FIELD_RADIUS), worldPosition.offset(FIELD_RADIUS, FIELD_RADIUS, FIELD_RADIUS)))
            if (level.getBlockState(p).is(com.jykito.industrialcore.block.ModBlocks.ELECTROMAGNET_CORE.get())) return true;
        return false;
    }

    // ───────── window ─────────

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> energy.getEnergyStored() & 0xFFFF;
                case 1 -> (energy.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energy.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energy.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> (int) ((long) progress * 1000 / (GENERATION_TICKS * PROGRESS_SCALE));
                case 5 -> heat;
                case 6 -> status.ordinal();
                case 7 -> Math.round(GENERATION_TICKS / (1 + (HEAT_SPEEDUP - 1) * heatFraction()));
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 8; }
    };

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.evolution_chamber"); }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new EvolutionChamberMenu(id, inv, this, data);
    }

    // ───────── capabilities, sync, saving ─────────

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.EnergyStorage.BLOCK) return (T) energy;
        if (cap == Capabilities.ItemHandler.BLOCK) return (T) automation;
        if (cap == HeatCapability.HEAT) return (T) heatHandler;
        return null;
    }

        private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    private void writeHistory(CompoundTag tag) {
        tag.putIntArray("hist", history);
        if (historyTarget != null) tag.putString("target", historyTarget);
        if (historyIcon != null) tag.putString("icon", String.valueOf(BuiltInRegistries.ITEM.getKey(historyIcon)));
    }

    private void readHistory(CompoundTag tag) {
        history.clear();
        for (int v : tag.getIntArray("hist")) history.add(v);
        historyTarget = tag.contains("target") ? tag.getString("target") : null;
        ResourceLocation icon = tag.contains("icon") ? ResourceLocation.tryParse(tag.getString("icon")) : null;
        historyIcon = icon == null ? null : BuiltInRegistries.ITEM.get(icon);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("heat", heat);
        tag.putInt("progress", progress);
        writeHistory(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("inventory"));
        energy.setEnergy(tag.getInt("energy"));
        heat = Math.max(0, Math.min(MAX_HEAT, tag.getInt("heat")));
        progress = tag.getInt("progress");
        readHistory(tag);
    }

    /** Only the graph goes to the client (energy and the rest go through the window). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeHistory(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) { readHistory(tag); }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        if (pkt.getTag() != null) handleUpdateTag(pkt.getTag(), registries);
    }
}
