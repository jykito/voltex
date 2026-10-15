package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.CrystallizerBlockEntity;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class CrystallizerMenu extends MachineMenu {

    /** Machine slots: seed, output, four upgrades. */
    private static final int MACHINE_SLOTS = 6;
    private static final int INV_END = MACHINE_SLOTS + 36;

    public final CrystallizerBlockEntity blockEntity;
    private final ContainerData data;

    public CrystallizerMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(6));
    }

    public CrystallizerMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.CRYSTALLIZER_MENU.get(), containerId);
        this.blockEntity = (CrystallizerBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();

        this.addSlot(new SlotItemHandler(h, 0, 96, 60));
        this.addSlot(new SlotItemHandler(h, 1, 141, 60) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        int[] upgradeY = {37, 55, 73, 91};
        for (int u = 0; u < 4; u++)
            this.addSlot(new SlotItemHandler(h, 2 + u, 184, upgradeY[u]));

        addPlayerInventory(inv, 43, 134);

        addDataSlots(data);
    }

    public int getEnergyStored() { return unpack(data.get(2), data.get(3)); }
    public int getMaxEnergy()    { return unpack(data.get(4), data.get(5)); }

    public int getScaledProgress(int barWidth) {
        int progress    = data.get(0);
        int maxProgress = data.get(1);
        return scaleProgress(progress, maxProgress, barWidth);
    }

    public int getScaledEnergy(int barWidth) {
        return scale(getEnergyStored(), getMaxEnergy(), barWidth);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(sourceStack, MACHINE_SLOTS, INV_END, true)) return ItemStack.EMPTY;
        } else {
            if (sourceStack.getItem() instanceof MachineUpgradeItem) {
                if (!moveItemStackTo(sourceStack, 2, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(sourceStack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();
        sourceSlot.onTake(player, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.CRYSTALLIZER.get());
    }
}
