package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.MetalFormerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MetalFormerMenu extends BaseMachineMenu {

    private final ContainerData data;

    public MetalFormerMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {

        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(7));
    }

    public MetalFormerMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {

        super(ModMenuTypes.ROLLING_MENU.get(), pContainerId, inv, entity, data);
        this.data = data;
    }

    public int getMode() {
        return this.data.get(6);
    }

    public MetalFormerBlockEntity getBlockEntity() {
        return (MetalFormerBlockEntity) this.blockEntity;
    }
}
