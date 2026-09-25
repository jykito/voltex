package com.jykito.industrialcore.block.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * Бак, растущий вместе с модулем параллели.
 * <p>
 * Без этого крупный модуль упирался бы в бак на первой же партии: ×64 за раз просит в 64 раза
 * больше жидкости, и игрок видел бы «параллель не работает» вместо «не хватает бака».
 * <p>
 * Множители от базовой ёмкости, а не абсолютные числа: правка базы двигает всю лестницу.
 * Рост нарочно отстаёт от роста партии, поэтому крупный модуль требует более плотной подачи,
 * а не снимает с неё нагрузку.
 * <p>
 * ⚠️ Ёмкость НЕ лежит в NBT бака — {@code writeToNBT} хранит только жидкость. Поэтому её надо
 * сохранять и читать через {@link #save} и {@link #load}, иначе после перезахода бак вернётся
 * к базовой ёмкости, а шкала в GUI будет считать долю от неправильного числа.
 */
public final class AdaptiveTank {

    private AdaptiveTank() {}

    public static int capacityFor(int base, int parallel) {
        if (parallel >= 64) return base * 16;
        if (parallel >= 32) return base * 8;
        if (parallel >= 8)  return base * 4;
        return base;
    }

    /**
     * Подгоняет ёмкость под модуль. При сжатии излишек сливается: пока в баке больше ёмкости,
     * шкала считает долю больше единицы и жидкость рисуется столбом выше самого бака.
     * Это заодно и цена за снятие апгрейда.
     *
     * @return true, если ёмкость изменилась — тогда зовущему надо разослать обновление блока.
     */
    public static boolean resize(FluidTank tank, int base, int parallel) {
        int want = capacityFor(base, parallel);
        if (tank.getCapacity() == want) return false;

        tank.setCapacity(want);
        int excess = tank.getFluidAmount() - want;
        if (excess > 0) tank.drain(excess, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    public static void save(CompoundTag tag, String key, FluidTank tank) {
        tag.putInt(key, tank.getCapacity());
    }

    /** Звать ДО чтения жидкости, иначе бак прочитает содержимое в ещё старую ёмкость. */
    public static void load(CompoundTag tag, String key, FluidTank tank, int base) {
        tank.setCapacity(Math.max(base, tag.getInt(key)));
    }
}
