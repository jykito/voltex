package com.jykito.industrialcore.block.entity;
import net.minecraft.core.HolderLookup;
import com.jykito.industrialcore.capability.Caps;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapability;

import com.jykito.industrialcore.api.RestructorRecipeRegistry;
import com.jykito.industrialcore.block.custom.RestructorBlock;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.custom.CatalystItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import com.jykito.industrialcore.menu.RestructorMenu;
import com.jykito.industrialcore.recipe.CachedRecipe;
import com.jykito.industrialcore.recipe.MachineRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Restructor. T1 is one line, T2 is three lines on a shared catalyst, each spending half the input
 * (energy per craft is the same).
 */
public class RestructorBlockEntity extends BlockEntity implements Caps.Provider, MenuProvider {

    public static final int MAX_RECEIVE = 10_000_000;

    public static final int SLOT_COUNT  = 5;
    public static final int T2_LANES = 3;

    /** Catalyst and logistics: the T1 slots. */
    public static final int SLOT_CATALYST = 2, SLOT_UPG_FIRST = 3, SLOT_UPG_LAST = 4;

    /** Line input: the first uses the T1 slots (0 -> 1), the others come after logistics (5 -> 6, 7 -> 8). T1 saves do not shift. */
    public static int inSlot(int lane)  { return lane == 0 ? 0 : SLOT_UPG_LAST - 1 + 2 * lane; }
    public static int outSlot(int lane) { return lane == 0 ? 1 : inSlot(lane) + 1; }
    private static boolean isInput(int slot)  { return slot == 0 || slot > SLOT_UPG_LAST && (slot - SLOT_UPG_LAST) % 2 == 1; }
    private static boolean isOutput(int slot) { return slot == 1 || slot > SLOT_UPG_LAST && (slot - SLOT_UPG_LAST) % 2 == 0; }

    private final int lanes;
    private final int slotCount;
    /** T2: input costs half. */
    private final boolean halfInput;

    // The restructor has no time: item + accumulated recipe cost = result in the same tick.
    private final int[] currentRecipeCost;
    private final int[] energyAccumulated;
    /** Ticks since the last craft (up to a second). While > 0 and the accumulation is empty, the GUI shows the charge as full:
     *  with a strong supply the cost is gathered and spent within one tick, and the honest accumulation is always 0. */
    private final int[] craftedRecently;
    /** T2: paid half of the input (0 or 1) and the item it was paid for. A half paid for another
     *  item burns, otherwise a cheap block could pay for every second expensive one. */
    private final int[] halfCredit;
    private final Item[] creditItem;

    private final CachedRecipe<RestructorRecipeRegistry.RestructorRecipeData>[] recipeCache;

