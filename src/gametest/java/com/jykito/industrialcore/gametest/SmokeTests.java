package com.jykito.industrialcore.gametest;

import com.jykito.industrialcore.IndustrialCore;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Dev-only smoke test of the port (src/gametest, not in the jar). Run: gradlew runGameTestServer.
 * The gametest server loads every data file of the mod; this test reports what did not make it:
 * recipes that failed to parse, block loot tables that are missing or broken, advancements that did not load.
 * Details of each failure are in the server log right above the "[smoke]" lines.
 */
@GameTestHolder(IndustrialCore.MODID)
@PrefixGameTestTemplate(false)
public class SmokeTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    @GameTest(template = "empty")
    public static void dataLoaded(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        List<String> problems = new ArrayList<>();

        // recipes: every unconditional json of ours must have loaded
        Map<String, Integer> byType = new TreeMap<>();
        Set<ResourceLocation> loaded = new HashSet<>();
        for (RecipeHolder<?> h : server.getRecipeManager().getRecipes()) {
            if (!h.id().getNamespace().equals(IndustrialCore.MODID)) continue;
            loaded.add(h.id());
            byType.merge(String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(h.value().getType())), 1, Integer::sum);
        }
        int files = 0, conditional = 0;
        for (Map.Entry<ResourceLocation, Resource> e : server.getResourceManager()
                .listResources("recipe", rl -> rl.getNamespace().equals(IndustrialCore.MODID) && rl.getPath().endsWith(".json")).entrySet()) {
            files++;
            String p = e.getKey().getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(IndustrialCore.MODID, p.substring("recipe/".length(), p.length() - ".json".length()));
            if (loaded.contains(id)) continue;
            if (read(e.getValue()).contains("neoforge:conditions")) { conditional++; continue; }
            problems.add("recipe not loaded: " + id);
        }
        LOGGER.info("[smoke] recipes: {} loaded of {} files ({} skipped by conditions); by type {}", loaded.size(), files, conditional, byType);

        // block loot tables: every block of ours with a table must resolve to a real one
        int blocks = 0, tables = 0;
        for (Block b : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals(IndustrialCore.MODID)) continue;
            blocks++;
            var key = b.getLootTable();
            if (key == BuiltInLootTables.EMPTY) continue;
            if (server.reloadableRegistries().getLootTable(key) == LootTable.EMPTY) problems.add("loot table missing or broken: " + key.location());
            else tables++;
        }
        LOGGER.info("[smoke] loot: {} of {} blocks have a table", tables, blocks);

        // advancements
        int adv = 0;
        for (var h : server.getAdvancements().getAllAdvancements()) if (h.id().getNamespace().equals(IndustrialCore.MODID)) adv++;
        int advFiles = server.getResourceManager()
                .listResources("advancement", rl -> rl.getNamespace().equals(IndustrialCore.MODID) && rl.getPath().endsWith(".json")).size();
        if (adv != advFiles) problems.add("advancements: " + adv + " loaded of " + advFiles + " files");
        LOGGER.info("[smoke] advancements: {} loaded of {} files", adv, advFiles);

        problems.forEach(p -> LOGGER.error("[smoke] {}", p));
        LOGGER.info("[smoke] {} problem(s)", problems.size());
        if (problems.isEmpty()) helper.succeed();
        else helper.fail(problems.size() + " problem(s), see [smoke] lines in the log");
    }

    /** A barrel taken off with the wrench keeps its fluid: block -> item (as the wrench builds it) -> placed again. */
    @GameTest(template = "empty")
    public static void barrelKeepsFluid(GameTestHelper helper) {
        var block = com.jykito.industrialcore.block.ModBlocks.BARREL_T1.get();
        net.minecraft.core.BlockPos a = new net.minecraft.core.BlockPos(0, 1, 0), b = new net.minecraft.core.BlockPos(0, 2, 0);
        helper.setBlock(a, block);
        helper.setBlock(b, block);
        var level = helper.getLevel();
        var tank = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, helper.absolutePos(a), null);
        if (tank == null) { helper.fail("barrel exposes no fluid handler"); return; }
        tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 5000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

        net.minecraft.world.item.ItemStack drop = new net.minecraft.world.item.ItemStack(block);   // what ElectricWrenchItem drops
        drop.applyComponents(level.getBlockEntity(helper.absolutePos(a)).collectComponents());
        var content = drop.get(com.jykito.industrialcore.item.ModDataComponents.FLUID_CONTENT.get());
        if (content == null || content.copy().getAmount() != 5000) { helper.fail("dropped barrel lost its fluid: " + content); return; }

        level.getBlockEntity(helper.absolutePos(b)).applyComponentsFromItemStack(drop);              // placing it again
        var placed = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, helper.absolutePos(b), null);
        var inside = placed == null ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : placed.getFluidInTank(0);
        if (!inside.is(net.minecraft.world.level.material.Fluids.WATER) || inside.getAmount() != 5000) {
            helper.fail("placed barrel holds " + inside.getAmount() + " mB instead of 5000 water"); return;
        }
        LOGGER.info("[smoke] barrel keeps 5000 mB through the wrench drop and placement");
        helper.succeed();
    }

    private static String read(Resource r) {
        try (Reader in = r.openAsReader()) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            for (int n; (n = in.read(buf)) > 0; ) sb.append(buf, 0, n);
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }
}
