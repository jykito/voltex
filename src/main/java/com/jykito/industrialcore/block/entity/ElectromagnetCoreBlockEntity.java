package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.Config;
import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.custom.ElectromagnetCoreBlock;
import com.jykito.industrialcore.block.custom.MultiblockShellBlock;
import com.jykito.industrialcore.energy.MachineEnergy;
import com.jykito.industrialcore.menu.ElectromagnetMenu;
import com.jykito.industrialcore.multiblock.WandBuild;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
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
 * BE ядра электромагнита-ускорителя. Валидирует скелет 7×7×8 (по схеме, с поворотом по фейсингу),
 * ставит FORMED. Корпус (тело/катушку/столбы) рисует ElectromagnetCoreBER при FORMED.
 */
public class ElectromagnetCoreBlockEntity extends BlockEntity implements MenuProvider {

    // Канонич. схема (фронт = север). Слои низ(Y=0)..верх(Y=7); строки север(0)..юг(6); столбцы запад(0)..восток(6).
    // S = структурный, H = держатель катушки, C = ядро (якорь, не проверяем), '.' = пусто.
    private static final String[] SCHEMA = {
        // Y=0 (низ)
        ".SSSSS.","SSSSSSS","SSSSSSS","SSSSSSS","SSSSSSS","SSSSSSS",".SSSSS.",
        // Y=1
        "...C...",".S...S.","..SSS..","..SSS..","..SSS..",".S...S.",".......",
        // Y=2
        ".......",".S...S.",".......","...H...",".......",".S...S.",".......",
        // Y=3
        "S.....S",".......",".......",".......",".......",".......","S.....S",
        // Y=4
        "S.....S",".......",".......",".......",".......",".......","S.....S",
        // Y=5
        "S.....S",".......",".......",".......",".......",".......","S.....S",
        // Y=6
        ".......",".S...S.",".......","...H...",".......",".S...S.",".......",
        // Y=7 (верх)
        ".......",".S...S.","..SSS..","..SSS..","..SSS..",".S...S.",".......",
    };
    private static final int SIZE = 7, HEIGHT = 8;
    private static final int CORE_LAYER = 1, CORE_ROW = 0, CORE_COL = 3;

    // ── ТЕСТ: магнитное поле (тянет сущности к центру) ──
    private static final double PULL_RADIUS = 8.0;
    private static final double PULL_STRENGTH = 0.10;
    private static final double LIFT_MULT = 1.8; // вариант 1: доп. подъём когда сущность НИЖЕ центра
    private static final double DISINTEGRATE_RADIUS = 1.5; // ТЕСТ: касание катушки в центре = дезинтеграция
    private static final double BURST_RADIUS = 4.5;        // радиус разлёта партиклов при дезинтеграции
    // Поле притяжения: радиус и сила растут с нестабильностью (0 без катушки). Радиус мониторим издалека через пульт.
    /** Радиус поля притяжения (блоки): 0 без катушки, иначе 2.5 + нестаб%*0.14. */
    public static double fieldRadius(float instability, int coilTier) {
        return coilTier <= 0 ? 0.0 : 2.5 + instability * 0.14;
    }

    private boolean formed = false;
    private boolean working = false;   // пока нет логики ускорителя → столбы крутятся медленно
    private int checkCooldown = 0;

    // ── ТЕСТ: клиентский разгон/звук (плавный spin01 → визуал + питч/громкость гула + пролёты столбов) ──
    private static final float IDLE_DEG = 1.2f;   // град/тик вхолостую
    private static final float WORK_DEG = 9.0f;   // град/тик на разгоне
    private static final float SPIN_EASE = 0.03f; // скорость разгона/торможения (меньше = дольше набор)
    private float spin01 = 0f, spin01Prev = 0f;         // 0..1 уровень разгона (клиент)
    private float spinAngle = 0f, spinAnglePrev = 0f;   // накопленный угол столбов/катушки (клиент)
    private static final float INSTAB_SPIN = 0.06f;     // +град/тик за каждый % нестабильности