    private final ItemStackHandler itemHandler;

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return slotCount; }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!isInput(slot)) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!isOutput(slot)) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    /** Energy goes to the lines in turn: the first one with an item and unpaid cost takes its share, the rest goes on. */
    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int taken = 0;
            for (int i = 0; i < lanes && taken < maxReceive; i++) {
                if (!accepts(i)) continue;
                int toReceive = Math.min(maxReceive - taken, currentRecipeCost[i] - energyAccumulated[i]);
                if (!simulate) energyAccumulated[i] += toReceive;
                taken += toReceive;
            }
            if (taken > 0 && !simulate) setChanged();
            return taken;
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { int s = 0; for (int e : energyAccumulated) s += e; return s; }
        @Override public int getMaxEnergyStored() { long s = 0; for (int c : currentRecipeCost) s += c; return (int) Math.min(Integer.MAX_VALUE, s); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() {
            for (int i = 0; i < lanes; i++) if (accepts(i)) return true;
            return false;
        }
    };

    private boolean accepts(int lane) {
        return energyAccumulated[lane] < currentRecipeCost[lane] && !itemHandler.getStackInSlot(inSlot(lane)).isEmpty();
    }


    /** 4 cells per line: displayed charge and cost, each in two halves. The first line matches T1. */
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int lane = index / 4;
            if (lane >= lanes) return 0;
            return switch (index % 4) {
                case 0 -> shownEnergy(lane) & 0xFFFF;
                case 1 -> (shownEnergy(lane) >> 16) & 0xFFFF;
                case 2 -> currentRecipeCost[lane] & 0xFFFF;
                default -> (currentRecipeCost[lane] >> 16) & 0xFFFF;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 4 * lanes; }
    };

    public RestructorBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.RESTRUCTOR_BE.get(), pos, state, 1, false);
    }

    public static RestructorBlockEntity t2(BlockPos pos, BlockState state) {
        return new RestructorBlockEntity(ModBlockEntities.RESTRUCTOR_T2_BE.get(), pos, state, T2_LANES, true);
    }

    @SuppressWarnings("unchecked")
    protected RestructorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int lanes, boolean halfInput) {
        super(type, pos, state);
        this.lanes = lanes;
        this.halfInput = halfInput;
        this.slotCount = Math.max(SLOT_COUNT, outSlot(lanes - 1) + 1);   // T1 has all 5 slots: input, output, catalyst, logistics
        this.currentRecipeCost = new int[lanes];
        java.util.Arrays.fill(currentRecipeCost, 10_000_000);
        this.energyAccumulated = new int[lanes];
        this.craftedRecently = new int[lanes];
        this.halfCredit = new int[lanes];
        this.creditItem = new Item[lanes];
        this.recipeCache = new CachedRecipe[lanes];
        for (int i = 0; i < lanes; i++) recipeCache[i] = new CachedRecipe<>();
        this.itemHandler = new ItemStackHandler(slotCount) {
            @Override protected void onContentsChanged(int slot) {
                for (CachedRecipe<?> c : recipeCache) c.markDirty();
                setChanged();
            }
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (slot == SLOT_CATALYST) return stack.getItem() instanceof CatalystItem;
                if (slot >= SLOT_UPG_FIRST && slot <= SLOT_UPG_LAST)
                    return stack.getItem() == ModItems.ITEM_PUSHER.get() || stack.getItem() == ModItems.ITEM_PULLER.get();
                if (isInput(slot)) return !(stack.getItem() instanceof com.jykito.industrialcore.item.upgrade.MachineUpgradeItem);
                return false;
            }
        };
    }

    public int getLanes() { return lanes; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return lanes == 1 ? new RestructorMenu(id, inventory, this, this.data) : RestructorMenu.t2(id, inventory, this, this.data);
    }

    /** Charge for the GUI: right after a craft with an empty accumulation it is full, see {@link #craftedRecently}. */
    private int shownEnergy(int lane) {
        return craftedRecently[lane] > 0 && energyAccumulated[lane] == 0 ? currentRecipeCost[lane] : energyAccumulated[lane];
    }

    /** The line's paid half, if it was paid for this same item. */
    private int credit(int lane, ItemStack input) {
        return halfCredit[lane] > 0 && input.is(creditItem[lane]) ? halfCredit[lane] : 0;
    }

    private Optional<RestructorRecipeRegistry.RestructorRecipeData> getRecipe(Level level, int lane) {
        return Optional.ofNullable(recipeCache[lane].get(() -> {
            ItemStack input = itemHandler.getStackInSlot(inSlot(lane));
            // T2 counts input in halves: a recipe for N items needs N halves, and the recipe check is the same
            if (halfInput && !input.isEmpty()) input = input.copyWithCount(input.getCount() * 2 + credit(lane, input));

            SimpleContainer c = new SimpleContainer(2);
            c.setItem(0, input);
            c.setItem(1, itemHandler.getStackInSlot(SLOT_CATALYST));
            Optional<MachineRecipe> mcRecipe = com.jykito.industrialcore.recipe.RecipeLookup.find(level.getRecipeManager(), ModRecipes.MOLECULAR_TRANSFORMING_TYPE.get(), c, level);
            if (mcRecipe.isPresent()) {
                MachineRecipe r = mcRecipe.get();
                return new RestructorRecipeRegistry.RestructorRecipeData(
                        r.getInputCount(),
                        r.getResultItem(level.registryAccess()),
                        r.getEnergyCost()
                );
            }

            return RestructorRecipeRegistry.findMatchData(input, itemHandler.getStackInSlot(SLOT_CATALYST)).orElse(null);
        }));
    }

    /** How many items to consume per craft. T2 pays half: a 1-item recipe takes it every second craft, 2 and 4 items take 1 and 2. */
    private int inputToTake(int lane, ItemStack input, int count) {
        if (!halfInput) return count;
        int owed = count - credit(lane, input);                // in halves of an item
        int take = (owed + 1) / 2;
        halfCredit[lane] = take * 2 - owed;
        creditItem[lane] = input.getItem();
        return take;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        boolean changed = false, working = false;

        for (int lane = 0; lane < lanes; lane++) {
            Optional<RestructorRecipeRegistry.RestructorRecipeData> recipe = getRecipe(level, lane);
            boolean crafted = false;
            if (recipe.isPresent()) {
                currentRecipeCost[lane] = recipe.get().energyCost();
                if (energyAccumulated[lane] >= currentRecipeCost[lane]) {
                    ItemStack result = recipe.get().result();
                    int out = outSlot(lane);
                    ItemStack output = itemHandler.getStackInSlot(out);
                    if (output.isEmpty() || (ItemStack.isSameItemSameComponents(output, result)
                            && output.getCount() + result.getCount() <= output.getMaxStackSize())) {
                        int in = inSlot(lane);
                        itemHandler.extractItem(in, inputToTake(lane, itemHandler.getStackInSlot(in), recipe.get().inputCount()), false);
                        if (output.isEmpty()) {
                            itemHandler.setStackInSlot(out, result.copy());
                        } else {
                            output.grow(result.getCount());
                        }
                        energyAccumulated[lane] = 0;
                        craftedRecently[lane] = MachineLit.GRACE;
                        crafted = changed = true;
                    }
                }
            }
            // "working" in the craft tick too: with a strong supply the whole cost is gathered within a tick and immediately
            // reset by the craft, so the accumulation is already 0 at this check; without crafted the machine never lit up
            working |= recipe.isPresent() && (energyAccumulated[lane] > 0 || crafted);
            if (!crafted && craftedRecently[lane] > 0) craftedRecently[lane]--;
        }

        processUpgrades(level, pos);

        MachineLit.tick(this, level, pos, state, RestructorBlock.LIT, working);

        if (changed) setChanged();
    }

    private void processUpgrades(Level level, BlockPos pos) {
        for (int i = SLOT_UPG_FIRST; i <= SLOT_UPG_LAST; i++) {
            ItemStack upgrade = itemHandler.getStackInSlot(i);
            if (!(upgrade.getItem() instanceof DirectionalUpgradeItem du)) continue;
            Direction dir = DirectionalUpgradeItem.getStoredDirection(upgrade);
            if (dir == null) continue;
            BlockEntity target = level.getBlockEntity(pos.relative(dir));
            if (target == null) continue;
            switch (du.getMode()) {
                case ITEM_PUSH -> doItemPush(target, dir);
                case ITEM_PULL -> doItemPull(target, dir);
                default -> {}
            }
        }
    }

    private void doItemPush(BlockEntity target, Direction dir) {
        var cap = Caps.get(target, Caps.ITEM_HANDLER, dir.getOpposite());
        if (!cap.isPresent()) return;
        IItemHandler dst = cap.get();
        for (int lane = 0; lane < lanes; lane++) {
            int out = outSlot(lane);
            ItemStack stack = itemHandler.getStackInSlot(out);
            if (stack.isEmpty()) continue;
            ItemStack leftover = ItemHandlerHelper.insertItem(dst, stack.copy(), false);
            if (leftover.getCount() < stack.getCount())
                itemHandler.setStackInSlot(out, leftover.isEmpty() ? ItemStack.EMPTY : leftover);
        }
    }

    /** Pulls one stack at a time; on T2 into the input that has the least of this item, so the lines stay even. */
    private void doItemPull(BlockEntity target, Direction dir) {
        var cap = Caps.get(target, Caps.ITEM_HANDLER, dir.getOpposite());
        if (!cap.isPresent()) return;
        IItemHandler src = cap.get();
        for (int si = 0; si < src.getSlots(); si++) {
            ItemStack avail = src.extractItem(si, 64, true);
            if (avail.isEmpty()) continue;
            int in = pullTarget(avail);
            if (in < 0) continue;
            ItemStack rem = automationHandler.insertItem(in, avail, true);
            int canTake = avail.getCount() - rem.getCount();
            if (canTake > 0) {
                ItemStack taken = src.extractItem(si, canTake, false);
                automationHandler.insertItem(in, taken, false);
                break;
            }
        }
    }

    /** The input with the fewest of this item among those that accept it; -1 if there is nowhere. */
    private int pullTarget(ItemStack stack) {
        int best = -1, bestCount = Integer.MAX_VALUE;
        for (int lane = 0; lane < lanes; lane++) {
            int in = inSlot(lane);
            if (automationHandler.insertItem(in, stack.copyWithCount(1), true).getCount() > 0) continue;
            int n = itemHandler.getStackInSlot(in).getCount();
            if (n < bestCount) { best = in; bestCount = n; }
        }
        return best;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCap(BlockCapability<T, Direction> cap, @Nullable Direction side) {
        if (cap == Capabilities.ItemHandler.BLOCK) return (T) automationHandler;
        if (cap == Capabilities.EnergyStorage.BLOCK)       return (T) energyStorage;
        return null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

        /** Line key in NBT: the first one matches T1. */
    private static String key(String base, int lane) { return lane == 0 ? base : base + lane; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        for (int i = 0; i < lanes; i++) {
            tag.putInt(key("energyAccumulated", i), energyAccumulated[i]);
            tag.putInt(key("recipeCost", i), currentRecipeCost[i]);
            if (halfCredit[i] > 0 && creditItem[i] != null) {
                tag.putInt(key("halfCredit", i), halfCredit[i]);
                tag.putString(key("creditItem", i), String.valueOf(BuiltInRegistries.ITEM.getKey(creditItem[i])));
            }
        }
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag inv = tag.getCompound("inventory");

        if (inv.contains("Size") && inv.getInt("Size") < slotCount) {
            inv.putInt("Size", slotCount);
        }
        itemHandler.deserializeNBT(registries, inv);
        for (int i = 0; i < lanes; i++) {
            energyAccumulated[i] = tag.getInt(key("energyAccumulated", i));
            if (tag.contains(key("recipeCost", i))) currentRecipeCost[i] = tag.getInt(key("recipeCost", i));
            halfCredit[i] = tag.getInt(key("halfCredit", i));
            creditItem[i] = halfCredit[i] > 0
                    ? BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(tag.getString(key("creditItem", i)))) : null;
        }
    }

    public void drops(Level level, BlockPos pos) {
        for (int i = 0; i < slotCount; i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemHandler.getStackInSlot(i));
        }
    }
}
