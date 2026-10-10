package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.block.custom.CryoCasingBlock;
import com.jykito.industrialcore.block.custom.CryoCoreBlock;
import com.jykito.industrialcore.block.custom.CryoGlassBlock;
import com.jykito.industrialcore.block.custom.RefrigerationChamberBlock;
import com.jykito.industrialcore.multiblock.WandBuild;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.item.custom.DepthModuleItem;
import com.jykito.industrialcore.menu.CryoChamberMenu;
import com.jykito.industrialcore.recipe.CachedRecipe;
import com.jykito.industrialcore.recipe.CryoRecipe;
import com.jykito.industrialcore.recipe.ModRecipes;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ядро криокамеры.
 *
 * <h2>В чём смысл</h2>
 * Тепло здесь <b>топливо</b>, а не побочка: абсорбционный принцип, как у газовых холодильников.
 * Цепочка {@code FE → термостанция → тепло → теплотрубы → криокамера → холод}.
 * <p>
 * Камера — не станок, а <b>установка</b>. Она копит {@link #depth ГЛУБИНУ}, и глубина и есть
 * настоящий ресурс: она набирается минутами устойчивой подачи и теряется быстрее, чем растёт.
 * Поэтому камеру не выключают.
 *
 * <h2>Как считается глубина</h2>
 * Каждый тик буфер тепла обнуляется и его содержимое читается как приток за этот тик. Приток
 * больше текущей глубины — глубина растёт, меньше — падает. Равновесие ровно там, где
 * глубина равна притоку, поэтому таблица ступеней читается напрямую в «тепле в тик».
 * <p>
 * Обнуление буфера ещё и держит его пустым, а теплотруба качает в потребителя по свободному
 * месту — если бы буфер стоял полным, подача бы захлебнулась.
 *
 *
 * <h2>Отказа нет намеренно</h2>
 * Просадка подачи просто ставит работу на паузу, прогресс сохраняется. Наказание за срыв
 * (потеря партии, трескающиеся панели) обсуждалось и <b>отвергнуто</b>: взрыв уже занят квенчем
 * магнита, и второй такой же отказ сделал бы все мультиблоки мода похожими друг на друга.
 * Не возвращать.
 */
public class CryoCoreBlockEntity extends BlockEntity implements MenuProvider {

    // ---- структура (тир 2 отличается только этими числами) ----
    private static final int DEPTH_D = 4, HEIGHT = 4, HALF_WIDTH = 2;
    private static final int FRIDGE_D = 2, FRIDGE_S = 0;
    private static final int CHECK_INTERVAL = 20;

    // ---- слоты ----
    private static final int SLOT_INPUT = 0, SLOT_OUTPUT = 1, SLOT_ROD_FIRST = 2, SLOT_ROD_LAST = 4;
    /** Модуль глубины: разрешение уходить глубже. Не тратится, старший перекрывает младший. */
    private static final int SLOT_MODULE = 5;

    // ---- ступени: порог глубины и расход хладагента в тик ----
    /**
     * Пороги привязаны к тому, что реально можно выдать. Полная башня термостанции на криогенных
     * теплообменниках даёт 1920 тепла в тик, а её пропуск при пятнадцати радиаторах — 1900.
     * Обе стены сходятся почти в точку, поэтому верхняя ступень стоит на 1800: эндгейм это
     * достроенная башня, а не «поставь вторую станцию».
     */
    private static final int[] STAGE_DEPTH   = {200, 800, 1800};
    private static final int[] STAGE_COOLANT = { 10,  40,  120};
    public  static final int MAX_DEPTH = 1800;

    /** Глубина растёт на единицу в тик, а теряется втрое быстрее: срыв подачи должен быть дорог. */
    private static final int DEPTH_RISE = 1, DEPTH_FALL = 3;

    private static final int TANK_CAP = 8000;
    private static final int ENERGY_CAPACITY = 200_000;
    private static final int MAX_HEAT = 20_000;

    /**
     * Сколько износа копится на одну единицу прочности стержня. Само накопление идёт квадратом
     * ступени, поэтому глубина и есть цена: T1 на третьей ступени живёт около четырёх минут,
     * а на первой — часами.
     */
    private static final int WEAR_PER_POINT = 40;

    private final CachedRecipe<CryoRecipe> recipeCache = new CachedRecipe<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(6) {
        @Override protected void onContentsChanged(int slot) { recipeCache.markDirty(); setChanged(); }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // Выход тут РАЗРЕШЁН намеренно: машина кладёт результат этим же хранилищем, и запрет
            // на уровне хранилища съедал бы готовый предмет молча. Руками и автоматикой в него
            // не положить — это ловится в меню (mayPlace) и в automationHandler.
            return switch (slot) {
                case SLOT_INPUT, SLOT_OUTPUT -> !isRod(stack) && !(stack.getItem() instanceof DepthModuleItem);
                case SLOT_MODULE             -> stack.getItem() instanceof DepthModuleItem;
                default                      -> isRod(stack);
            };
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private static boolean isRod(ItemStack s) {
        return s.getItem() == ModItems.CRYO_ROD.get()
            || s.getItem() == ModItems.CRYO_ROD_T2.get()
            || s.getItem() == ModItems.CRYO_ROD_T3.get();
    }

    /** Стержень ускоряет работу и тем сильнее, чем он лучше. */
    private static float rodBonus(ItemStack s) {
        if (s.getItem() == ModItems.CRYO_ROD_T3.get()) return 1.00f;
        if (s.getItem() == ModItems.CRYO_ROD_T2.get()) return 0.50f;
        if (s.getItem() == ModItems.CRYO_ROD.get())    return 0.25f;
        return 0f;
    }

    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot == SLOT_OUTPUT) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != SLOT_OUTPUT) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };

    public ItemStackHandler getItemHandler() { return itemHandler; }

    private final MachineEnergy energyStorage = MachineEnergy.consumer(ENERGY_CAPACITY, ENERGY_CAPACITY);
    private LazyOptional<EnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    /** Хладагент внутрь. */
    public final FluidTank coolantTank = new FluidTank(TANK_CAP,
            fs -> fs.getFluid() == ModFluids.SOURCE_COOLANT.get()) {
        @Override protected void onContentsChanged() { markTankChanged(); }
    };
    /** Отработанный хладагент наружу — его освежает обогатитель (enrich/coolant_regen: отработка + лазуритовая пыль). */
    public final FluidTank hotTank = new FluidTank(TANK_CAP) {
        @Override protected void onContentsChanged() { markTankChanged(); }
    };
    /** Жидкий продукт: третья ступень даёт сверхтекучий конденсат. */
    public final FluidTank productTank = new FluidTank(TANK_CAP) {
        @Override protected void onContentsChanged() { markTankChanged(); }
    };

    private void markTankChanged() {
        recipeCache.markDirty();
        setChanged();
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Наружу: принимаем хладагент, отдаём отработку и продукт. */
    private final IFluidHandler combinedFluid = new IFluidHandler() {
        @Override public int getTanks() { return 3; }
        @Override public @NotNull FluidStack getFluidInTank(int t) {
            return t == 0 ? coolantTank.getFluid() : t == 1 ? hotTank.getFluid() : productTank.getFluid();
        }
        @Override public int getTankCapacity(int t) { return TANK_CAP; }
        @Override public boolean isFluidValid(int t, @NotNull FluidStack fs) { return t == 0 && coolantTank.isFluidValid(fs); }
        @Override public int fill(FluidStack resource, FluidAction action) { return coolantTank.fill(resource, action); }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack out = productTank.drain(resource, action);
            return out.isEmpty() ? hotTank.drain(resource, action) : out;
        }
        @Override public @NotNull FluidStack drain(int max, FluidAction action) {
            FluidStack out = productTank.drain(max, action);
            return out.isEmpty() ? hotTank.drain(max, action) : out;
        }
    };
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    // ---- тепло ----
    /** Буфер прихода за тик. Обнуляется каждый тик, поэтому труба всегда качает на полную. */
    private int heatAccum = 0;
    /** Приток за прошлый тик — то, что показывается игроку и что задаёт глубину. */
    private int heatInflow = 0;

    private final com.jykito.industrialcore.heat.IHeatHandler heatHandler = new com.jykito.industrialcore.heat.IHeatHandler() {
        @Override public int getHeat() { return heatAccum; }
        @Override public int getMaxHeat() { return MAX_HEAT; }
        @Override public int receiveHeat(int amount, boolean simulate) {
            int accepted = Math.min(Math.max(0, amount), MAX_HEAT - heatAccum);
            if (!simulate && accepted > 0) { heatAccum += accepted; setChanged(); }
            return accepted;
        }
    };
    private LazyOptional<com.jykito.industrialcore.heat.IHeatHandler> lazyHeat = LazyOptional.empty();

    // ---- состояние ----
    private int checkTimer = 0;
    private boolean formed = false;
    /** Ступень прошлого тика, только для достижения (не сохраняется). */
    private int lastStage = 0;
    private int depth = 0;
    private int progress = 0;
    private int maxProgress = 400;
    private int rodWear = 0;

    protected final ContainerData data;

    public CryoCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYO_CORE_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                    case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    case 6 -> depth;
                    case 7 -> heatInflow;
                    case 8 -> formed ? 1 : 0;
                    case 9 -> allowedStage();
                    default -> 0;
                };
            }
            @Override
            public void set(int i, int v) {
                switch (i) {
                    case 0 -> progress = v;
                    case 1 -> maxProgress = v;
                    case 6 -> depth = v;
                    case 7 -> heatInflow = v;
                    case 8 -> formed = v != 0;
                    default -> { }
                }
            }
            @Override public int getCount() { return 10; }
        };
    }

    public boolean isFormed()  { return formed; }
    public int getDepth()      { return depth; }
    public int getHeatInflow() { return heatInflow; }

    /** Ступень по текущей глубине: 0 — камера ещё ничего не умеет. */
    public int getStage() { return stageFor(depth); }

    /** До какой ступени пускает вставленный модуль. Без модуля — только первая. */
    public int allowedStage() {
        ItemStack m = itemHandler.getStackInSlot(SLOT_MODULE);
        return m.getItem() instanceof DepthModuleItem mod ? mod.getTier() : 1;
    }

    private static int stageFor(int d) {
        for (int i = STAGE_DEPTH.length - 1; i >= 0; i--)
            if (d >= STAGE_DEPTH[i]) return i + 1;
        return 0;
    }

    public static int depthForStage(int stage) {
        return stage >= 1 && stage <= STAGE_DEPTH.length ? STAGE_DEPTH[stage - 1] : 0;
    }

    /** Расход хладагента в тик на этой ступени — тем же числом его показывает JEI. */
    public static int coolantPerTick(int stage) {
        return stage >= 1 && stage <= STAGE_COOLANT.length ? STAGE_COOLANT[stage - 1] : STAGE_COOLANT[0];
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CryoCoreBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        // Приход за тик и обнуление буфера: держим место свободным, иначе труба перестанет качать.
        heatInflow = heatAccum;
        heatAccum = 0;

        if (++checkTimer >= CHECK_INTERVAL) {
            checkTimer = 0;
            boolean nowFormed = checkStructure(level, pos, state) == null;
            if (nowFormed && !formed)
                com.jykito.industrialcore.advancement.VoltexEventTrigger.award(level, pos, 16,
                        com.jykito.industrialcore.advancement.VoltexEventTrigger.CRYO_FORMED);
            if (nowFormed != formed) { formed = nowFormed; setChanged(); }
            linkCasings(level, pos, state, nowFormed);
        }

        // Разобранная камера мгновенно теряет глубину: держать её нечем.
        if (!formed) {
            if (depth > 0) { depth = Math.max(0, depth - DEPTH_FALL * 4); setChanged(); }
            progress = 0;
            setRunning(level, pos, state, false);
            return;
        }

        // Глубина идёт за притоком, но упирается в потолок модуля: без разрешения камера глубже
        // не пойдёт, сколько тепла ни лей. Именно это и не обходится количеством котельных.
        int cap = depthForStage(allowedStage());
        if (heatInflow > depth)      depth = Math.min(cap, depth + DEPTH_RISE);
        else if (heatInflow < depth) depth = Math.max(0, depth - DEPTH_FALL);
        if (depth > cap) depth = cap;   // модуль вынули на ходу

        int stage = getStage();
        if (stage == 3 && lastStage != 3)   // достижение «На самом дне»: только в момент перехода, не каждый тик
            com.jykito.industrialcore.advancement.VoltexEventTrigger.award(level, pos, 16,
                    com.jykito.industrialcore.advancement.VoltexEventTrigger.CRYO_STAGE_3);
        lastStage = stage;
        CryoRecipe recipe = findRecipe(level, stage);

        boolean working = false;
        if (recipe != null) {
            int coolantPerTick = STAGE_COOLANT[Math.max(0, recipe.getRequiredStage() - 1)];
            int energyPerTick  = recipe.getEnergyCostPerTick();

            this.maxProgress = Math.max(1, (int)(recipe.getProcessingTime() / rodSpeedFactor()));

            boolean deepEnough = stage >= recipe.getRequiredStage();
            boolean hasCoolant = coolantTank.getFluidAmount() >= coolantPerTick
                    && hotTank.getSpace() >= coolantPerTick;
            boolean hasEnergy  = energyStorage.getEnergyStored() >= energyPerTick;

            // Не сошлось хоть одно условие — работа просто встаёт, прогресс стоит на месте.
            // Наказания за просадку нет намеренно: взрыв уже занят квенчем магнита, и второй
            // такой же отказ сделал бы все мультиблоки мода одинаковыми.
            if (deepEnough && hasCoolant && hasEnergy) {
                energyStorage.consumeEnergy(energyPerTick);
                cycleCoolant(coolantPerTick);
                wearRods(level, stage);
                progress++;
                working = true;
                if (progress >= maxProgress) { craft(recipe); progress = 0; }
            }
        } else {
            progress = 0;
        }

        setRunning(level, pos, state, working);
        setChanged();
    }

    /** Хладагент проходит через камеру и уходит горячим — его освежает обогатитель. */
    private void cycleCoolant(int amount) {
        coolantTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        hotTank.fill(new FluidStack(ModFluids.SOURCE_HOT_COOLANT.get(), amount), IFluidHandler.FluidAction.EXECUTE);
    }

    /** Стержни — теплообменники внутри: ускоряют работу и стачиваются тем быстрее, чем глубже камера. */
    private float rodSpeedFactor() {
        float f = 1f;
        for (int s = SLOT_ROD_FIRST; s <= SLOT_ROD_LAST; s++)
            f += rodBonus(itemHandler.getStackInSlot(s));
        return f;
    }

    private void wearRods(Level level, int stage) {
        rodWear += Math.max(1, stage * stage); // третья ступень стачивает вдевятеро быстрее первой
        if (rodWear < WEAR_PER_POINT) return;
        rodWear -= WEAR_PER_POINT;

        // Стачиваются ВСЕ вставленные разом: три стержня дают четырёхкратную скорость, но и
        // расходуются втрое быстрее. Иначе набивать все слоты было бы бесплатным выигрышем.
        for (int s = SLOT_ROD_FIRST; s <= SLOT_ROD_LAST; s++) {
            ItemStack rod = itemHandler.getStackInSlot(s);
            if (rod.isEmpty()) continue;
            if (rod.hurt(1, level.random, null)) itemHandler.setStackInSlot(s, ItemStack.EMPTY);
        }
    }

    private void setRunning(Level level, BlockPos pos, BlockState state, boolean running) {
        boolean lit = MachineLit.tick(this, level, pos, state, CryoCoreBlock.LIT, running);
        lightFridges(level, pos, state.getValue(CryoCoreBlock.FACING), lit);
    }

    @Nullable
    private CryoRecipe findRecipe(Level level, int stage) {
        return recipeCache.get(() -> {
            ItemStack in = itemHandler.getStackInSlot(SLOT_INPUT);
            if (in.isEmpty()) return null;
            for (CryoRecipe r : level.getRecipeManager().getAllRecipesFor(ModRecipes.CRYO_FREEZING_TYPE.get())) {
                if (!r.inputMatches(in)) continue;
                if (r.hasItemResult() && !fitsOutput(r.getResult())) continue;
                if (r.hasFluidResult()) {
                    FluidStack out = r.makeFluidResult();
                    if (!productTank.isEmpty() && !productTank.getFluid().isFluidEqual(out)) continue;
                    if (productTank.getSpace() < out.getAmount()) continue;
                }
                return r;
            }
            return null;
        });
    }

    private boolean fitsOutput(ItemStack result) {
        ItemStack out = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(out, result)) return false;
        return out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void craft(CryoRecipe recipe) {
        itemHandler.extractItem(SLOT_INPUT, recipe.getIngredientCount(), false);
        if (recipe.hasItemResult())
            itemHandler.insertItem(SLOT_OUTPUT, recipe.getResult().copy(), false);
        if (recipe.hasFluidResult())
            productTank.fill(recipe.makeFluidResult(), IFluidHandler.FluidAction.EXECUTE);
    }

    /**
     * Прописывает корпусу, где ядро, — тогда тепло можно подавать в любой блок пола или потолка.
     * <p>
     * Это полсотни граней наружу вместо шести у ядра, а глубокой ступени нужно несколько линий
     * труб сразу. Обходятся только два слоя корпуса, раз в {@link #CHECK_INTERVAL} тиков; связь
     * переписывается заново каждый раз, поэтому подменённый блок подхватится сам.
     */
    private void linkCasings(Level level, BlockPos pos, BlockState state, boolean formedNow) {
        Direction facing = state.getValue(CryoCoreBlock.FACING);
        for (int h : new int[]{0, HEIGHT}) {
            for (int d = 0; d <= DEPTH_D; d++) {
                for (int s = -HALF_WIDTH; s <= HALF_WIDTH; s++) {
                    if (h == 0 && d == 0 && s == 0) continue;              // само ядро
                    BlockEntity be = level.getBlockEntity(local(pos, facing, d, s, h));
                    if (!(be instanceof CryoCasingBlockEntity casing)) continue;
                    if (formedNow) casing.setCore(pos); else casing.clearCore();
                }
            }
        }
    }

    // Обмерзание панелей по ступени убрано. Пробовали красить тинтом — не вышло и не выйдет:
    // тинт УМНОЖАЕТ цвет, то есть умеет только затемнять, а иней должен делать стекло белее.
    // Если решим вернуться — только слоем поверх панели с отдельной текстурой инея.

    /** Колонна холодильников горит вместе с ядром. */
    private void lightFridges(Level level, BlockPos pos, Direction facing, boolean lit) {
        for (int h = 1; h < HEIGHT; h++) {
            BlockPos p = local(pos, facing, FRIDGE_D, FRIDGE_S, h);
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof RefrigerationChamberBlock
                    && st.getValue(RefrigerationChamberBlock.LIT) != lit)
                level.setBlock(p, st.setValue(RefrigerationChamberBlock.LIT, lit), 3);
        }
    }

    /** Первое несовпадение с описанием структуры, или null если всё на месте. */
    @Nullable
    public String firstMismatch(Level level, BlockPos pos, BlockState state) {
        return checkStructure(level, pos, state);
    }

    /** Позиция в локальных осях ядра: d вглубь, s вбок, h вверх. */
    private static BlockPos local(BlockPos core, Direction facing, int d, int s, int h) {
        Direction back  = facing.getOpposite();
        Direction right = facing.getClockWise();
        return core.relative(back, d).relative(right, s).above(h);
    }

    @Nullable
    private String checkStructure(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(CryoCoreBlock.FACING);

        for (int h = 0; h <= HEIGHT; h++) {
            for (int d = 0; d <= DEPTH_D; d++) {
                for (int s = -HALF_WIDTH; s <= HALF_WIDTH; s++) {
                    if (h == 0 && d == 0 && s == 0) continue;              // само ядро

                    BlockPos p = local(pos, facing, d, s, h);
                    Part want = partAt(d, s, h);
                    if (!fits(want, level.getBlockState(p)))
                        return want.label + " @ " + p.getX() + ", " + p.getY() + ", " + p.getZ();
                }
            }
        }
        return null;
    }

    /** Что должно стоять в этой клетке. Одно описание раскладки на проверку и на сборку жезлом. */
    private enum Part {
        CASING("корпус"), GLASS("стекло"), FRIDGE("холодильник"), AIR("пусто");
        final String label;
        Part(String label) { this.label = label; }
    }

    /** Подходит ли блок в клетку. Общий критерий для проверки и для сборки палочкой. */
    private static boolean fits(Part want, BlockState st) {
        return switch (want) {
            case CASING -> st.getBlock() instanceof CryoCasingBlock;
            case GLASS  -> st.getBlock() instanceof CryoGlassBlock;
            case FRIDGE -> st.getBlock() instanceof RefrigerationChamberBlock;
            case AIR    -> st.isAir();
        };
    }

    private static Part partAt(int d, int s, int h) {
        if (h == 0 || h == HEIGHT) return Part.CASING;
        if (d == 0 || d == DEPTH_D || s == -HALF_WIDTH || s == HALF_WIDTH) return Part.GLASS;
        if (d == FRIDGE_D && s == FRIDGE_S) return Part.FRIDGE;
        return Part.AIR;
    }

    /**
     * Ставит камеру вокруг ядра структурной палочкой: в творческом бесплатно и поверх всего, в выживании из инвентаря и только в пустое.
     * <p>
     * Раскладку берёт из {@link #partAt}, той же, по которой идёт проверка. Иначе сборка и
     * валидатор со временем разъедутся, и жезл будет ставить то, что камера не примет.
     */
    public static void buildStructure(Level level, BlockPos corePos, Direction facing, WandBuild wb) {
        for (int h = 0; h <= HEIGHT; h++) {
            for (int d = 0; d <= DEPTH_D; d++) {
                for (int s = -HALF_WIDTH; s <= HALF_WIDTH; s++) {
                    if (h == 0 && d == 0 && s == 0) continue;              // само ядро
                    BlockPos p = local(corePos, facing, d, s, h);
                    Part part = partAt(d, s, h);
                    if (fits(part, level.getBlockState(p))) continue;
                    // в творческом палочка расчищает и перезаписывает всё, как раньше;
                    // в выживании чужое не ломает: ставит только в пустое и из инвентаря
                    if (part == Part.AIR) {
                        if (wb.free()) wb.overwrite(level, p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        else wb.markBlocked();
                        continue;
                    }
                    BlockState put = switch (part) {
                        case CASING -> com.jykito.industrialcore.block.ModBlocks.CRYO_CASING.get().defaultBlockState();
                        case GLASS  -> com.jykito.industrialcore.block.ModBlocks.CRYO_GLASS.get().defaultBlockState();
                        default     -> com.jykito.industrialcore.block.ModBlocks.REFRIGERATION_CHAMBER.get().defaultBlockState();
                    };
                    // флаг 3 внутри: соседям приходит обновление, поэтому соединения панелей и корпуса
                    // сходятся сами, без второго прохода
                    if (wb.free()) wb.overwrite(level, p, put);
                    else wb.place(level, p, put);
                }
            }
        }
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public Component getDisplayName() { return Component.translatable("block.industrial_core.cryo_core"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CryoChamberMenu(id, inv, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY)        return lazyEnergyHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER)  return lazyItemHandler.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        if (cap == com.jykito.industrialcore.heat.HeatCapability.HEAT) return lazyHeat.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        lazyItemHandler   = LazyOptional.of(() -> automationHandler);
        lazyFluidHandler  = LazyOptional.of(() -> combinedFluid);
        lazyHeat          = LazyOptional.of(() -> heatHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergyHandler.invalidate();
        lazyItemHandler.invalidate();
        lazyFluidHandler.invalidate();
        lazyHeat.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("coolantTank", coolantTank.writeToNBT(new CompoundTag()));
        tag.put("hotTank",     hotTank.writeToNBT(new CompoundTag()));
        tag.put("productTank", productTank.writeToNBT(new CompoundTag()));
        tag.putBoolean("formed", formed);
        tag.putInt("depth", depth);
        tag.putInt("progress", progress);
        tag.putInt("rodWear", rodWear);
        tag.putInt("heatInflow", heatInflow);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // ⚠️ deserializeNBT берёт число слотов из NBT, а не из кода. Камеры, сохранённые до
        // появления слота модуля, приносят Size=5 и схлопывают хранилище назад — тик потом
        // падает на шестом слоте. Выкидываем Size, тогда размер остаётся кодовый, а предметы
        // разложатся по своим индексам. Та же грабля ждёт любую машину, у которой вырос
        // список слотов.
        CompoundTag inv = tag.getCompound("inventory").copy();
        inv.remove("Size");
        itemHandler.deserializeNBT(inv);
        energyStorage.setEnergy(tag.getInt("energy"));
        coolantTank.readFromNBT(tag.getCompound("coolantTank"));
        hotTank.readFromNBT(tag.getCompound("hotTank"));
        productTank.readFromNBT(tag.getCompound("productTank"));
        formed = tag.getBoolean("formed");
        depth = tag.getInt("depth");
        progress = tag.getInt("progress");
        rodWear = tag.getInt("rodWear");
        heatInflow = tag.getInt("heatInflow");
    }
}