    // ── Катушка (ставится ПКМ по держателю) + нестабильность ──
    // катушка хранится в itemHandler, слот COIL_SLOT (10) — единая точка правды (GUI-слот + держатель)
    private float instability = 0f;               // 0..100 %, синкается на клиент (визуал поля/оборотов)
    private float throttle = 0f;                  // 0..1 ручная мощность (ползунок в GUI); ∝ рост нестаб. + расход FE
    private float effThrottle = 0f;               // фактическая мощность: редстоун перекрывает ручную, если есть сигнал
    private int   redstonePower = 0;              // входной редстоун 0..15 → мощность
    private int   lastComparatorSignal = 0;       // кэш выхода компаратора (нотифай только при смене)
    private int   driftPhase = 0;                 // фаза дрейфа резонансной цели (0..DRIFT_PERIOD)
    private float resonanceTarget = 0f;           // текущая цель нестабильности (дрейфует в полосе рецепта), синк в GUI
    private float resonanceCharge = 0f;           // 0..100 заряд резонанса; полный → ×2
    private float cachedCool = 0f;                 // троттл дорогого скана колонн (охлаждение меняется медленно)
    private int coolCd = 0;
    // троттл дорогого AABB-скана сущностей поля: список кэшируется, сам пул идёт каждый тик (визуал/сила без изменений)
    private java.util.List<net.minecraft.world.entity.Entity> fieldEntities = java.util.Collections.emptyList();
    private int fieldScanCd = 0;
    private static final int FIELD_SCAN_TICKS = 5;
    private static final float RESPONSE_RATE = 0.08f; // доля закрытия разрыва до цели за тик — мягкий СИММЕТР. отклик (без мёртвой зоны, но не рывками)
    private static final float POWER_SMOOTH  = 0.2f;  // сглаживание 16-ступенчатой редстоун-мощности → ползунок не скачет + петля меньше «ищет»
    private static final int   PHASE_LOCK_COST = 400; // FE/тик за авто-режим Фазозахвата (окно само держится на цели)
    private static final int QUENCH_RADIUS = 32;       // радиус волны расщепления при 100%
    // Антиграв-колонны на тепле гасят нестабильность (тепло→стабильность): держи поток без квенча
    private static final double COLUMN_RADIUS = 16.0;  // радиус помощи от центра структуры
    private static final int    MAX_COLUMNS  = 8;      // кап вклада колонн (не тривиализировать)
    private static final float  COLUMN_COOL  = 0.05f;  // охлаждение/тик на полную (100% тепла) колонну
    // Раскрутка: запитанный магнит с катушкой лезет к потолку нестабильности по тиру; колонны тянут вниз
    private static final float  BASE_QUENCH     = 50f;  // порог квенча БЕЗ пилонов (низкий → без охлаждения высоко не разогнаться)
    private static final float  QUENCH_PER_COOL = 150f; // пилоны поднимают порог квенча (охлаждение = безопасный потолок нестаб.)
    private static final int    SPIN_UPKEEP = 200;     // FE/тик на поддержание раскрутки на полном троттле (∝ троттл)
    // ── Резонанс: дрейфующая цель + троттл + заряд → ×2 (активный скилл вместо пассивной полосы) ──
    private static final int    DRIFT_PERIOD  = 600;   // тиков на цикл дрейфа цели (~30с) — медленнее, чтоб авто-контур поспевал
    private static final float  RESONANCE_TOL = 3f;    // ±% допуск: узкий («процент в процент») — ×2 руками = скилл; надёжное ×2 = апгрейд-автоследователь
    private static final float  CHARGE_RISE   = 2.0f;  // заряд/тик в допуске (копится быстрее)
    private static final float  CHARGE_DECAY  = 0.7f;  // заряд/тик вне допуска (тает лениво — ×2 переживает краткие срывы/отставание)
    private static final float  RESONANCE_X2  = 85f;   // порог заряда для ×2
    // ── Редстоун: вход = мощность (0..15), выход-компаратор = рассогласование резонанса ──
    private static final int    COMP_ON_TARGET = 7;    // 7 = нестаб. точно на цели; >7 перелёт, <7 недолёт
    private static final float  COMP_ERR_SCALE = 3f;   // % рассогласования на 1 ступень компаратора
    // Скорость крафта растёт с нестабильностью → риск/награда живой и на ×1 (не только ради ×2)
    private static final float  SPEED_GAIN = 3.0f;     // прирост множителя скорости на 100% над экзо-гейтом
    private static final float  SPEED_MAX  = 3.0f;     // кап множителя скорости

