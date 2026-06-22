package com.jykito.industrialcore.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ExtractorMenu extends BaseMachineMenu {

    public ExtractorMenu(int pContainerId, Inventory inv, FriendlyByteBuf buf) {
        super(ModMenuTypes.EXTRACTOR_MENU.get(), pContainerId, inv,
                inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(6));
    }

    public ExtractorMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.EXTRACTOR_MENU.get(), pContainerId, inv, entity, data);
    }
}
