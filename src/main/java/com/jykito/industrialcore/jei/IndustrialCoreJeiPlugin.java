package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.networking.ModMessages;
import com.jykito.industrialcore.recipe.*;
import net.minecraft.world.item.Items;
import com.jykito.industrialcore.screen.HubScreen;
import com.jykito.industrialcore.screen.MePortScreen;
import com.jykito.industrialcore.networking.MePortPacket;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import java.util.Arrays;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class IndustrialCoreJeiPlugin implements IModPlugin {

    public static final RecipeType<MachineRecipe> CRUSHING =
            RecipeType.create(IndustrialCore.MODID, "crushing", MachineRecipe.class);
    public static final RecipeType<MachineRecipe> COMPRESSING =
            RecipeType.create(IndustrialCore.MODID, "compressing", MachineRecipe.class);
    public static final RecipeType<MachineRecipe> ELECTRIC_SMELTING =
            RecipeType.create(IndustrialCore.MODID, "electric_smelting", MachineRecipe.class);
    public static final RecipeType<MachineRecipe> ROLLING =
            RecipeType.create(IndustrialCore.MODID, "rolling", MachineRecipe.class);
    public static final RecipeType<MachineRecipe> EXTRACTING =
            RecipeType.create(IndustrialCore.MODID, "extracting", MachineRecipe.class);
    public static final RecipeType<MachineRecipe> EXTRUDING =
            RecipeType.create(IndustrialCore.MODID, "extruding", MachineRecipe.class);

    public static final RecipeType<MachineRecipe> RESTRUCTOR =
            RecipeType.create(IndustrialCore.MODID, "restructor_jei", MachineRecipe.class);
    public static final RecipeType<BlastFurnaceRecipe> BLAST_FURNACE =
            RecipeType.create(IndustrialCore.MODID, "blast_furnace_jei", BlastFurnaceRecipe.class);
    public static final RecipeType<RodFabricatorRecipe> ROD_FABRICATOR =
            RecipeType.create(IndustrialCore.MODID, "rod_fabricator_jei", RodFabricatorRecipe.class);
    public static final RecipeType<FluidEnricherRecipe> FLUID_ENRICHER =
            RecipeType.create(IndustrialCore.MODID, "fluid_enricher_jei", FluidEnricherRecipe.class);
    public static final RecipeType<BioreactorRecipe> BIOREACTOR =
            RecipeType.create(IndustrialCore.MODID, "bioreactor_jei", BioreactorRecipe.class);

    public static final RecipeType<OreWashingRecipe> ORE_WASHING =
            RecipeType.create(IndustrialCore.MODID, "ore_washer_jei", OreWashingRecipe.class);

    public static final RecipeType<LiquefierRecipe> LIQUEFYING =
            RecipeType.create(IndustrialCore.MODID, "liquefying", LiquefierRecipe.class);

    public static final RecipeType<com.jykito.industrialcore.recipe.CryoRecipe> CRYO_FREEZING =
            RecipeType.create(IndustrialCore.MODID, "cryo_freezing", com.jykito.industrialcore.recipe.CryoRecipe.class);

    public static final RecipeType<com.jykito.industrialcore.recipe.CrystallizerRecipe> CRYSTALLIZING =
            RecipeType.create(IndustrialCore.MODID, "crystallizing", com.jykito.industrialcore.recipe.CrystallizerRecipe.class);

    public static final RecipeType<PhaseSplitterRecipe> PHASE_SPLITTING =
            RecipeType.create(IndustrialCore.MODID, "phase_splitting", PhaseSplitterRecipe.class);

    public static final RecipeType<MinerLensRecipe> MINER_LENS =
            RecipeType.create(IndustrialCore.MODID, "miner_lens", MinerLensRecipe.class);

    public static final RecipeType<GrowingRecipe> GROWING_PLANT =
            RecipeType.create(IndustrialCore.MODID, "growing_plant", GrowingRecipe.class);
    public static final RecipeType<GrowingRecipe> GROWING_MINERAL =
            RecipeType.create(IndustrialCore.MODID, "growing_mineral", GrowingRecipe.class);
    public static final RecipeType<com.jykito.industrialcore.block.entity.StoneGeneratorMode> STONE_GEN =
            RecipeType.create(IndustrialCore.MODID, "stone_generator", com.jykito.industrialcore.block.entity.StoneGeneratorMode.class);
    public static final RecipeType<com.jykito.industrialcore.block.entity.FluidGeneratorType> FLUID_GEN =
            RecipeType.create(IndustrialCore.MODID, "fluid_generator", com.jykito.industrialcore.block.entity.FluidGeneratorType.class);
    public static final RecipeType<CapsuleFillerJeiRecipe> CAPSULE_FILLER =
            RecipeType.create(IndustrialCore.MODID, "capsule_filler", CapsuleFillerJeiRecipe.class);
    public static final RecipeType<AcceleratorRecipe> MAGNET =
            RecipeType.create(IndustrialCore.MODID, "magnet_jei", AcceleratorRecipe.class);

    /**
     * Окна лейт-машин (Л1, LateJeiCategory) — только то, чего нет в окнах малых машин: рецепты промышленного режима
     * (lateOnly; малые машины их не выполняют, поэтому из их окон они убраны) и сборка (малого сборщика нет).
     * Обычные рецепты лейт-машины остаются в окнах малых машин, ядро там — катализатор.
     */
    private record Late(RecipeType<LateJeiCategory.Entry> type, java.util.function.Supplier<net.minecraft.world.level.block.Block> core, String mode) {}
    private static Late late(String id, java.util.function.Supplier<net.minecraft.world.level.block.Block> core, String mode) {
        return new Late(RecipeType.create(IndustrialCore.MODID, "late_" + id, LateJeiCategory.Entry.class), core, mode);
    }
    private static final List<Late> LATE = List.of(
            late("compressor", ModBlocks.MULTIBLOCK_COMPRESSOR, "industrial"),
            late("blast_furnace", ModBlocks.MULTIBLOCK_BLAST_FURNACE, "industrial"),
            late("rolling_mill", ModBlocks.MULTIBLOCK_ROLLING_MILL, "industrial"),
            late("washer", ModBlocks.MULTIBLOCK_WASHER, "industrial"),
            late("fluid_enricher", ModBlocks.MULTIBLOCK_FLUID_ENRICHER, "industrial"),
            late("magnet", ModBlocks.MULTIBLOCK_MAGNET, "industrial"),
            late("crafter", ModBlocks.MULTIBLOCK_CRAFTER, "single"));
    /** Рецепты окна: из режима ядра; подпись режима — если у ядра их несколько. Считаются в registerCategories (по ним раскладка). */
    private final java.util.Map<Late, List<LateJeiCategory.Entry>> lateEntries = new java.util.HashMap<>();

    private static List<LateJeiCategory.Entry> lateEntries(Late l) {
        var level = Minecraft.getInstance().level;
        if (level == null || !(l.core().get() instanceof com.jykito.industrialcore.block.custom.LateMachineCoreBlock core)) return List.of();
        var modes = core.modes();
        return modes.stream().filter(m -> m.key().equals(l.mode()))
                .flatMap(m -> m.source().all(level).stream().map(r -> new LateJeiCategory.Entry(r, modes.size() < 2 ? null
                        : Component.translatable("gui.industrial_core.late_core.mode", Component.translatable("gui.industrial_core.late_core.mode." + m.key())))))
                .toList();
    }

    /** Нанолаборатория (Н3 «Лестница тем»): по рецепту на тему — откуда берутся карты данных. */
    public static final RecipeType<NaniteLabJeiCategory.Theme> NANITE_LAB =
            RecipeType.create(IndustrialCore.MODID, "nanite_lab", NaniteLabJeiCategory.Theme.class);

    /** Рецепты «только для лейт» малые машины не выполняют (matches → false): в их окнах их нет. */
    private static <T> List<T> regular(List<T> recipes, java.util.function.Predicate<T> lateOnly) {
        return recipes.stream().filter(r -> !lateOnly.test(r)).toList();
    }

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(IndustrialCore.MODID, "jei_plugin");
    }

    private static ResourceLocation guiTex(String name) {
        return new ResourceLocation(IndustrialCore.MODID, "textures/gui/" + name + ".png");
    }

    private static ResourceLocation jeiTex(String name) {
        return new ResourceLocation(IndustrialCore.MODID, "textures/gui/jei/" + name + ".png");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(
            VanillaTypes.ITEM_STACK,
            ModItems.UNIVERSAL_CAPSULE.get(),
            (stack, context) -> stack
                .getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .resolve()
                .map(h -> {
                    FluidStack f = h.getFluidInTank(0);
                    if (f.isEmpty()) return "empty";
                    var key = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(f.getFluid());
                    return key != null ? key.toString() : "unknown";
                })
                .orElse(IIngredientSubtypeInterpreter.NONE)
        );
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(

                new MachineCropCategory(gui, CRUSHING,
                        new ItemStack(ModBlocks.CRUSHER.get()),
                        Component.translatable("block.industrial_core.crusher"),
                        jeiTex("jei_crusher"), 154,140, 154,84, 44,30, 94,30, 44,64,66, 69,32,16,12, 66,99, 56,48),

                new MachineCropCategory(gui, COMPRESSING,
                        new ItemStack(ModBlocks.COMPRESSOR.get()),
                        Component.translatable("block.industrial_core.compressor"),
                        jeiTex("jei_compressor"), 154,140, 154,84, 44,30, 94,30, 44,64,66, 69,32,16,12, 60,93, 56,48),

                new MachineCropCategory(gui, ELECTRIC_SMELTING,
                        new ItemStack(ModBlocks.ELECTRIC_FURNACE.get()),
                        Component.translatable("block.industrial_core.electric_furnace"),
                        jeiTex("jei_electric_furnace"), 154,140, 154,84, 44,30, 94,30, 44,64,66, 70,32,16,12, 71,107, 56,48),

                new MachineCropCategory(gui, ROLLING,
                        new ItemStack(ModBlocks.METAL_FORMER.get()),
                        Component.translatable("jei.industrial_core.metal_former.rolling"),
                        jeiTex("jei_metal_former_rolling"), 154,140, 154,84, 41,30, 96,30, 45,64,66, 60,31,33,14, 62,111, 55,48),

                new MachineCropCategory(gui, EXTRUDING,
                        new ItemStack(ModBlocks.METAL_FORMER.get()),
                        Component.translatable("jei.industrial_core.metal_former.extruding"),
                        jeiTex("jei_metal_former_extruding"), 154,140, 154,84, 41,30, 96,30, 45,64,66, 60,31,33,14, 62,111, 55,48),

                new MachineCropCategory(gui, EXTRACTING,
                        new ItemStack(ModBlocks.EXTRACTOR.get()),
                        Component.translatable("block.industrial_core.extractor"),
                        jeiTex("jei_extractor"), 154,140, 154,84, 41,30, 96,30, 45,64,66, 65,32,23,13, 67,105, 55,48),

                new RestructorJeiCategory(gui, RESTRUCTOR,
                        new ItemStack(ModBlocks.RESTRUCTOR.get()),
                        Component.translatable("block.industrial_core.restructor")),

                new BlastFurnaceJeiCategory(gui, BLAST_FURNACE,
                        new ItemStack(ModBlocks.BLAST_FURNACE.get()),
                        Component.translatable("block.industrial_core.blast_furnace")),

                new RodFabricatorJeiCategory(gui, ROD_FABRICATOR,
                        new ItemStack(ModBlocks.ROD_FABRICATOR.get()),
                        Component.translatable("block.industrial_core.rod_fabricator")),

                new FluidEnricherJeiCategory(gui, FLUID_ENRICHER,
                        new ItemStack(ModBlocks.FLUID_ENRICHER.get()),
                        Component.translatable("block.industrial_core.fluid_enricher")),

                new OreWasherJeiCategory(gui, ORE_WASHING,
                        new ItemStack(ModBlocks.ORE_WASHER.get()),
                        Component.translatable("block.industrial_core.ore_washer")),

                new BioreactorJeiCategory(gui, BIOREACTOR,
                        new ItemStack(ModBlocks.BIOREACTOR.get()),
                        Component.translatable("block.industrial_core.bioreactor")),

                new LiquefierJeiCategory(gui, LIQUEFYING,
                        new ItemStack(ModBlocks.LIQUEFIER.get()),
                        Component.translatable("block.industrial_core.liquefier")),

                new CryoChamberJeiCategory(gui, CRYO_FREEZING,
                        new ItemStack(ModBlocks.CRYO_CORE.get()),
                        Component.translatable("block.industrial_core.cryo_core")),

                new CrystallizerJeiCategory(gui, CRYSTALLIZING,
                        new ItemStack(ModBlocks.CRYSTALLIZER.get()),
                        Component.translatable("block.industrial_core.crystallizer")),

                new PhaseSplitterRecipeCategory(gui, PHASE_SPLITTING,
                        new ItemStack(ModBlocks.PHASE_SPLITTER.get()),
                        Component.translatable("block.industrial_core.phase_splitter")),

                new MinerLensCategory(gui, MINER_LENS,
                        new ItemStack(ModBlocks.MINER_CORE.get()),
                        Component.translatable("jei.industrial_core.miner_lens")),

                new GrowingJeiCategory(gui, GROWING_PLANT,
                        new ItemStack(ModItems.CULTIVATOR_BIOSTIMULATOR.get()),
                        Component.translatable("jei.industrial_core.cultivator.plant_title"),
                        new ItemStack(ModItems.CULTIVATOR_BIOSTIMULATOR.get()), jeiTex("jei_cultivator_plant")),

                new GrowingJeiCategory(gui, GROWING_MINERAL,
                        new ItemStack(ModItems.CULTIVATOR_RESONATOR.get()),
                        Component.translatable("jei.industrial_core.cultivator.mineral_title"),
                        new ItemStack(ModItems.CULTIVATOR_RESONATOR.get()), jeiTex("jei_cultivator_mineral")),

                new StoneGenJeiCategory(gui, STONE_GEN,
                        new ItemStack(ModBlocks.STONE_GENERATOR.get()),
                        Component.translatable("block.industrial_core.stone_generator"), jeiTex("jei_stone_generator")),

                new FluidGenJeiCategory(gui, FLUID_GEN,
                        new ItemStack(ModBlocks.LAVA_GENERATOR.get()),
                        Component.translatable("jei.industrial_core.fluid_generator"), jeiTex("jei_fluid_generator")),

                new CapsuleFillerJeiCategory(gui, CAPSULE_FILLER,
                        new ItemStack(ModBlocks.CAPSULE_FILLER.get()),
                        Component.translatable("block.industrial_core.capsule_filler"), jeiTex("jei_capsule_filler")),

                new MagnetJeiCategory(gui, MAGNET,
                        new ItemStack(ModBlocks.ELECTROMAGNET_CORE.get()),
                        Component.translatable("block.industrial_core.electromagnet_core"))
        );
        registration.addRecipeCategories(new NaniteLabJeiCategory(gui, NANITE_LAB, new ItemStack(ModBlocks.NANITE_LAB.get()),
                Component.translatable("block.industrial_core.nanite_lab")));
        lateEntries.clear();
        for (Late l : LATE) {
            List<LateJeiCategory.Entry> entries = lateEntries(l);
            lateEntries.put(l, entries);
            var core = l.core().get();
            registration.addRecipeCategories(new LateJeiCategory(gui, l.type(), new ItemStack(core), Component.translatable(core.getDescriptionId()), entries));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var rm = Minecraft.getInstance().level.getRecipeManager();

        registration.addRecipes(CRUSHING,        regular(rm.getAllRecipesFor(ModRecipes.CRUSHING_TYPE.get()), MachineRecipe::isLateOnly));
        registration.addRecipes(COMPRESSING,     regular(rm.getAllRecipesFor(ModRecipes.COMPRESSING_TYPE.get()), MachineRecipe::isLateOnly));
        registration.addRecipes(ELECTRIC_SMELTING, regular(rm.getAllRecipesFor(ModRecipes.ELECTRIC_SMELTING_TYPE.get()), MachineRecipe::isLateOnly));
        registration.addRecipes(ROLLING,         regular(rm.getAllRecipesFor(ModRecipes.ROLLING_TYPE.get()), MachineRecipe::isLateOnly));
        registration.addRecipes(EXTRUDING,        regular(rm.getAllRecipesFor(ModRecipes.EXTRUDING_TYPE.get()), MachineRecipe::isLateOnly));
        registration.addRecipes(EXTRACTING,      regular(rm.getAllRecipesFor(ModRecipes.EXTRACTING_TYPE.get()), MachineRecipe::isLateOnly));

        registration.addRecipes(RESTRUCTOR, rm.getAllRecipesFor(ModRecipes.MOLECULAR_TRANSFORMING_TYPE.get()));

        registration.addRecipes(BLAST_FURNACE,  regular(rm.getAllRecipesFor(ModRecipes.BLAST_FURNACE_TYPE.get()), BlastFurnaceRecipe::isLateOnly));
        registration.addRecipes(ROD_FABRICATOR, rm.getAllRecipesFor(ModRecipes.ROD_FABRICATION_TYPE.get()));
        registration.addRecipes(FLUID_ENRICHER, regular(rm.getAllRecipesFor(ModRecipes.FLUID_ENRICHER_TYPE.get()), FluidEnricherRecipe::isLateOnly));
        registration.addRecipes(ORE_WASHING,    regular(rm.getAllRecipesFor(ModRecipes.ORE_WASHING_TYPE.get()), OreWashingRecipe::isLateOnly));
        registration.addRecipes(BIOREACTOR,     rm.getAllRecipesFor(ModRecipes.BIOREACTOR_TYPE.get()));

        registration.addRecipes(LIQUEFYING, rm.getAllRecipesFor(ModRecipes.LIQUEFYING_TYPE.get()));
        registration.addRecipes(CRYSTALLIZING, rm.getAllRecipesFor(ModRecipes.CRYSTALLIZING_TYPE.get()));
        registration.addRecipes(CRYO_FREEZING, rm.getAllRecipesFor(ModRecipes.CRYO_FREEZING_TYPE.get()));

        registration.addRecipes(PHASE_SPLITTING, rm.getAllRecipesFor(ModRecipes.PHASE_SPLITTING_TYPE.get()));

        java.util.List<MinerLensRecipe> lensRecipes = new java.util.ArrayList<>();
        net.minecraft.world.item.Item[] lensItems = {
                ModItems.MINER_LENS_1.get(), ModItems.MINER_LENS_2.get(), ModItems.MINER_LENS_3.get(), ModItems.MINER_LENS_4.get() };
        com.jykito.industrialcore.item.custom.MinerLensItem.LensType[] lensTypes = {
                com.jykito.industrialcore.item.custom.MinerLensItem.LensType.OVERWORLD,
                com.jykito.industrialcore.item.custom.MinerLensItem.LensType.NETHER,
                com.jykito.industrialcore.item.custom.MinerLensItem.LensType.END,
                com.jykito.industrialcore.item.custom.MinerLensItem.LensType.ALL };
        final int PER_PAGE = 28;
        for (int li = 0; li < lensItems.length; li++) {
            ItemStack lensStack = new ItemStack(lensItems[li]);
            var ores = com.jykito.industrialcore.item.custom.MinerLensItem.getOresForType(lensTypes[li]);
            var weights = com.jykito.industrialcore.item.custom.MinerLensItem.getWeightsForType(lensTypes[li]);
            int total = 0; for (int w : weights) total += w;
            if (ores.isEmpty()) { lensRecipes.add(new MinerLensRecipe(lensStack, ores, weights, total)); continue; }
            for (int i = 0; i < ores.size(); i += PER_PAGE) {
                int end = Math.min(i + PER_PAGE, ores.size());
                lensRecipes.add(new MinerLensRecipe(lensStack, ores.subList(i, end), weights.subList(i, end), total));
            }
        }
        registration.addRecipes(MINER_LENS, lensRecipes);

        java.util.List<GrowingRecipe> growingAll = new java.util.ArrayList<>(rm.getAllRecipesFor(ModRecipes.GROWING_TYPE.get()));
        var plantsTag = net.minecraftforge.registries.ForgeRegistries.ITEMS.tags()
                .getTag(com.jykito.industrialcore.block.entity.CultivatorBlockEntity.GROWABLE_PLANTS);
        int plantIdx = 0;
        for (net.minecraft.world.item.Item plant : plantsTag) {
            ItemStack ps = new ItemStack(plant);
            growingAll.add(new GrowingRecipe(
                    new ResourceLocation(IndustrialCore.MODID, "jei_plant_dup_" + (plantIdx++)),
                    net.minecraft.world.item.crafting.Ingredient.of(ps), 1,
                    ps.copy(), 100, 200, GrowingRecipe.Category.PLANT, true));
        }
        java.util.List<GrowingRecipe> plantRecipes = new java.util.ArrayList<>();
        java.util.List<GrowingRecipe> mineralRecipes = new java.util.ArrayList<>();
        for (GrowingRecipe gr : growingAll) {
            if (gr.getCategory() == GrowingRecipe.Category.PLANT) plantRecipes.add(gr);
            else mineralRecipes.add(gr);
        }
        registration.addRecipes(GROWING_PLANT, plantRecipes);
        registration.addRecipes(GROWING_MINERAL, mineralRecipes);

        registration.addRecipes(STONE_GEN, Arrays.asList(
                com.jykito.industrialcore.block.entity.StoneGeneratorMode.values()));

        registration.addRecipes(FLUID_GEN, Arrays.asList(
                com.jykito.industrialcore.block.entity.FluidGeneratorType.values()));

        java.util.List<CapsuleFillerJeiRecipe> capRecipes = new java.util.ArrayList<>();
        for (net.minecraft.world.level.material.Fluid f : net.minecraftforge.registries.ForgeRegistries.FLUIDS) {
            if (f == net.minecraft.world.level.material.Fluids.EMPTY) continue;
            if (!f.isSource(f.defaultFluidState())) continue;
            FluidStack fs = new FluidStack(f, 1000);
            ItemStack filled = new ItemStack(ModItems.UNIVERSAL_CAPSULE.get());
            filled.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                  .ifPresent(h -> h.fill(fs.copy(), IFluidHandlerItem.FluidAction.EXECUTE));

            if (filled.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                    .map(h -> !h.getFluidInTank(0).isEmpty()).orElse(false)) {
                capRecipes.add(new CapsuleFillerJeiRecipe(
                        new ItemStack(ModItems.UNIVERSAL_CAPSULE.get()), fs, filled));
            }
        }
        registration.addRecipes(CAPSULE_FILLER, capRecipes);

        registration.addRecipes(MAGNET, regular(rm.getAllRecipesFor(ModRecipes.ACCELERATING_TYPE.get()), AcceleratorRecipe::isLateOnly));
        for (Late l : LATE) registration.addRecipes(l.type(), lateEntries.getOrDefault(l, List.of()));
        registration.addRecipes(NANITE_LAB, NaniteLabJeiCategory.themes());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(HubScreen.class, new HubGhostHandler());
        registration.addGhostIngredientHandler(MePortScreen.class, new MePortGhostHandler());

        recipeArea(registration, com.jykito.industrialcore.screen.CrusherScreen.class,         117,61,16,12, CRUSHING);
        recipeArea(registration, com.jykito.industrialcore.screen.CompressorScreen.class,      116,61,16,12, COMPRESSING);
        recipeArea(registration, com.jykito.industrialcore.screen.ElectricFurnaceScreen.class, 118,61,16,12, mezz.jei.api.constants.RecipeTypes.SMELTING);

        recipeArea(registration, com.jykito.industrialcore.screen.MetalFormerScreen.class,     107,60,14,14, ROLLING, EXTRUDING);
        recipeArea(registration, com.jykito.industrialcore.screen.MetalFormerScreen.class,     129,60,11,14, ROLLING, EXTRUDING);
        recipeArea(registration, com.jykito.industrialcore.screen.ExtractorScreen.class,       112,60,23,13, EXTRACTING);
        recipeArea(registration, com.jykito.industrialcore.screen.RestructorScreen.class,      103,77,50,5, RESTRUCTOR);
        // T2: жила от катализатора (на зарядах свои подсказки)
        recipeArea(registration, com.jykito.industrialcore.screen.RestructorT2Screen.class,    78,43,19,50, RESTRUCTOR);
        recipeArea(registration, com.jykito.industrialcore.screen.BlastFurnaceScreen.class,    110,49,28,20, BLAST_FURNACE);
        recipeArea(registration, com.jykito.industrialcore.screen.RodFabricatorScreen.class,   108,51,29,25, ROD_FABRICATOR);

        recipeArea(registration, com.jykito.industrialcore.screen.FluidEnricherScreen.class,   103,42,46,26, FLUID_ENRICHER);
        recipeArea(registration, com.jykito.industrialcore.screen.OreWasherScreen.class,       118,56,16,12, ORE_WASHING);
        recipeArea(registration, com.jykito.industrialcore.screen.BioreactorScreen.class,      118,56,16,12, BIOREACTOR);
        recipeArea(registration, com.jykito.industrialcore.screen.LiquefierScreen.class,       105,47,33,28, LIQUEFYING);
        recipeArea(registration, com.jykito.industrialcore.screen.CrystallizerScreen.class,    115,60,23,16, CRYSTALLIZING);
        recipeArea(registration, com.jykito.industrialcore.screen.CryoChamberScreen.class,     112,37,32,12, CRYO_FREEZING);
        recipeArea(registration, com.jykito.industrialcore.screen.PhaseSplitterScreen.class,   108,45,32,46, PHASE_SPLITTING);
        recipeArea(registration, com.jykito.industrialcore.screen.StoneGeneratorScreen.class,  125,66,6,9,   STONE_GEN);
        recipeArea(registration, com.jykito.industrialcore.screen.FluidGeneratorScreen.class,  103,89,50,6, FLUID_GEN);
        recipeArea(registration, com.jykito.industrialcore.screen.CapsuleFillerScreen.class,   127,58,8,13,  CAPSULE_FILLER);
        recipeArea(registration, com.jykito.industrialcore.screen.CultivatorScreen.class,      120,62,22,8,  GROWING_PLANT, GROWING_MINERAL);
        for (int lane = 0; lane < com.jykito.industrialcore.block.entity.CultivatorBlockEntity.T2_LANES; lane++)
            recipeArea(registration, com.jykito.industrialcore.screen.CultivatorT2Screen.class,
                    com.jykito.industrialcore.screen.CultivatorT2Screen.PROG_X, com.jykito.industrialcore.screen.CultivatorT2Screen.PROG_Y + com.jykito.industrialcore.screen.CultivatorT2Screen.PROG_STEP * lane, 22, 8,
                    GROWING_PLANT, GROWING_MINERAL);

        recipeArea(registration, com.jykito.industrialcore.screen.MinerCoreScreen.class,       111,53,30,6,  MINER_LENS);
        recipeArea(registration, com.jykito.industrialcore.screen.MinerCoreScreen.class,       111,75,30,12, MINER_LENS);

        // выход в (120,52); клик-зоны — в зазорах вокруг него (крест), чтобы хинт JEI не налезал на тултип слота
        recipeArea(registration, com.jykito.industrialcore.screen.ElectromagnetScreen.class,   120,39,16,12, MAGNET);
        recipeArea(registration, com.jykito.industrialcore.screen.ElectromagnetScreen.class,   120,69,16,12, MAGNET);
        recipeArea(registration, com.jykito.industrialcore.screen.ElectromagnetScreen.class,   101,53,18,14, MAGNET);
        recipeArea(registration, com.jykito.industrialcore.screen.ElectromagnetScreen.class,   137,53,18,14, MAGNET);
    }

    private void recipeArea(IGuiHandlerRegistration reg, Class<? extends AbstractContainerScreen<?>> cls,
                            int x, int y, int w, int h, RecipeType<?>... types) {
        reg.addRecipeClickArea(cls, x, y, w, h, types);
    }

    /** Клетки настройки МЭ-входа: предмет из JEI задаёт полную стопку, жидкость — ведро. */
    private static class MePortGhostHandler implements IGhostIngredientHandler<MePortScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(MePortScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            boolean fluid = ingredient.getType() == ForgeTypes.FLUID_STACK;
            if (!fluid && ingredient.getType() != VanillaTypes.ITEM_STACK) return List.of();
            int containerId = gui.getMenu().containerId;
            List<Target<I>> targets = new ArrayList<>();
            for (MePortScreen.GhostTarget t : gui.ghostTargets(fluid))
                targets.add(new Target<>() {
                    @Override public Rect2i getArea() { return t.area(); }
                    @Override public void accept(I value) {
                        if (value instanceof FluidStack f)
                            ModMessages.sendToServer(MePortPacket.ghost(containerId, t.index(), ItemStack.EMPTY, f));
                        else if (value instanceof ItemStack stack)
                            ModMessages.sendToServer(MePortPacket.ghost(containerId, t.index(), stack, FluidStack.EMPTY));
                    }
                });
            return targets;
        }
        @Override public void onComplete() {}
    }

    private static class HubGhostHandler implements IGhostIngredientHandler<HubScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(HubScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            if (ingredient.getType() != VanillaTypes.ITEM_STACK) return List.of();
            int face = gui.getSelectedFace();
            if (face < 0 || gui.getMenu().be == null) return List.of();
            final net.minecraft.core.BlockPos pos = gui.getMenu().be.getBlockPos();
            final boolean fluid = gui.getMenu().be.fluid;
            List<Target<I>> targets = new ArrayList<>();
            for (int s = 0; s < com.jykito.industrialcore.block.entity.HubBlockEntity.FILTER_SLOTS; s++) {
                final int slot = s;
                int x = gui.getGuiLeft() + HubScreen.FGRID_X + (s % 3) * HubScreen.FCELL;
                int y = gui.getGuiTop()  + HubScreen.FGRID_Y + (s / 3) * HubScreen.FCELL;
                targets.add(new Target<>() {
                    @Override public Rect2i getArea() { return new Rect2i(x, y, 16, 16); }
                    @Override public void accept(I value) {
                        if (!(value instanceof ItemStack stack)) return;
                        boolean out = gui.getMenu().filterDir == 1;
                        if (fluid) {
                            var cap = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
                            if (cap.isEmpty()) return;
                            IFluidHandlerItem handler = cap.get();
                            if (handler.getTanks() == 0) return;
                            FluidStack f = handler.getFluidInTank(0);
                            if (!f.isEmpty())
                                ModMessages.sendToServer(new com.jykito.industrialcore.networking.HubFluidFilterPacket(
                                        pos, face, slot, out, f));
                        } else {
                            ModMessages.sendToServer(new com.jykito.industrialcore.networking.HubFilterPacket(
                                    pos, face, slot, stack, out));
                        }
                    }
                });
            }
            return targets;
        }
        @Override public void onComplete() {}
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.RESTRUCTOR.get()), RESTRUCTOR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.RESTRUCTOR_T2.get()), RESTRUCTOR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUSHER.get()),        CRUSHING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MULTIBLOCK_MACERATOR.get()), CRUSHING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.COMPRESSOR.get()),     COMPRESSING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ELECTRIC_FURNACE.get()), mezz.jei.api.constants.RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MULTIBLOCK_FURNACE.get()), mezz.jei.api.constants.RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.METAL_FORMER.get()),   ROLLING, EXTRUDING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MULTIBLOCK_ROLLING_MILL.get()), ROLLING, EXTRUDING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MULTIBLOCK_COMPRESSOR.get()), COMPRESSING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MULTIBLOCK_LIQUEFIER.get()), LIQUEFYING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.BLAST_FURNACE.get()),   BLAST_FURNACE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MULTIBLOCK_BLAST_FURNACE.get()), BLAST_FURNACE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROD_FABRICATOR.get()),  ROD_FABRICATOR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EXTRACTOR.get()),        EXTRACTING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FLUID_ENRICHER.get()),   FLUID_ENRICHER);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MULTIBLOCK_FLUID_ENRICHER.get()), FLUID_ENRICHER);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ORE_WASHER.get()),        ORE_WASHING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.BIOREACTOR.get()),        BIOREACTOR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MULTIBLOCK_WASHER.get()), ORE_WASHING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.LIQUEFIER.get()),          LIQUEFYING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRYSTALLIZER.get()),      CRYSTALLIZING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRYO_CORE.get()),         CRYO_FREEZING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PHASE_SPLITTER.get()),   PHASE_SPLITTING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MINER_CORE.get()), MINER_LENS);
        registration.addRecipeCatalyst(new ItemStack(ModItems.MINER_LENS_1.get()), MINER_LENS);
        registration.addRecipeCatalyst(new ItemStack(ModItems.MINER_LENS_2.get()), MINER_LENS);
        registration.addRecipeCatalyst(new ItemStack(ModItems.MINER_LENS_3.get()), MINER_LENS);
        registration.addRecipeCatalyst(new ItemStack(ModItems.MINER_LENS_4.get()), MINER_LENS);

        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CULTIVATOR.get()),       GROWING_PLANT, GROWING_MINERAL);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CULTIVATOR_T2.get()),    GROWING_PLANT, GROWING_MINERAL);
        registration.addRecipeCatalyst(new ItemStack(ModItems.CULTIVATOR_BIOSTIMULATOR.get()), GROWING_PLANT);
        registration.addRecipeCatalyst(new ItemStack(ModItems.CULTIVATOR_RESONATOR.get()),     GROWING_MINERAL);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.STONE_GENERATOR.get()),   STONE_GEN);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.LAVA_GENERATOR.get()),    FLUID_GEN);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.WATER_GENERATOR.get()),   FLUID_GEN);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CAPSULE_FILLER.get()),    CAPSULE_FILLER);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ELECTROMAGNET_CORE.get()), MAGNET);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MULTIBLOCK_MAGNET.get()), MAGNET);
        for (Late l : LATE) registration.addRecipeCatalyst(new ItemStack(l.core().get()), l.type());
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.NANITE_LAB.get()), NANITE_LAB);
    }
}
