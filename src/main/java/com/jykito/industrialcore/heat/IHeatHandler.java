package com.jykito.industrialcore.heat;

/**
 * Тепло как ресурс: уровень (условная температура) 0..max.
 * Передача только «толчком» (receiveHeat) от горячего к холодному — как теплопроводность.
 */
public interface IHeatHandler {

    /** Текущий уровень тепла. */
    int getHeat();

    /** Максимум. */
    int getMaxHeat();

    /**
     * Принять тепло. Возвращает сколько реально принято (ограничено ёмкостью).
     * @param simulate если true — только прикинуть, не менять состояние.
     */
    int receiveHeat(int amount, boolean simulate);
}
