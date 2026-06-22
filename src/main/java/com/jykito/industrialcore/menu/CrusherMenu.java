package com.jykito.industrialcore.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.inventory.ContainerData;

public class CrusherMenu extends BaseMachineMenu {

    public CrusherMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        super(ModMenuTypes.CRUSHER_MENU.get(), pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(6));
    }

    public CrusherMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.CRUSHER_MENU.get(), pContainerId, inv, entity, data);
    }
}
