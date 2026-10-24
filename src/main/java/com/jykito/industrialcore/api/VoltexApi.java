package com.jykito.industrialcore.api;

/**
 * Entry point of the Voltex API for add-ons. Everything listed here stays compatible within Voltex 3.x;
 * {@link #VERSION} goes up when something is added. See API.md in the repository for examples.
 * <ul>
 *   <li>{@link VoltexNanites}: nanite classes, scientist topics, rare traits, chamber surroundings.</li>
 *   <li>{@link VoltexUpgrades}: shared machine upgrades (accelerator, parallel, pushers, pullers) in add-on machines.</li>
 *   <li>{@link VoltexWind}: wind turbine rotors (packs: the KubeJS startup event {@code ICStartupEvents.windRotor}).</li>
 *   <li>{@link WandAssemblable}: multiblocks the structure wand completes.</li>
 *   <li>{@code LateMachineCoreBlock} / {@code LateMachineCoreBlockEntity} / {@code LateMultiblockPattern}: machines on the
 *       late multiblock logic, with own frame blocks, sockets and concealed (model-drawn) structures.</li>
 *   <li>{@link PowerSuitArmor}: armour drawn with the Voltex power suit model.</li>
 *   <li>{@link com.jykito.industrialcore.api.client.GuideModels}: formed multiblock models in the guide panel (client).</li>
 *   <li>{@code block.custom.cover.Covers} / {@code CoverHolder}: blocks that can hide inside a cover, like cables.</li>
 *   <li>{@link RestructorRecipeRegistry}, {@code api.solar.SolarPanelRegistry}: restructor recipes and solar panels.</li>
 * </ul>
 * Open without any API: Voltex recipe types in data packs, restructor catalysts (any {@code CatalystItem}), the heat
 * capability ({@code heat.HeatCapability}), guide chapters from any mod ({@code assets/industrial_core/guide/<lang>/}).
 */
public final class VoltexApi {
    /** 1: first public API (Voltex 3.0.4). */
    public static final int VERSION = 1;

    private VoltexApi() {}
}
