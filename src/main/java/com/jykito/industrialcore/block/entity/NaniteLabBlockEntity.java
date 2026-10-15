package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.NaniteLabBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.custom.NaniteStrainItem;
import com.jykito.industrialcore.menu.NaniteLabMenu;
import com.jykito.industrialcore.nanite.NaniteFeed;
import com.jykito.industrialcore.nanite.NaniteGenome;
import com.jykito.industrialcore.nanite.NaniteLab;
import com.jykito.industrialcore.nanite.NaniteTrait;
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
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Nanite lab: a scientist nanite studies a topic sample and writes blank data cards. It only takes on a topic
 * once its knowledge reaches the topic threshold; above the threshold knowledge speeds up writing ({@link NaniteLab}); the sample is not consumed,
 * a blank card and energy are spent. The scientist wears out like a miner: after each card with chance {@code (1 − stability) × }{@link #WEAR_CHANCE}.
 * Numbers are preliminary, balance comes later.
 */
public class NaniteLabBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_STRAIN = 0, SLOT_SAMPLE = 1, SLOT_BLANK = 2, SLOT_OUTPUT = 3;
    public static final int ENERGY_PER_TICK = 2_000;
    public static final float WEAR_CHANCE = 0.05f, WEAR = 0.97f;

    /** Add new values only at the end: the ordinal is sent to the client. */
    public enum Status { WORKING, NO_STRAIN, NO_SAMPLE, LOW_KNOWLEDGE, NO_BLANK, OUTPUT_FULL, NO_ENERGY }

    private final ItemStackHandler items = new ItemStackHandler(4) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_STRAIN -> stack.getItem() instanceof NaniteStrainItem && NaniteGenome.kindOf(stack) == NaniteGenome.Kind.SCIENTIST;
                case SLOT_SAMPLE -> NaniteFeed.theme(stack.getItem()) != null;
                case SLOT_BLANK -> stack.is(ModItems.DATA_CARD_BLANK.get());
                default -> false;
            };
        }
        @Override public int getSlotLimit(int slot) { return slot == SLOT_STRAIN ? 1 : 64; }
    };

    /** From outside a sample and blank cards are inserted, only written cards are extracted. */
    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return items.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot == SLOT_SAMPLE || slot == SLOT_BLANK ? items.insertItem(slot, stack, simulate) : stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUTPUT ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return items.isItemValid(slot, stack); }
    };

    private final MachineEnergy energy = MachineEnergy.consumer(1_000_000, 32_768);
    private final LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> energy);
    private final LazyOptional<IItemHandler> lazyItems = LazyOptional.of(() -> automation);

    private int progress, maxProgress = NaniteLab.SLOW_TICKS;
    /** Sample topic threshold in thousandths, for the display. */
    private int threshold;
    private Status status = Status.NO_STRAIN;

    public NaniteLabBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NANITE_LAB_BE.get(), pos, state);
    }

    public ItemStackHandler getItems() { return items; }

    public void tick(Level level, BlockPos pos, BlockState state) {
        ItemStack strain = items.getStackInSlot(SLOT_STRAIN), sample = items.getStackInSlot(SLOT_SAMPLE);
        String theme = sample.isEmpty() ? null : NaniteFeed.theme(sample.getItem());
        NaniteGenome genome = strain.isEmpty() ? null : NaniteGenome.read(strain);
        float knowledge = genome == null || theme == null ? 0f : genome.bonus(theme);
        Item out = theme == null ? null : NaniteLab.card(theme);
        threshold = theme == null ? 0 : Math.round(NaniteLab.threshold(theme) * 1000);

        status = strain.isEmpty() ? Status.NO_STRAIN
                : theme == null ? Status.NO_SAMPLE
                : out == null || knowledge < NaniteLab.threshold(theme) ? Status.LOW_KNOWLEDGE
                : items.getStackInSlot(SLOT_BLANK).isEmpty() ? Status.NO_BLANK
                : !fits(out) ? Status.OUTPUT_FULL
                : energy.getEnergyStored() < ENERGY_PER_TICK ? Status.NO_ENERGY : Status.WORKING;
        boolean working = status == Status.WORKING;
        if (working) {
            maxProgress = NaniteLab.ticks(genome.effect(theme));   // admission by pure knowledge, speed with traits
            energy.consumeEnergy(ENERGY_PER_TICK);
            if (++progress >= maxProgress) {
                progress = 0;
                items.extractItem(SLOT_BLANK, 1, false);
                ItemStack done = items.getStackInSlot(SLOT_OUTPUT);   // the output is closed for insertion from outside, we put into it ourselves
                int made = genome.has(NaniteTrait.RESONATOR) && level.random.nextFloat() < NaniteTrait.RESONATOR_CHANCE
                        && (done.isEmpty() || done.getCount() + 2 <= done.getMaxStackSize()) ? 2 : 1;   // resonance: two cards for one
                items.setStackInSlot(SLOT_OUTPUT, done.isEmpty() ? new ItemStack(out, made) : done.copyWithCount(done.getCount() + made));
                wear(level, strain);
            }
        } else if (status != Status.NO_ENERGY) {
            progress = 0;   // sample or strain changed, or the output filled up: writing the card starts over
        }
        MachineLit.tick(this, level, pos, state, NaniteLabBlock.LIT, working);
        setChanged();
    }

    /** The output accepts the card: the slot is empty or holds the same card with room. A blank card is only spent when written. */
    private boolean fits(Item out) {
        ItemStack s = items.getStackInSlot(SLOT_OUTPUT);
        return s.isEmpty() || (s.is(out) && s.getCount() < s.getMaxStackSize());
    }

    private void wear(Level level, ItemStack strain) {
        NaniteGenome g = NaniteGenome.read(strain);
        if (!g.wears() || level.random.nextFloat() >= (1f - g.stability()) * WEAR_CHANCE) return;
        g.decay(WEAR);
        ItemStack worn = strain.copy();
        g.write(worn);
        items.setStackInSlot(SLOT_STRAIN, worn);
    }

    // ───────── window ─────────

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> energy.getEnergyStored() & 0xFFFF;
                case 1 -> (energy.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energy.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energy.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> progress;
                case 5 -> maxProgress;
                case 6 -> status.ordinal();
                case 7 -> threshold;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 8; }
    };

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.nanite_lab"); }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new NaniteLabMenu(id, inv, this, data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItems.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate(); lazyItems.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", items.serializeNBT());
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("inventory"));
        energy.setEnergy(tag.getInt("energy"));
        progress = tag.getInt("progress");
    }
}
