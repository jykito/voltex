package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.LogisticsNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class LogisticsNodeMenu extends AbstractContainerMenu {
    public final BlockPos nodePos;

    public LogisticsNodeMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(ModMenuTypes.LOGISTICS_NODE_MENU.get(), id);
        this.nodePos = buf.readBlockPos();
    }

    public LogisticsNodeMenu(int id, Inventory inv, BlockPos pos) {
        super(ModMenuTypes.LOGISTICS_NODE_MENU.get(), id);
        this.nodePos = pos;
    }

    @Override
    public boolean stillValid(Player player) {

        return player.level().getBlockEntity(nodePos) instanceof LogisticsNodeBlockEntity
                && player.distanceToSqr(nodePos.getX() + 0.5, nodePos.getY() + 0.5, nodePos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
