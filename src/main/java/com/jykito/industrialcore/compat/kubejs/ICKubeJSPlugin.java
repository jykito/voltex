package com.jykito.industrialcore.compat.kubejs;

import com.jykito.industrialcore.IndustrialCore;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.script.ScriptType;

public class ICKubeJSPlugin extends KubeJSPlugin {

    @Override
    public void registerEvents() {
        ICStartupEvents.GROUP.register();
    }

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        event.namespace(IndustrialCore.MODID)
                .register("liquefier_recipe",      ICRecipeSchemas.LIQUEFIER)
                .register("ore_washing_recipe",    ICRecipeSchemas.ORE_WASHING)
                .register("fluid_enricher_recipe", ICRecipeSchemas.FLUID_ENRICHER)
                .register("phase_splitter_recipe", ICRecipeSchemas.PHASE_SPLITTER)
                .register("rod_fabrication_recipe", ICRecipeSchemas.ROD_FABRICATION)
                .register("blast_furnace_recipe",  ICRecipeSchemas.BLAST_FURNACE)
                .register("growing_recipe",        ICRecipeSchemas.GROWING)

                .register("crushing",               ICRecipeSchemas.MACHINE)
                .register("compressing",            ICRecipeSchemas.MACHINE)
                .register("electric_smelting",      ICRecipeSchemas.MACHINE)
                .register("extracting",             ICRecipeSchemas.MACHINE)
                .register("extruding",              ICRecipeSchemas.MACHINE)
                .register("rolling",                ICRecipeSchemas.MACHINE)
                .register("molecular_transforming", ICRecipeSchemas.MACHINE);
    }

    @Override
    public void initStartup() {
        ICStartupEvents.SOLAR_PANEL.post(ScriptType.STARTUP, new SolarPanelEventJS());
        ICStartupEvents.RESTRUCTOR.post(ScriptType.STARTUP, new RestructorEventJS());
    }
}
