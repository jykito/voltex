package com.jykito.industrialcore.compat.jade;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade entry point. Jade finds the class by annotation when it starts;
 * if the mod is absent nobody looks here and the missing classes bother nobody.
 * That is why mod code must not reference it.
 */
@WailaPlugin(IndustrialCore.MODID)
public class VoltexJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MachineDataProvider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MachineDataProvider.INSTANCE, Block.class);
    }
}
