package com.jykito.industrialcore.jei;

import com.jykito.industrialcore.IndustrialCore;
import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.networking.ConnectorFilterFluidPacket;
import com.jykito.industrialcore.networking.ConnectorFilterItemPacket;
import com.jykito.industrialcore.networking.ModMessages;
import com.jykito.industrialcore.recipe.*;
import net.minecraft.world.item.Items;
import com.jykito.industrialcore.screen.FluidConnectorScreen;
import com.jykito.industrialcore.screen.ItemConnectorScreen;
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
    public static final RecipeType<OreWashingRecipe> ORE_WASHING =
            RecipeType.create(IndustrialCore.MODID, "ore_washer_jei", OreWashingRecipe.class);

    public static final RecipeType<LiquefierRecipe> LIQUEFYING =
            RecipeType.create(IndustrialCore.MODID, "liquefying", LiquefierRecipe.class);

    public static final RecipeType<PhaseSplitterRecipe> PHASE_SPLITTING =
            RecipeType.create(IndustrialCore.MODID, "phase_splitting", PhaseSplitterRecipe.class);

    public static final RecipeType<MinerLensRecipe> MINER_LENS =
            RecipeType.create(IndustrialCore.MODID, "miner_lens", MinerLensRecipe.class);

    public static final RecipeType<ReactorSchemeRecipe> REACTOR_SCHEME =
            RecipeType.create(IndustrialCore.MODID, "reactor_scheme", ReactorSchemeRecipe.class);

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
                        jeiTex("jei_electric_furnace"), 154,140, 154,84, 44,30, 94,30, 44,64,66, 65,33,24,10, 69,102, 56,48),

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

                new LiquefierJeiCategory(gui, LIQUEFYING,
                        new ItemStack(ModBlocks.LIQUEFIER.get()),
                        Component.translatable("block.industrial_core.liquefier")),

                new PhaseSplitterRecipeCategory(gui, PHASE_SPLITTING,
                        new ItemStack(ModBlocks.PHASE_SPLITTER.get()),
                        Component.translatable("block.industrial_core.phase_splitter")),

                new MinerLensCategory(gui, MINER_LENS,
                        new ItemStack(ModBlocks.MINER_CORE.get()),
                        Component.translatable("jei.industrial_core.miner_lens")),

