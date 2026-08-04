package com.jykito.industrialcore.compat.jade;

import com.jykito.industrialcore.IndustrialCore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Точка входа Jade. Класс находит сам Jade по аннотации при своём старте —
 * если мода нет, никто сюда не заглядывает и отсутствующие классы никого не смущают.
 * Поэтому ссылаться на него из кода мода нельзя.
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
