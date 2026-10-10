package com.jykito.industrialcore.compat.ae2;

import com.jykito.industrialcore.block.entity.ItemFluidPortBlockEntity;
import com.jykito.industrialcore.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

/**
 * Развилка для МЭ-портов без ссылок на AE2: этот класс грузится всегда, классы с API AE2
 * ({@link Ae2PortFactory} и порты) — только когда AE2 установлен. Без AE2 МЭ-порт — обычный порт.
 */
public final class MePorts {

    private MePorts() {}

    public static final boolean AE2 = ModList.get().isLoaded("ae2");

    public static ItemFluidPortBlockEntity create(BlockPos pos, BlockState state) {
        return AE2 ? Ae2PortFactory.create(pos, state) : new ItemFluidPortBlockEntity(ModBlockEntities.ME_PORT_BE.get(), pos, state);
    }
}