new ReactorSchemeCategory(gui, REACTOR_SCHEME,
                        new ItemStack(ModBlocks.NUCLEAR_REACTOR.get()),
                        Component.translatable("jei.industrial_core.reactor_scheme")),

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
                        Component.translatable("block.industrial_core.capsule_filler"), jeiTex("jei_capsule_filler"))
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var rm = Minecraft.getInstance().level.getRecipeManager();

        registration.addRecipes(CRUSHING,        rm.getAllRecipesFor(ModRecipes.CRUSHING_TYPE.get()));
        registration.addRecipes(COMPRESSING,     rm.getAllRecipesFor(ModRecipes.COMPRESSING_TYPE.get()));
        registration.addRecipes(ELECTRIC_SMELTING, rm.getAllRecipesFor(ModRecipes.ELECTRIC_SMELTING_TYPE.get()));
        registration.addRecipes(ROLLING,         rm.getAllRecipesFor(ModRecipes.ROLLING_TYPE.get()));
        registration.addRecipes(EXTRUDING,        rm.getAllRecipesFor(ModRecipes.EXTRUDING_TYPE.get()));
        registration.addRecipes(EXTRACTING,      rm.getAllRecipesFor(ModRecipes.EXTRACTING_TYPE.get()));

        registration.addRecipes(RESTRUCTOR, rm.getAllRecipesFor(ModRecipes.MOLECULAR_TRANSFORMING_TYPE.get()));

        registration.addRecipes(BLAST_FURNACE,  rm.getAllRecipesFor(ModRecipes.BLAST_FURNACE_TYPE.get()));
        registration.addRecipes(ROD_FABRICATOR, rm.getAllRecipesFor(ModRecipes.ROD_FABRICATION_TYPE.get()));
        registration.addRecipes(FLUID_ENRICHER, rm.getAllRecipesFor(ModRecipes.FLUID_ENRICHER_TYPE.get()));
        registration.addRecipes(ORE_WASHING,    rm.getAllRecipesFor(ModRecipes.ORE_WASHING_TYPE.get()));

        registration.addRecipes(LIQUEFYING, rm.getAllRecipesFor(ModRecipes.LIQUEFYING_TYPE.get()));

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

        java.util.List<ReactorSchemeRecipe> schemeRecipes = new java.util.ArrayList<>();
        for (com.jykito.industrialcore.reactor.ReactorSchemes.Scheme s
                : com.jykito.industrialcore.reactor.ReactorSchemes.SCHEMES) {
            schemeRecipes.add(new ReactorSchemeRecipe(s));
        }
        registration.addRecipes(REACTOR_SCHEME, schemeRecipes);

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
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(ItemConnectorScreen.class, new ItemConnectorGhostHandler());
        registration.addGhostIngredientHandler(FluidConnectorScreen.class, new FluidConnectorGhostHandler());

        recipeArea(registration, com.jykito.industrialcore.screen.CrusherScreen.class,         117,61,16,12, CRUSHING);
        recipeArea(registration, com.jykito.industrialcore.screen.CompressorScreen.class,      116,61,16,12, COMPRESSING);
        recipeArea(registration, com.jykito.industrialcore.screen.ElectricFurnaceScreen.class, 112,62,24,10, ELECTRIC_SMELTING);

        recipeArea(registration, com.jykito.industrialcore.screen.MetalFormerScreen.class,     107,60,14,14, ROLLING, EXTRUDING);
        recipeArea(registration, com.jykito.industrialcore.screen.MetalFormerScreen.class,     129,60,11,14, ROLLING, EXTRUDING);
        recipeArea(registration, com.jykito.industrialcore.screen.ExtractorScreen.class,       112,60,23,13, EXTRACTING);
        recipeArea(registration, com.jykito.industrialcore.screen.RestructorScreen.class,      103,77,50,5, RESTRUCTOR);
        recipeArea(registration, com.jykito.industrialcore.screen.BlastFurnaceScreen.class,    110,49,28,20, BLAST_FURNACE);
        recipeArea(registration, com.jykito.industrialcore.screen.RodFabricatorScreen.class,   108,51,29,25, ROD_FABRICATOR);

        recipeArea(registration, com.jykito.industrialcore.screen.FluidEnricherScreen.class,   79,42,39,26,  FLUID_ENRICHER);
        recipeArea(registration, com.jykito.industrialcore.screen.FluidEnricherScreen.class,   134,42,37,26, FLUID_ENRICHER);
        recipeArea(registration, com.jykito.industrialcore.screen.OreWasherScreen.class,       111,54,27,14, ORE_WASHING);
        recipeArea(registration, com.jykito.industrialcore.screen.LiquefierScreen.class,       105,47,33,28, LIQUEFYING);
        recipeArea(registration, com.jykito.industrialcore.screen.PhaseSplitterScreen.class,   108,45,32,46, PHASE_SPLITTING);
        recipeArea(registration, com.jykito.industrialcore.screen.StoneGeneratorScreen.class,  125,66,6,9,   STONE_GEN);
        recipeArea(registration, com.jykito.industrialcore.screen.FluidGeneratorScreen.class,  103,89,50,6, FLUID_GEN);
        recipeArea(registration, com.jykito.industrialcore.screen.CapsuleFillerScreen.class,   127,58,8,13,  CAPSULE_FILLER);
        recipeArea(registration, com.jykito.industrialcore.screen.CultivatorScreen.class,      120,62,22,8,  GROWING_PLANT, GROWING_MINERAL);

        recipeArea(registration, com.jykito.industrialcore.screen.MinerCoreScreen.class,       111,53,30,6,  MINER_LENS);
        recipeArea(registration, com.jykito.industrialcore.screen.MinerCoreScreen.class,       111,75,30,12, MINER_LENS);
    }

    private void recipeArea(IGuiHandlerRegistration reg, Class<? extends AbstractContainerScreen<?>> cls,
                            int x, int y, int w, int h, RecipeType<?>... types) {
        reg.addRecipeClickArea(cls, x, y, w, h, types);
    }

    private static class ItemConnectorGhostHandler implements IGhostIngredientHandler<ItemConnectorScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(ItemConnectorScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            if (ingredient.getType() != VanillaTypes.ITEM_STACK) return List.of();
            List<Target<I>> targets = new ArrayList<>();
            for (int s = 0; s < com.jykito.industrialcore.menu.ItemConnectorMenu.FILTER_SLOTS; s++) {
                final int slot = s;
                int col = s % ItemConnectorScreen.FILTER_COLS;
                int row = s / ItemConnectorScreen.FILTER_COLS;
                int x = gui.getGuiLeft() + ItemConnectorScreen.FILTER_COL_START + col * ItemConnectorScreen.FILTER_GAP;
                int y = gui.getGuiTop()  + ItemConnectorScreen.FILTER_ROW_START + row * ItemConnectorScreen.FILTER_GAP;
                targets.add(new Target<>() {
                    @Override public Rect2i getArea() { return new Rect2i(x, y, 16, 16); }
                    @Override public void accept(I value) {
                        if (value instanceof ItemStack stack)
                            ModMessages.sendToServer(new ConnectorFilterItemPacket(
                                    gui.getMenu().blockEntity.getBlockPos(), slot, stack));
                    }
                });
            }
            return targets;
        }
        @Override public void onComplete() {}
    }

    private static class FluidConnectorGhostHandler implements IGhostIngredientHandler<FluidConnectorScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(FluidConnectorScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
            if (ingredient.getType() != VanillaTypes.ITEM_STACK) return List.of();
            List<Target<I>> targets = new ArrayList<>();
            for (int s = 0; s < FluidConnectorScreen.FILTER_SLOTS; s++) {
                final int slot = s;
                int x = gui.getGuiLeft() + FluidConnectorScreen.FILTER_COL_START + s * FluidConnectorScreen.FILTER_GAP;
                int y = gui.getGuiTop()  + FluidConnectorScreen.FILTER_ROW_START;
                targets.add(new Target<>() {
                    @Override public Rect2i getArea() { return new Rect2i(x, y, 16, 16); }
                    @Override public void accept(I value) {
                        if (value instanceof ItemStack stack) {
                            var cap = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
                            if (cap.isEmpty()) return;
                            IFluidHandlerItem handler = cap.get();
                            if (handler.getTanks() == 0) return;
                            FluidStack fluid = handler.getFluidInTank(0);
                            if (!fluid.isEmpty())
                                ModMessages.sendToServer(new ConnectorFilterFluidPacket(
                                        gui.getMenu().blockEntity.getBlockPos(), slot, fluid));
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
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUSHER.get()),        CRUSHING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.COMPRESSOR.get()),     COMPRESSING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ELECTRIC_FURNACE.get()), ELECTRIC_SMELTING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.METAL_FORMER.get()),   ROLLING, EXTRUDING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.BLAST_FURNACE.get()),   BLAST_FURNACE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROD_FABRICATOR.get()),  ROD_FABRICATOR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EXTRACTOR.get()),        EXTRACTING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FLUID_ENRICHER.get()),   FLUID_ENRICHER);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ORE_WASHER.get()),        ORE_WASHING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.LIQUEFIER.get()),          LIQUEFYING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PHASE_SPLITTER.get()),   PHASE_SPLITTING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MINER_CORE.get()), MINER_LENS);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.NUCLEAR_REACTOR.get()), REACTOR_SCHEME);
        registration.addRecipeCatalyst(new ItemStack(ModItems.MINER_LENS_1.get()), MINER_LENS);
        registration.addRecipeCatalyst(new ItemStack(ModItems.MINER_LENS_2.get()), MINER_LENS);
        registration.addRecipeCatalyst(new ItemStack(ModItems.MINER_LENS_3.get()), MINER_LENS);
        registration.addRecipeCatalyst(new ItemStack(ModItems.MINER_LENS_4.get()), MINER_LENS);

        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CULTIVATOR.get()),       GROWING_PLANT, GROWING_MINERAL);
        registration.addRecipeCatalyst(new ItemStack(ModItems.CULTIVATOR_BIOSTIMULATOR.get()), GROWING_PLANT);
        registration.addRecipeCatalyst(new ItemStack(ModItems.CULTIVATOR_RESONATOR.get()),     GROWING_MINERAL);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.STONE_GENERATOR.get()),   STONE_GEN);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.LAVA_GENERATOR.get()),    FLUID_GEN);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.WATER_GENERATOR.get()),   FLUID_GEN);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CAPSULE_FILLER.get()),    CAPSULE_FILLER);
    }
}
