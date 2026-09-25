package com.jykito.industrialcore.block.entity;

import com.jykito.industrialcore.heat.HeatCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Корпус криокамеры как проводник ядра.
 * <p>
 * В ядро надо завести тепло, хладагент, отработку, продукт, энергию и предметы — на шести
 * гранях это не помещается, а одной только третьей ступени нужно пять линий труб под тепло.
 * Поэтому любой блок корпуса отдаёт наружу capability ядра: пол и потолок дают полсотни
 * граней, подключайся к любой.
 * <p>
 * Сущность намеренно <b>не тикает</b> и ничего не хранит — она только переадресует. Позицию
 * ядра пишет само ядро при проверке структуры: оно и так обходит каждую клетку, второй поиск
 * не нужен. Пока камера не собрана, {@code corePos} пуст и корпус не отдаёт ничего — так труба
 * или кабель не подключатся к недостроенной коробке, и по ним сразу видно, что структура
 * ещё не сошлась.
 */
public class CryoCasingBlockEntity extends BlockEntity {

    @Nullable
    private BlockPos corePos;

    public CryoCasingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYO_CASING_BE.get(), pos, state);
    }

    /** Зовёт ядро при успешной проверке структуры. */
    public void setCore(BlockPos pos) {
        if (pos.equals(corePos)) return;
        corePos = pos.immutable();
        setChanged();
    }

    public void clearCore() {
        if (corePos == null) return;
        corePos = null;
        setChanged();
    }

    @Nullable
    private BlockEntity core() {
        if (level == null || corePos == null) return null;
        BlockEntity be = level.getBlockEntity(corePos);
        if (!(be instanceof CryoCoreBlockEntity) || be.isRemoved()) { corePos = null; return null; }
        return be;
    }

    /**
     * Отдаём ровно то, что отдаёт ядро. Возвращается его же {@code LazyOptional}, поэтому при
     * сносе ядра подписчики узнают об этом сами, без второго механизма инвалидации.
     */
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (isRemoved()) return LazyOptional.empty();
        if (cap == HeatCapability.HEAT
                || cap == ForgeCapabilities.ENERGY
                || cap == ForgeCapabilities.ITEM_HANDLER
                || cap == ForgeCapabilities.FLUID_HANDLER) {
            BlockEntity core = core();
            if (core != null) return core.getCapability(cap, side);
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (corePos != null) tag.putLong("core", corePos.asLong());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        corePos = tag.contains("core") ? BlockPos.of(tag.getLong("core")) : null;
    }
}
