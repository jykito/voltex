package com.jykito.industrialcore;

import com.jykito.industrialcore.block.ModBlocks;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.ModContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(IndustrialCore.MODID)
public class IndustrialCore {

    public static final String MODID = "industrial_core";
    private static final Logger LOGGER = LogUtils.getLogger();

    public IndustrialCore(IEventBus modEventBus, ModContainer container) {

        ModFluids.register(modEventBus);
        com.jykito.industrialcore.sound.ModSounds.register(modEventBus);
        com.jykito.industrialcore.entity.ModEntities.register(modEventBus);
        com.jykito.industrialcore.particle.ModParticles.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        com.jykito.industrialcore.event.ItemRemaps.register();   // aliases must be in before the registries fire
        com.jykito.industrialcore.item.ModDataComponents.register(modEventBus);
        com.jykito.industrialcore.item.ModArmorMaterials.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);
        com.jykito.industrialcore.advancement.VoltexEventTrigger.TRIGGERS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModMessages::register);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new com.jykito.industrialcore.item.custom.NexiteArmorEventHandler());
        NeoForge.EVENT_BUS.register(new com.jykito.industrialcore.item.custom.PlasmaArmorEventHandler());
        NeoForge.EVENT_BUS.register(new com.jykito.industrialcore.item.custom.weapon.WeaponEventHandler());
        NeoForge.EVENT_BUS.register(new com.jykito.industrialcore.item.custom.PreArmorDamage());
        container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // packets and the advancement criterion are registered on the mod bus in the constructor (1.21)
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