    // ── Машина-ускоритель: 8 входов(0-7) + выход(8) + 2 апгрейда(9,10, пока заблокированы) ──
    private final com.jykito.industrialcore.recipe.CachedRecipe<com.jykito.industrialcore.recipe.AcceleratorRecipe> recipeCache =
            new com.jykito.industrialcore.recipe.CachedRecipe<>();
    private final ItemStackHandler itemHandler = new ItemStackHandler(11) {
        @Override protected void onContentsChanged(int slot) {
            recipeCache.markDirty(); setChanged();
            if (slot == COIL_SLOT) syncMagnet();   // катушка сменилась (в т.ч. перетаскиванием в GUI) → обновить BER/поле на клиенте
        }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 8) return false;                            // выход закрыт
            if (slot == 9) return stack.getItem() == com.jykito.industrialcore.item.ModItems.PHASE_LOCK_UPGRADE.get(); // слот апгрейда: только Фазозахват
            if (slot == COIL_SLOT) return stack.getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem; // слот катушки
            return super.isItemValid(slot, stack);
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private final IItemHandlerModifiable automationHandler = new IItemHandlerModifiable() {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public int getSlots() { return itemHandler.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return itemHandler.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot > 7) return stack;             // авто-вставка только во входы 0..7
            return itemHandler.insertItem(slot, stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 8) return ItemStack.EMPTY;  // авто-извлечение только из выхода
            return itemHandler.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return itemHandler.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return itemHandler.isItemValid(slot, stack); }
    };
    public ItemStackHandler getItemHandler() { return itemHandler; }

    // ── Наружу для порт-BE нижней платформы: те же капы, что ядро отдаёт своим прямым соседям ──
    public IItemHandler getExternalItemHandler() { return automationHandler; }
    public IFluidHandler getExternalFluidHandler() { return combinedFluidHandler; }
    public net.minecraftforge.energy.IEnergyStorage getExternalEnergyHandler() { return energyStorage; }

    private final MachineEnergy energyStorage = MachineEnergy.consumer(1_000_000, 20_000);
    private LazyOptional<EnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    // 2 бака, оба входные (внешний налив, без слива)
    public final FluidTank tankA = new FluidTank(16_000) {
        @Override protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    public final FluidTank tankB = new FluidTank(16_000) {
        @Override protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    private final IFluidHandler combinedFluidHandler = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return tank == 0 ? tankA.getFluid() : tankB.getFluid(); }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? tankA.getCapacity() : tankB.getCapacity(); }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return true; }
        @Override public int fill(FluidStack resource, FluidAction action) {
            int f = tankA.fill(resource, action);
            if (f < resource.getAmount()) {
                FluidStack rest = resource.copy(); rest.shrink(f);
                f += tankB.fill(rest, action);
            }
            return f;
        }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    private final ContainerData data;
    private float progress = 0f;      // float ради плавной скорости ∝ нестабильности
    private int maxProgress = 1;

    public ElectromagnetCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROMAGNET_CORE_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override public int get(int i) {
                return switch (i) {
                    case 0 -> (int) progress;
                    case 1 -> maxProgress;
                    case 2 -> energyStorage.getEnergyStored() & 0xFFFF;
                    case 3 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                    case 4 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                    case 5 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                    case 6 -> (int) instability;        // 0..100 % — читаемость в GUI
                    case 7 -> working ? 1 : 0;
                    case 8 -> inBandNow ? 1 : 0;        // заряд резонанса полон → ×2 (индикатор в GUI)
                    case 9 -> (int) resonanceTarget;    // 0..100 дрейфующая цель резонанса
                    case 10 -> (int) resonanceCharge;   // 0..100 заряд резонанса
                    case 11 -> (int) (effThrottle * 100f); // 0..100 фактическая мощность (редстоун/ползунок)
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) { if (i == 0) progress = v; else if (i == 1) maxProgress = v; }
            @Override public int getCount() { return 12; }
        };
    }

    public boolean isWorking() { return working; }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        if (formed) {
            redstonePower = level.getBestNeighborSignal(pos);                  // редстоун-вход → мощность
            if (redstonePower > 0) effThrottle += (redstonePower / 15f - effThrottle) * POWER_SMOOTH; // сглаживаем ступени редстоуна (не скачет)
            else effThrottle = throttle;                                       // ручной ползунок — сразу
            tickInstability(level, pos);  // сначала раскрутка/колонны → свежая нестабильность
            tickProcessing(level);        // крафт по свежей нестаб.: экзо-гейт + резонанс ×2
            pullEntities(level, pos);     // поле-побочка (тянет у центра)
            updateComparator(level, pos); // редстоун-выход: рассогласование резонанса
            if (--outputCd <= 0) { outputCd = 8; autoOutput(level, pos); } // автовывод в соседний интерфейс
        } else {
            working = false;
        }

        if (--checkCooldown > 0) return;
        checkCooldown = formed ? 20 : 40;

        if (formed) {
            Direction f = state.getValue(ElectromagnetCoreBlock.FACING);
            if (checkStructure(level, pos, f)) {
                setShellFormed(level, pos, f, true);   // держим скелет скрытым (само-синхр. после релога/апдейта)
                return;
            }
            formed = false;
            setShellFormed(level, pos, f, false);   // вернуть видимость скелету
            level.setBlock(pos, state.setValue(ElectromagnetCoreBlock.FORMED, false), 3);
            setChanged();
            return;
        }

        // не собран — пробуем собрать в любой из 4 сторон, ставим найденный фейсинг
        for (Direction f : Direction.Plane.HORIZONTAL) {
            if (checkStructure(level, pos, f)) {
                formed = true;
                com.jykito.industrialcore.advancement.VoltexEventTrigger.award(level, pos, 16,
                        com.jykito.industrialcore.advancement.VoltexEventTrigger.MAGNET_FORMED);
                level.setBlock(pos, state.setValue(ElectromagnetCoreBlock.FACING, f)
                        .setValue(ElectromagnetCoreBlock.FORMED, true), 3);
                setShellFormed(level, pos, f, true);   // спрятать скелет — корпус занимает его место
                setChanged();
                return;
            }
        }
    }

    // ── Катушка + нестабильность ──

    public float getInstability() { return instability; }
    /** Результат текущего рецепта (что варится) — для экрана дист.контроллера; EMPTY если нет катушки/рецепта. */
    public ItemStack getInProgressResult() {
        if (level == null || getCoilTier() == 0) return ItemStack.EMPTY;
        var r = findRecipe(level);
        return r == null ? ItemStack.EMPTY : r.getResult();
    }
    /** Троттл раскрутки 0..1 — ставится игроком (пакет MagnetThrottlePacket). */
    public void setThrottle(float t) { throttle = Mth.clamp(t, 0f, 1f); setChanged(); }
    public float getThrottle() { return throttle; }
    public float getResonanceTarget() { return resonanceTarget; }
    public float getResonanceCharge() { return resonanceCharge; }
    // Катушка живёт в слоте 10 itemHandler — единая точка правды: GUI-слот и держатель пишут сюда, вся логика читает отсюда.
    private static final int COIL_SLOT = 10;
    public ItemStack getCoil() { return itemHandler.getStackInSlot(COIL_SLOT); }
    public int getCoilTier() {
        return getCoil().getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem m ? m.getTier() : 0;
    }
    private float coilSpinMult() {
        return getCoil().getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem m ? m.getSpinMult() : 1f;
    }

    /** Ставит катушку (1 шт) в слот, если слот пуст и это катушка. */
    public boolean installCoil(ItemStack stack) {
        if (!getCoil().isEmpty() || !(stack.getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem)) return false;
        itemHandler.setStackInSlot(COIL_SLOT, stack.copyWithCount(1));   // onContentsChanged → markDirty + syncMagnet
        return true;
    }
    /** Снимает катушку из слота, возвращает снятый стек (или EMPTY). */
    public ItemStack removeCoil() {
        ItemStack out = getCoil();
        if (out.isEmpty()) return ItemStack.EMPTY;
        itemHandler.setStackInSlot(COIL_SLOT, ItemStack.EMPTY);
        instability = 0f;                                               // нет катушки → сброс нестабильности
        return out;
    }
    private void syncMagnet() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** ТЕСТ-драйв: под нагрузкой (редстоун) с катушкой нестабильность растёт (× тир), в простое падает; 100% → квенч. */
    private void tickInstability(Level level, BlockPos pos) {
        float prev = instability;
        int tier = getCoilTier();
        float ceiling = tierCeiling(tier);
        if (--coolCd <= 0) { coolCd = 5; cachedCool = columnCooling(level); } // дорогой O(колонны) скан раз в 5 тиков
        float cool = cachedCool;
        float quenchThreshold = Mth.clamp(BASE_QUENCH + cool * QUENCH_PER_COOL, BASE_QUENCH, 100f); // пилоны = безопасный потолок нестаб.
        driftPhase = (driftPhase + 1) % DRIFT_PERIOD;                                          // резонансная цель дрейфует, пока собран
        // Фазозахват: модуль в слоте апгрейда сам держит окно на цели → надёжное ×2 (но не выше безопасного потолка)
        boolean phaseLock = itemHandler.getStackInSlot(9).getItem() == com.jykito.industrialcore.item.ModItems.PHASE_LOCK_UPGRADE.get();
        if (phaseLock && tier > 0 && resonanceTarget > 0f && energyStorage.getEnergyStored() >= PHASE_LOCK_COST) {
            energyStorage.consumeEnergy(PHASE_LOCK_COST);
            instability = Math.min(resonanceTarget, quenchThreshold - 1f);                     // липнет к цели, но не в квенч (без пилонов до полосы не дотянет)
            if (Math.abs(instability - resonanceTarget) <= RESONANCE_TOL) resonanceCharge = 100f; // дотянул до цели → надёжное ×2
        } else {
            int upkeep = (int) (SPIN_UPKEEP * effThrottle);                                    // расход ∝ мощность (редстоун/ползунок)
            boolean powered = tier > 0 && effThrottle > 0f && energyStorage.getEnergyStored() >= upkeep;
            float eq;                                                                          // «цель» нестабильности: куда её тянет мощность
            if (powered) {
                energyStorage.consumeEnergy(upkeep);
                eq = ceiling * effThrottle;                                                    // мощность линейно задаёт цель (потолок реаха — от катушки)
            } else {
                eq = 0f;                                                                       // нет питания / катушки → стремится к 0
            }
            instability = Mth.clamp(instability + (eq - instability) * RESPONSE_RATE, 0f, 100f); // мягкий симметричный отклик — окно идёт за мощностью
        }
        if (instability >= quenchThreshold) {
            com.jykito.industrialcore.magnet.MagnetQuench.start((net.minecraft.server.level.ServerLevel) level, center(), QUENCH_RADIUS);
            instability = 0f;
            return;
        }
        if ((int) instability != (int) prev) // синк на клиент по смене целого % → визуал поля/оборотов
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
    }

    /** Потолок нестабильности по тиру катушки: выше катушка → глубже в экзо-режим (ближе к квенчу). */
    private static float tierCeiling(int tier) {
        return switch (tier) { case 3 -> 105f; case 2 -> 80f; case 1 -> 45f; default -> 0f; };
    }

    /** Суммарное охлаждение от активных антиграв-колонн в радиусе (× доля тепла каждой, кап MAX_COLUMNS). */
    private float columnCooling(Level level) {
        if (AntigravColumnBlockEntity.COLUMNS.isEmpty()) return 0f;
        net.minecraft.world.phys.Vec3 c = center();
        double r2 = COLUMN_RADIUS * COLUMN_RADIUS;
        float sum = 0f; int n = 0;
        for (var col : AntigravColumnBlockEntity.COLUMNS) {
            if (col.getLevel() != level || col.isRemoved() || !col.isActive()) continue;
            double dx = col.getBlockPos().getX() + 0.5 - c.x;
            double dy = col.getBlockPos().getY() + 0.5 - c.y;
            double dz = col.getBlockPos().getZ() + 0.5 - c.z;
            if (dx * dx + dy * dy + dz * dz > r2) continue;
            col.markEngaged();                    // визуально: колонна работает на магнит
            sum += col.getHeatFraction();
            if (++n >= MAX_COLUMNS) break;
        }
        return sum * COLUMN_COOL;
    }

    // ── Обработка (сборка/столкновение: 1–8 общих входов → выход; экзо-гейт + ×2 в полосе нестабильности) ──
    private boolean inBandNow = false; // текущий крафт идёт в оптимальной полосе → ×2 (индикатор/логика)
    private int outputCd = 0;          // кулдаун автовывода

    private ItemStack[] inputSlots() {
        ItemStack[] s = new ItemStack[8];
        for (int i = 0; i < 8; i++) s[i] = itemHandler.getStackInSlot(i);
        return s;
    }

    private com.jykito.industrialcore.recipe.AcceleratorRecipe findRecipe(Level level) {
        return recipeCache.get(() -> {
            ItemStack[] slots = inputSlots();
            int tier = getCoilTier();
            for (com.jykito.industrialcore.recipe.AcceleratorRecipe r :
                    level.getRecipeManager().getAllRecipesFor(com.jykito.industrialcore.recipe.ModRecipes.ACCELERATING_TYPE.get())) {
                if (tier >= r.getMinTier() && r.matches(slots)) return r;
            }
            return null;
        });
    }

    private boolean outputFits(ItemStack result, int mult) {
        int add = result.getCount() * mult;
        ItemStack cur = itemHandler.getStackInSlot(8);
        if (cur.isEmpty()) return add <= result.getMaxStackSize();
        return ItemStack.isSameItemSameTags(cur, result) && cur.getCount() + add <= cur.getMaxStackSize();
    }

    private void tickProcessing(Level level) {
        boolean was = working;
        working = false; inBandNow = false;
        int tier = getCoilTier();
        var recipe = (tier == 0) ? null : findRecipe(level);
        updateResonance(recipe);                                                         // цель+заряд идут всегда (и между крафтами)
        if (recipe == null || tier < recipe.getMinTier() || !outputFits(recipe.getResult(), 1)) {
            progress = 0; maxProgress = 1; if (was) syncMagnet(); return;                // нет рецепта / выход забит
        }
        if (instability < recipe.getMinInstab()) { if (was) syncMagnet(); return; }      // экзо-гейт: ждём разгона
        maxProgress = Math.max(1, recipe.getProcessingTime());
        float speed = craftSpeed(recipe);                                                // скорость крафта ∝ нестабильность (выше = быстрее)
        int energy = Math.max(1, (int) (recipe.getEnergyPerTick() * speed));             // быстрее → больше FE/тик (итого за крафт ~как было)
        if (energyStorage.getEnergyStored() < energy) { if (was) syncMagnet(); return; } // мало энергии — пауза
        working = true;
        inBandNow = resonanceCharge >= RESONANCE_X2;                                     // заряд резонанса полон → индикатор ×2
        energyStorage.consumeEnergy(energy);
        progress += speed;
        if (progress >= maxProgress) { craft(recipe); progress = 0; }
        if (was != working) syncMagnet();
    }

    /** Скорость крафта ∝ нестабильности: ×1 у экзо-гейта → быстрее к потолку (с капом). Нестаб. осмысленна и без ×2. */
    private float craftSpeed(com.jykito.industrialcore.recipe.AcceleratorRecipe recipe) {
        float over = Math.max(0f, instability - recipe.getMinInstab());
        float instabFactor = Math.min(SPEED_MAX, 1f + SPEED_GAIN * over / 100f);
        float coilBase = getCoil().getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem m ? m.getCraftSpeed() : 1f;
        return coilBase * instabFactor;                                                        // катушка = движок (база), нестабильность = разгон
    }

    /** Дрейф резонансной цели в полосе рецепта [bandLo,bandHi] + накопление/спад заряда по попаданию нестабильности. */
    private void updateResonance(com.jykito.industrialcore.recipe.AcceleratorRecipe recipe) {
        float lo = recipe == null ? 0f : recipe.getBandLo();
        float hi = recipe == null ? 0f : recipe.getBandHi();
        if (hi <= lo) {                                                                  // рецепт без полосы → резонанс невозможен
            resonanceTarget = 0f;
            resonanceCharge = Math.max(0f, resonanceCharge - CHARGE_DECAY);
            return;
        }
        float f = 0.5f - 0.5f * Mth.cos((float) (2.0 * Math.PI * driftPhase / DRIFT_PERIOD)); // 0..1..0 плавно
        resonanceTarget = lo + (hi - lo) * f;
        boolean inTol = Math.abs(instability - resonanceTarget) <= RESONANCE_TOL;
        resonanceCharge = Mth.clamp(resonanceCharge + (inTol ? CHARGE_RISE : -CHARGE_DECAY), 0f, 100f);
    }

    /** Выход компаратора: рассогласование резонанса. 7 = нестаб. на цели, >7 перелёт, <7 недолёт, 0 = нет цели. */
    public int getComparatorSignal() {
        if (resonanceTarget <= 0f) return 0;                                              // 0 зарезервирован под «нет цели»
        return Mth.clamp(COMP_ON_TARGET + Math.round((instability - resonanceTarget) / COMP_ERR_SCALE), 1, 15);
    }

    /** Нотифай компараторов только при смене ступени (не каждый тик). */
    private void updateComparator(Level level, BlockPos pos) {
        int comp = getComparatorSignal();
        if (comp != lastComparatorSignal) {
            lastComparatorSignal = comp;
            level.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
        }
    }

    private void craft(com.jykito.industrialcore.recipe.AcceleratorRecipe recipe) {
        // расход входов: каждому ингредиенту — свой слот 0-7 (жадное сопоставление, как в matches)
        ItemStack[] slots = inputSlots();
        boolean[] used = new boolean[8];
        var ing = recipe.getInputs(); int[] cnt = recipe.getCounts();
        for (int i = 0; i < ing.length; i++) {
            for (int j = 0; j < 8; j++) {
                if (used[j]) continue;
                if (ing[i].test(slots[j]) && slots[j].getCount() >= cnt[i]) { itemHandler.extractItem(j, cnt[i], false); used[j] = true; break; }
            }
        }
        // ×2, если на завершении держим полосу нестабильности и выход вмещает
        int mult = (resonanceCharge >= RESONANCE_X2 && outputFits(recipe.getResult(), 2)) ? 2 : 1; // ×2 только за удержанный резонанс
        if (mult == 2)
            com.jykito.industrialcore.advancement.VoltexEventTrigger.award(level, worldPosition, 32,
                    com.jykito.industrialcore.advancement.VoltexEventTrigger.MAGNET_RESONANCE);
        ItemStack out = recipe.getResult().copy();
        out.setCount(out.getCount() * mult);
        ItemStack cur = itemHandler.getStackInSlot(8);
        if (cur.isEmpty()) itemHandler.setStackInSlot(8, out);
        else cur.grow(out.getCount());
        setChanged();
    }

    /** Автовывод: толкает готовый предмет из выхода в инвентари, стоящие в упор к блокам нижней платформы. */
    private void autoOutput(Level level, BlockPos core) {
        if (itemHandler.getStackInSlot(8).isEmpty()) return;
        Direction facing = getBlockState().getValue(ElectromagnetCoreBlock.FACING);
        for (int row = 0; row < SIZE; row++) {
            String rowStr = SCHEMA[row];                            // нижний слой Y=0 (платформа)
            for (int col = 0; col < SIZE; col++) {
                if (rowStr.charAt(col) != 'S') continue;
                int[] r = rot(col - CORE_COL, row - CORE_ROW, facing);
                BlockPos p = core.offset(r[0], -CORE_LAYER, r[1]);
                for (Direction d : Direction.values()) {
                    BlockEntity nb = level.getBlockEntity(p.relative(d));
                    if (nb == null || nb == this || nb instanceof MagnetPortBlockEntity) continue; // не в себя/свои порты (фидбэк)
                    var cap = nb.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite());
                    if (!cap.isPresent()) continue;
                    ItemStack out = itemHandler.getStackInSlot(8);
                    if (out.isEmpty()) return;
                    ItemStack rem = net.minecraftforge.items.ItemHandlerHelper.insertItem(cap.orElse(null), out.copy(), false);
                    if (rem.getCount() < out.getCount()) {
                        itemHandler.setStackInSlot(8, rem.isEmpty() ? ItemStack.EMPTY : rem);
                        setChanged();
                    }
                }
            }
        }
    }

    /**
     * Строит скелет по схеме вокруг ядра (структурная палочка). Ставит только в заменяемые клетки,
     * готовые не трогает; в выживании блоки берутся из инвентаря (см. {@link WandBuild}).
     */
    public static void buildStructure(Level level, BlockPos core, Direction facing, WandBuild wb) {
        Block structural = ModBlocks.STRUCTURAL_BLOCK.get();
        Block holder     = ModBlocks.COIL_HOLDER.get();
        for (int layer = 0; layer < HEIGHT; layer++) {
            for (int row = 0; row < SIZE; row++) {
                String rowStr = SCHEMA[layer * SIZE + row];
                for (int col = 0; col < SIZE; col++) {
                    char c = rowStr.charAt(col);
                    if (c == '.' || c == 'C') continue;                 // воздух + ядро пропускаем
                    int dx = col - CORE_COL, dy = layer - CORE_LAYER, dz = row - CORE_ROW;
                    int[] r = rot(dx, dz, facing);
                    BlockPos p = core.offset(r[0], dy, r[1]);
                    Block want = (c == 'H') ? holder : structural;
                    if (!level.getBlockState(p).is(want)) wb.place(level, p, want.defaultBlockState());
                }
            }
        }
    }

    /** Весь 7×7×8 скелет по схеме (с поворотом смещений по фейсингу). */
    private boolean checkStructure(Level level, BlockPos core, Direction facing) {
        Block structural = ModBlocks.STRUCTURAL_BLOCK.get();
        Block holder     = ModBlocks.COIL_HOLDER.get();
        for (int layer = 0; layer < HEIGHT; layer++) {
            for (int row = 0; row < SIZE; row++) {
                String rowStr = SCHEMA[layer * SIZE + row];
                for (int col = 0; col < SIZE; col++) {
                    char c = rowStr.charAt(col);
                    if (c == '.' || c == 'C') continue;
                    int dx = col - CORE_COL, dy = layer - CORE_LAYER, dz = row - CORE_ROW;
                    int[] r = rot(dx, dz, facing);
                    BlockPos p = core.offset(r[0], dy, r[1]);
                    Block want = (c == 'H') ? holder : structural;
                    if (!level.getBlockState(p).is(want)) return false;
                }
            }
        }
        return true;
    }

    /** Поворот смещения (dx,dz) как модели в BER: NORTH=0/WEST=90/SOUTH=180/EAST=270 (ось YP). */
    public static int[] rot(int dx, int dz, Direction facing) {
        return switch (facing) {
            case WEST  -> new int[]{ dz, -dx};
            case SOUTH -> new int[]{-dx, -dz};
            case EAST  -> new int[]{-dz,  dx};
            default    -> new int[]{ dx,  dz}; // NORTH
        };
    }

    /** ТЕСТ: тянет все сущности в радиусе к центру структуры. */
    private void pullEntities(Level level, BlockPos core) {
        if (getCoilTier() == 0) return; // нет катушки → нет поля → нет притяжки
        double radius = fieldRadius(instability, getCoilTier()); // растущий радиус: 2.5 + нестаб%*0.14
        net.minecraft.world.phys.Vec3 center = center();
        net.minecraft.server.level.ServerLevel sl = (net.minecraft.server.level.ServerLevel) level;

        // дорогой AABB-скан троттлится (раз в FIELD_SCAN_TICKS), список кэшируется; сам пул — КАЖДЫЙ тик по кэшу,
        // со свежими позициями/дистанцией/силой → визуал и механика 1-в-1, тяжёлый getEntities реже в 5 раз.
        if (--fieldScanCd <= 0) {
            fieldScanCd = FIELD_SCAN_TICKS;
            double r = radius + 1.0; // чуть шире радиуса — ловим входящих между сканами (и рост радиуса)
            AABB box = new AABB(center.x - r, center.y - r, center.z - r, center.x + r, center.y + r, center.z + r);
            fieldEntities = level.getEntities((net.minecraft.world.entity.Entity) null, box,
                    en -> !en.isSpectator() && en.isAlive());
        }

        for (net.minecraft.world.entity.Entity e : fieldEntities) {
            if (!e.isAlive() || e.isSpectator()) continue;              // из кэша мог выбыть/умереть между сканами
            net.minecraft.world.phys.Vec3 to = center.subtract(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
            double dist = to.length();
            if (dist > radius) continue;
            if (dist < DISINTEGRATE_RADIUS) { disintegrate(sl, e, center); continue; } // касание катушки → дезинтеграция
            double force = (1.0 - dist / radius) * PULL_STRENGTH * (1.0 + instability * 0.05); // масштаб силы растёт с нестаб.
            net.minecraft.world.phys.Vec3 pull = to.normalize().scale(force);
            if (to.y > 0.0) pull = pull.add(0.0, force * LIFT_MULT, 0.0); // ниже центра → подбрасывать в открытую клетку
            e.setDeltaMovement(e.getDeltaMovement().add(pull));
            e.hasImpulse = true;
            if (e instanceof net.minecraft.server.level.ServerPlayer sp) sp.hurtMarked = true;
        }
    }

    /** ТЕСТ: касание катушки — дезинтеграция: большой красивый разлёт партиклов, спиралью в центр + смерть. */
    private void disintegrate(net.minecraft.server.level.ServerLevel sl, net.minecraft.world.entity.Entity e,
                              net.minecraft.world.phys.Vec3 center) {
        // щадим креатив/спектатора — можно безопасно стоять в центре и тестить
        if (e instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) return;
        double ex = e.getX(), ey = e.getY() + e.getBbHeight() * 0.5, ez = e.getZ();
        net.minecraft.core.particles.SimpleParticleType mote = com.jykito.industrialcore.particle.ModParticles.ENERGY_MOTE.get();
        // разлёт кастомных мотов вокруг сущности — закрутятся к оси катушки и стянутся в линию
        // (центр орбиты передаётся через velocity-слоты sendParticles при speed=1)
        for (int i = 0; i < 70; i++) {
            double az = Math.random() * Math.PI * 2, pol = Math.acos(2 * Math.random() - 1);
            double rr = Math.cbrt(Math.random()) * BURST_RADIUS;
            double ox = ex + rr * Math.sin(pol) * Math.cos(az);
            double oy = ey + rr * Math.cos(pol);
            double oz = ez + rr * Math.sin(pol) * Math.sin(az);
            sl.sendParticles(mote, ox, oy, oz, 0, center.x, center.y, center.z, 1.0);
        }
        // ВОЛНА: кольцо мотов вокруг оси катушки — схлопывается в центр
        int ring = 48;
        for (int i = 0; i < ring; i++) {
            double a = i * (Math.PI * 2.0 / ring);
            double ox = center.x + BURST_RADIUS * Math.cos(a);
            double oz = center.z + BURST_RADIUS * Math.sin(a);
            sl.sendParticles(mote, ox, center.y, oz, 0, center.x, center.y, center.z, 1.0);
        }
        // честное «испарение»: игрок — гарантир. смерть в обход брони/резиста/тотема (genericKill, как /kill);
        // моб/предмет — тихий discard без дропа и сообщения
        if (e instanceof net.minecraft.world.entity.player.Player) {
            e.kill();
        } else {
            e.discard();
        }
    }

    // ── ТЕСТ: клиентский разгон + звук ──

    public float getSpinLevel() { return spin01; }

    /** Плавный угол столбов/катушки для BER (интерполяция по partialTick, с разматыванием 360→0). */
    public float getSpinAngle(float partialTick) {
        float a0 = spinAnglePrev, a1 = spinAngle;
        if (a1 < a0) a1 += 360f;
        return (a0 + (a1 - a0) * partialTick) % 360f;
    }

    /** Центр структуры (куда тянет поле и откуда идёт звук): ядро + rot(0,3) по горизонтали, +3 по Y. */
    private net.minecraft.world.phys.Vec3 center() {
        Direction facing = getBlockState().getValue(ElectromagnetCoreBlock.FACING);
        int[] r = rot(0, 3, facing);
        return new net.minecraft.world.phys.Vec3(
                worldPosition.getX() + 0.5 + r[0],
                worldPosition.getY() + 0.5 + 3.0,
                worldPosition.getZ() + 0.5 + r[1]);
    }

    /**
     * Клиентский тик: плавный разгон/торможение (spin01), накопление угла столбов + звук —
     * луп-гул с питчем/громкостью по скорости, свист на каждый пролёт столба (через 90°), powerup на старте.
     * ТЕСТ-триггер разгона: редстоун-сигнал на ядро (потом заменить на логику ускорителя).
     */
    public void clientTick(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(ElectromagnetCoreBlock.FORMED)) { spin01 = spin01Prev = 0f; return; }
        boolean hasCoil = getCoilTier() > 0;
        // нет катушки → столбы стоят; есть+вхолостую → медленно; в работе → быстро; нестабильность разгоняет
        float target = !hasCoil ? 0f : (isWorking() ? 1f : 0.30f);
        spin01Prev = spin01;
        spin01 += (target - spin01) * SPIN_EASE;
        float speed = Mth.lerp(spin01, 0f, WORK_DEG) * coilSpinMult() + instability * INSTAB_SPIN;
        spinAnglePrev = spinAngle;
        spinAngle = (spinAngle + speed) % 360f;

        com.jykito.industrialcore.client.MagnetSoundClient.ensureHum(this); // луп-гул (клиент-онли класс)

        if (spin01 > 0.2f && spin01Prev <= 0.2f) {            // старт разгона → powerup
            net.minecraft.world.phys.Vec3 c = center();
            level.playLocalSound(c.x, c.y, c.z, com.jykito.industrialcore.sound.ModSounds.MAGNET_POWERUP.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.9f * (float) Config.soundVolume, 1.0f, false);
        }
        int q = (int) (spinAngle / 90f) & 3, qp = (int) (spinAnglePrev / 90f) & 3;
        if (q != qp) {                                        // столб прошёл ближнюю точку → пролёт
            net.minecraft.world.phys.Vec3 c = center();
            level.playLocalSound(c.x, c.y, c.z, com.jykito.industrialcore.sound.ModSounds.MAGNET_WHOOSH.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    (0.30f + 0.60f * spin01) * (float) Config.soundVolume, 0.80f + 0.35f * spin01, false);
        }
    }

    /** Прячет (true) / показывает (false) скелет-блоки, чтобы корпус-OBJ не z-fight'ил с ними. */
    public static void setShellFormed(Level level, BlockPos core, Direction facing, boolean formedVal) {
        for (int layer = 0; layer < HEIGHT; layer++) {
            for (int row = 0; row < SIZE; row++) {
                String rowStr = SCHEMA[layer * SIZE + row];
                for (int col = 0; col < SIZE; col++) {
                    char c = rowStr.charAt(col);
                    if (c == '.' || c == 'C') continue;
                    int dx = col - CORE_COL, dy = layer - CORE_LAYER, dz = row - CORE_ROW;
                    int[] r = rot(dx, dz, facing);
                    BlockPos p = core.offset(r[0], dy, r[1]);
                    BlockState bs = level.getBlockState(p);
                    if (!(bs.getBlock() instanceof MultiblockShellBlock)) continue;
                    if (bs.getValue(MultiblockShellBlock.FORMED) != formedVal)
                        level.setBlock(p, bs.setValue(MultiblockShellBlock.FORMED, formedVal), 3);
                    bindPort(level, p, core, formedVal, dy == -1); // низ платформы (dy=-1) = входная поверхность
                }
            }
        }
    }

    /** Привязка/отвязка порт-BE скелет-блока к ядру; для старых (до-апдейт) структур принудительно создаёт BE. */
    private static void bindPort(Level level, BlockPos p, BlockPos core, boolean formedVal, boolean isInput) {
        BlockState cur = level.getBlockState(p);
        if (!(cur.getBlock() instanceof MultiblockShellBlock)) return;
        BlockEntity be = level.getBlockEntity(p);
        if (be == null && formedVal && cur.getBlock() instanceof net.minecraft.world.level.block.EntityBlock eb) {
            be = eb.newBlockEntity(p, cur);            // блок стал EntityBlock после апдейта — досоздаём BE
            if (be != null) level.setBlockEntity(be);
        }
        if (be instanceof MagnetPortBlockEntity port) {
            if (formedVal) port.bindCore(core, isInput);
            else port.unbind();
        }
    }

    /** Корпус ×7 торчит далеко за блок — расширяем bbox при сборке, иначе фрустум-куллинг срежет. */
    @Override
    public AABB getRenderBoundingBox() {
        // ВАЖНО: читаем блокстейт (синкается на клиент), а НЕ поле formed (оно клиентом=false) —
        // иначе bbox=1 блок и корпус отсекается фрустумом под углом.
        if (!getBlockState().getValue(ElectromagnetCoreBlock.FORMED)) return super.getRenderBoundingBox();
        return new AABB(worldPosition).inflate(16); // покрывает корпус + макс. вынос дуг поля (для off-screen cull)
    }

    // ── Меню / капабилити / клиент-синк (жидкости баков рисуются из BE) ──
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public Component getDisplayName() { return Component.translatable("block.industrial_core.electromagnet_core"); }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ElectromagnetMenu(id, inv, this, this.data);
    }

    /** Меню для удалённого пульта: данные из ядра, дистанция валидируется по anchor (позиция контроллера). */
    public AbstractContainerMenu remoteMenu(int id, Inventory inv, net.minecraft.core.BlockPos anchor) {
        return new ElectromagnetMenu(id, inv, this, this.data, anchor);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (getBlockState().getValue(ElectromagnetCoreBlock.FORMED)) { // машина активна только собранной
            if (cap == ForgeCapabilities.ENERGY)        return lazyEnergyHandler.cast();
            if (cap == ForgeCapabilities.ITEM_HANDLER)  return lazyItemHandler.cast();
            if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler   = LazyOptional.of(() -> automationHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        lazyFluidHandler  = LazyOptional.of(() -> combinedFluidHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
        lazyFluidHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("formed", formed);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("tankA", tankA.writeToNBT(new CompoundTag()));
        tag.put("tankB", tankB.writeToNBT(new CompoundTag()));
        tag.putInt("progress", (int) progress);
        tag.putFloat("instability", instability);
        tag.putFloat("throttle", throttle);
        tag.putBoolean("working", working);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        formed = tag.getBoolean("formed");
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        tankA.readFromNBT(tag.getCompound("tankA"));
        tankB.readFromNBT(tag.getCompound("tankB"));
        progress = tag.getInt("progress");
        // миграция старых сейвов: катушка держалась в "coil" — перенести в слот COIL_SLOT
        if (tag.contains("coil") && getCoil().isEmpty()) {
            ItemStack legacy = ItemStack.of(tag.getCompound("coil"));
            if (!legacy.isEmpty()) itemHandler.setStackInSlot(COIL_SLOT, legacy);
        }
        instability = tag.getFloat("instability");
        throttle = tag.getFloat("throttle");
        working = tag.getBoolean("working");
    }
}
