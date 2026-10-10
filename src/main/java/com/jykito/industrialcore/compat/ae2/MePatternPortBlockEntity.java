package com.jykito.industrialcore.compat.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternContainer;
import com.jykito.industrialcore.item.upgrade.PortUpgradeItem;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Порт шаблонов: поставщик шаблонов AE2, встроенный в мультиблок. Держит шаблоны обработки (сколько слотов —
 * задаёт улучшение порта); по заказу автокрафта кладёт ингредиенты прямо в свой буфер, а ядро берёт их как из
 * обычного входа. Результат возвращается в сеть через МЭ-выход той же структуры.
 * <p>
 * Ингредиенты кладутся целиком или никак: если весь шаблон не влезает, заказ откладывается. Жидкостей в шаблоне
 * может быть столько, сколько открыто баков. Блокировка: новый заказ не принимается, пока в буфере что-то лежит.
 * Порт виден в терминале доступа к шаблонам (можно скрыть кнопкой), там он подписан именем своей машины.
 */
class MePatternPortBlockEntity extends MePortBlockEntity implements ICraftingProvider, PatternContainer {

    private static final int TANKS = 3;

    private final ItemStackHandler patterns = new ItemStackHandler(PortUpgradeItem.MAX_SLOTS) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot < activeSlots() && PatternDetailsHelper.isEncodedPattern(stack);
        }
        @Override protected void onContentsChanged(int slot) { patternsChanged(); }
    };
    /** Те же слоты для терминала доступа к шаблонам: кладёт он через isItemValid, закрытые слоты не займёт. */
    private final InternalInventory terminalInv = new InternalInventory() {
        @Override public int size() { return patterns.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return patterns.getStackInSlot(slot); }
        @Override public void setItemDirect(int slot, ItemStack stack) { patterns.setStackInSlot(slot, stack); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return patterns.isItemValid(slot, stack); }
    };
    private List<IPatternDetails> cache = List.of();
    private boolean dirty = true, blocking, showInTerminal = true;
    private int priority;
    private int lastSlots = -1;

    MePatternPortBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
        node.addService(ICraftingProvider.class, this);
    }

    private void patternsChanged() {
        dirty = true;
        setChanged();
        ICraftingProvider.requestUpdate(node);
    }

    @Override public int maxTanks() { return TANKS; }
    @Override public ItemStackHandler mePatterns() { return patterns; }
    @Override public boolean meBlocking() { return blocking; }
    @Override public void toggleMeBlocking() { blocking = !blocking; setChanged(); }
    @Override public boolean meShowInTerminal() { return showInTerminal; }
    @Override public void toggleMeShowInTerminal() { showInTerminal = !showInTerminal; setChanged(); }
    @Override public int mePriority() { return priority; }

    @Override
    public void setMePriority(int value) {
        priority = value;
        setChanged();
        ICraftingProvider.requestUpdate(node);
    }

    /** Улучшение открыло или закрыло слоты шаблонов — сети нужен новый список. */
    @Override
    protected void meWork(IGrid grid) {
        if (activeSlots() != lastSlots) { lastSlots = activeSlots(); patternsChanged(); }
    }

    /** Снять улучшение можно, только если шаблоны в закрываемых слотах вынуты. */
    @Override
    protected boolean extraFits(int t) {
        for (int i = PortUpgradeItem.slots(t); i < patterns.getSlots(); i++)
            if (!patterns.getStackInSlot(i).isEmpty()) return false;
        return super.extraFits(t);
    }

    // ── поставщик шаблонов ──

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        Level level = getLevel();
        if (dirty && level != null) {
            List<IPatternDetails> out = new ArrayList<>();
            for (int i = 0; i < activeSlots(); i++) {
                ItemStack s = patterns.getStackInSlot(i);
                if (s.isEmpty()) continue;
                IPatternDetails d = PatternDetailsHelper.decodePattern(s, level);
                // только обработка: шаблоны верстака собирает молекулярный сборщик, не машина
                if (d != null && d.supportsPushInputsToExternalInventory()) out.add(d);
            }
            cache = List.copyOf(out);
            dirty = false;
        }
        return cache;
    }

    @Override public int getPatternPriority() { return priority; }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
        if (!node.isActive() || isBusy()) return false;
        List<ItemStack> stacks = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        for (KeyCounter counter : inputs)
            for (Object2LongMap.Entry<AEKey> e : counter) {
                AEKey key = e.getKey();
                long amount = e.getLongValue();
                if (key instanceof AEItemKey item) {
                    int max = item.getItem().getMaxStackSize();
                    for (long left = amount; left > 0; left -= max) stacks.add(item.toStack((int) Math.min(left, max)));
                } else if (key instanceof AEFluidKey fk) {
                    FluidStack f = fk.toStack((int) Math.min(Integer.MAX_VALUE, amount));
                    FluidStack same = fluids.stream().filter(x -> x.isFluidEqual(f)).findFirst().orElse(null);
                    if (same != null) same.grow(f.getAmount()); else fluids.add(f);
                } else return false;
            }
        int[] into = placeFluids(fluids);
        if (into == null || !fits(stacks)) return false;
        for (ItemStack s : stacks) ItemHandlerHelper.insertItemStacked(getItems(), s, false);
        List<FluidTank> tanks = tanks();
        for (int k = 0; k < fluids.size(); k++) tanks.get(into[k]).fill(fluids.get(k), IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    /**
     * Каждой жидкости шаблона — свой открытый бак: с той же жидкостью или пустой, и чтобы влезла целиком.
     * Вернёт номера баков по порядку жидкостей или null, если места нет.
     */
    @Nullable
    private int[] placeFluids(List<FluidStack> fluids) {
        List<FluidTank> tanks = tanks();
        int[] into = new int[fluids.size()];
        boolean[] taken = new boolean[tanks.size()];
        for (int k = 0; k < fluids.size(); k++) {
            FluidStack f = fluids.get(k);
            int pick = -1;
            for (int t = 0; t < tanks.size() && pick < 0; t++)   // сначала бак, где эта жидкость уже есть
                if (!taken[t] && tanks.get(t).getFluid().isFluidEqual(f)) pick = t;
            for (int t = 0; t < tanks.size() && pick < 0; t++)
                if (!taken[t] && tanks.get(t).isEmpty()) pick = t;
            if (pick < 0 || tanks.get(pick).fill(f, IFluidHandler.FluidAction.SIMULATE) < f.getAmount()) return null;
            taken[pick] = true;
            into[k] = pick;
        }
        return into;
    }

    /** Влезут ли все стаки в открытые слоты буфера: пробуем на копии. */
    private boolean fits(List<ItemStack> stacks) {
        ItemStackHandler copy = new ItemStackHandler(activeSlots());
        for (int i = 0; i < activeSlots(); i++) copy.setStackInSlot(i, getItems().getStackInSlot(i).copy());
        for (ItemStack s : stacks) if (!ItemHandlerHelper.insertItemStacked(copy, s.copy(), false).isEmpty()) return false;
        return true;
    }

    @Override public boolean isBusy() { return blocking && !isEmpty(); }

    // ── терминал доступа к шаблонам ──

    @Nullable @Override public IGrid getGrid() { return node.getGrid(); }
    @Override public boolean isVisibleInTerminal() { return showInTerminal; }
    @Override public InternalInventory getTerminalPatternInventory() { return terminalInv; }

    /** Группа в терминале — машина, в которую входит порт (её ядро), иначе сам порт. */
    @Override
    public PatternContainerGroup getTerminalGroup() {
        Level level = getLevel();
        BlockPos core = corePos();
        Block block = level != null && core != null ? level.getBlockState(core).getBlock() : getBlockState().getBlock();
        return new PatternContainerGroup(AEItemKey.of(block.asItem()), block.getName(), List.of());
    }

    // ── сохранение и снятие ──

    @Override
    public void dropContents(Level level, BlockPos pos) {
        super.dropContents(level, pos);
        for (int i = 0; i < patterns.getSlots(); i++)
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), patterns.getStackInSlot(i));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("mePatterns", patterns.serializeNBT());
        tag.putBoolean("meBlocking", blocking);
        tag.putBoolean("meHidden", !showInTerminal);
        tag.putInt("mePriority", priority);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("mePatterns")) patterns.deserializeNBT(tag.getCompound("mePatterns"));
        blocking = tag.getBoolean("meBlocking");
        showInTerminal = !tag.getBoolean("meHidden");
        priority = tag.getInt("mePriority");
        dirty = true;
    }
}
