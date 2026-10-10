package com.jykito.industrialcore.block.entity;

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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Реструктор. T1 — одна линия, T2 — три линии на общем катализаторе, и каждая тратит вдвое меньше входа
 * (энергия на крафт та же).
 */
public class RestructorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int MAX_RECEIVE = 10_000_000;

    public static final int SLOT_COUNT  = 5;
    public static final int T2_LANES = 3;

    /** Катализатор и логистика — слоты T1. */
    public static final int SLOT_CATALYST = 2, SLOT_UPG_FIRST = 3, SLOT_UPG_LAST = 4;

    /** Вход линии: первая на слотах T1 (0 → 1), остальные после логистики (5 → 6, 7 → 8). Сейвы T1 не сдвигаются. */
    public static int inSlot(int lane)  { return lane == 0 ? 0 : SLOT_UPG_LAST - 1 + 2 * lane; }
    public static int outSlot(int lane) { return lane == 0 ? 1 : inSlot(lane) + 1; }
    private static boolean isInput(int slot)  { return slot == 0 || slot > SLOT_UPG_LAST && (slot - SLOT_UPG_LAST) % 2 == 1; }
    private static boolean isOutput(int slot) { return slot == 1 || slot > SLOT_UPG_LAST && (slot - SLOT_UPG_LAST) % 2 == 0; }

    private final int lanes;
    private final int slotCount;
    /** T2: вход вдвое дешевле. */
    private final boolean halfInput;

    // Времени у реструктора нет: предмет + накопленная стоимость рецепта = результат в тот же тик.
    private final int[] currentRecipeCost;
    private final int[] energyAccumulated;
    /** Тиков с последнего крафта (до секунды). Пока > 0 и накопление пустое, в GUI заряд показывается полным:
     *  при мощной подаче стоимость набирается и тратится за один тик, и честное накопление всегда 0. */
    private final int[] craftedRecently;
    /** T2: оплаченная половинка входа (0 или 1) и предмет, за который она оплачена. Половинка от другого
     *  предмета сгорает, иначе дешёвым блоком можно было бы оплачивать каждый второй дорогой. */
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

    /** Энергия идёт в линии по очереди: первая с предметом и недобранной стоимостью забирает своё, остаток дальше. */
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

    private LazyOptional<IItemHandler>   lazyItemHandler   = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    /** По 4 ячейки на линию: показываемый заряд и стоимость, каждое двумя половинами. У первой линии — как у T1. */
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
        this.slotCount = Math.max(SLOT_COUNT, outSlot(lanes - 1) + 1);   // у T1 все 5 слотов: вход, выход, катализатор, логистика
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

    /** Заряд для GUI: сразу после крафта при пустом накоплении — полный, см. {@link #craftedRecently}. */
    private int shownEnergy(int lane) {
        return craftedRecently[lane] > 0 && energyAccumulated[lane] == 0 ? currentRecipeCost[lane] : energyAccumulated[lane];
    }

    /** Оплаченная половинка линии, если она за этот же предмет. */
    private int credit(int lane, ItemStack input) {
        return halfCredit[lane] > 0 && input.is(creditItem[lane]) ? halfCredit[lane] : 0;
    }

    private Optional<RestructorRecipeRegistry.RestructorRecipeData> getRecipe(Level level, int lane) {
        return Optional.ofNullable(recipeCache[lane].get(() -> {
            ItemStack input = itemHandler.getStackInSlot(inSlot(lane));
            // T2 считает вход в половинках: рецепту на N предметов хватает N половинок, и проверка рецепта та же
            if (halfInput && !input.isEmpty()) input = input.copyWithCount(input.getCount() * 2 + credit(lane, input));

            SimpleContainer c = new SimpleContainer(2);
            c.setItem(0, input);
            c.setItem(1, itemHandler.getStackInSlot(SLOT_CATALYST));
            Optional<MachineRecipe> mcRecipe = level.getRecipeManager()
                    .getRecipeFor(ModRecipes.MOLECULAR_TRANSFORMING_TYPE.get(), c, level);
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

    /** Сколько предметов списать за крафт. T2 платит половину: рецепт на 1 предмет берёт его раз в два крафта, на 2 и 4 — 1 и 2. */
    private int inputToTake(int lane, ItemStack input, int count) {
        if (!halfInput) return count;
        int owed = count - credit(lane, input);                // в половинках предмета
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
                    if (output.isEmpty() || (ItemStack.isSameItemSameTags(output, result)
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
            // «работает» и в тик крафта: при мощной подаче вся стоимость набирается за тик и сразу
            // обнуляется крафтом, накопление к этой проверке уже 0 — без crafted машина не загоралась вовсе
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
        var cap = target.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
        if (!cap.isPresent()) return;
        IItemHandler dst = cap.resolve().get();
        for (int lane = 0; lane < lanes; lane++) {
            int out = outSlot(lane);
            ItemStack stack = itemHandler.getStackInSlot(out);
            if (stack.isEmpty()) continue;
            ItemStack leftover = ItemHandlerHelper.insertItem(dst, stack.copy(), false);
            if (leftover.getCount() < stack.getCount())
                itemHandler.setStackInSlot(out, leftover.isEmpty() ? ItemStack.EMPTY : leftover);
        }
    }

    /** Затягивает один стак за раз; у T2 — во вход, где этого предмета меньше всего, чтобы линии шли вровень. */
    private void doItemPull(BlockEntity target, Direction dir) {
        var cap = target.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
        if (!cap.isPresent()) return;
        IItemHandler src = cap.resolve().get();
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

    /** Вход с наименьшим числом этого предмета из тех, что его примут; -1, если некуда. */
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
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        if (cap == ForgeCapabilities.ENERGY)       return lazyEnergyHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler   = LazyOptional.of(() -> automationHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }

    /** Ключ линии в NBT: у первой как у T1. */
    private static String key(String base, int lane) { return lane == 0 ? base : base + lane; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        for (int i = 0; i < lanes; i++) {
            tag.putInt(key("energyAccumulated", i), energyAccumulated[i]);
            tag.putInt(key("recipeCost", i), currentRecipeCost[i]);
            if (halfCredit[i] > 0 && creditItem[i] != null) {
                tag.putInt(key("halfCredit", i), halfCredit[i]);
                tag.putString(key("creditItem", i), String.valueOf(ForgeRegistries.ITEMS.getKey(creditItem[i])));
            }
        }
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag inv = tag.getCompound("inventory");

        if (inv.contains("Size") && inv.getInt("Size") < slotCount) {
            inv.putInt("Size", slotCount);
        }
        itemHandler.deserializeNBT(inv);
        for (int i = 0; i < lanes; i++) {
            energyAccumulated[i] = tag.getInt(key("energyAccumulated", i));
            if (tag.contains(key("recipeCost", i))) currentRecipeCost[i] = tag.getInt(key("recipeCost", i));
            halfCredit[i] = tag.getInt(key("halfCredit", i));
            creditItem[i] = halfCredit[i] > 0
                    ? ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(tag.getString(key("creditItem", i)))) : null;
        }
    }

    public void drops(Level level, BlockPos pos) {
        for (int i = 0; i < slotCount; i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemHandler.getStackInSlot(i));
        }
    }
}
