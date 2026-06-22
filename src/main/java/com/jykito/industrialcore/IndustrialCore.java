package com.jykito.industrialcore;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.compat.DynamicLightsCompat;
import com.jykito.industrialcore.block.entity.BaseMachineBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.item.ModItems;
import com.jykito.industrialcore.menu.ModMenuTypes;
import com.jykito.industrialcore.networking.ModMessages;
import com.jykito.industrialcore.recipe.ModRecipes;
import com.jykito.industrialcore.worldgen.ModFeatures;
import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(IndustrialCore.MODID)
public class IndustrialCore {

    public static final String MODID = "industrial_core";
    private static final Logger LOGGER = LogUtils.getLogger();

    public IndustrialCore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModFluids.register(modEventBus);
        com.jykito.industrialcore.sound.ModSounds.register(modEventBus);
        com.jykito.industrialcore.entity.ModEntities.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new com.jykito.industrialcore.item.custom.NexiteArmorEventHandler());
        MinecraftForge.EVENT_BUS.register(new com.jykito.industrialcore.item.custom.PlasmaArmorEventHandler());
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModMessages::register);
        event.enqueueWork(DynamicLightsCompat::init);
    }

@SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {}

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        com.jykito.industrialcore.item.custom.MinerLensItem.clearCache();
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(@NotNull ResourceManager rm, @NotNull ProfilerFiller profiler) { return null; }
            @Override
            protected void apply(Void obj, @NotNull ResourceManager rm, @NotNull ProfilerFiller profiler) {
                BaseMachineBlockEntity.markAllRecipesDirty();
                com.jykito.industrialcore.recipe.CachedRecipe.markAllDirty();
            }
        });
    }
}
