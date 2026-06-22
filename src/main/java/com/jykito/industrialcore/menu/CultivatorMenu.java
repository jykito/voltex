package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.CultivatorBlockEntity;
import com.jykito.industrialcore.item.upgrade.CultivatorUpgradeItem;
import com.jykito.industrialcore.item.upgrade.MachineUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class CultivatorMenu extends AbstractContainerMenu {

    public final CultivatorBlockEntity blockEntity;
    private final ContainerData data;

    public CultivatorMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(6));
    }

    public CultivatorMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.CULTIVATOR_MENU.get(), containerId);
        this.blockEntity = (CultivatorBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();
        this.addSlot(new SlotItemHandler(h, 0, 102, 58));
        this.addSlot(new SlotItemHandler(h, 1, 144, 58) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        this.addSlot(new SlotItemHandler(h, 2, 123, 77));
        this.addSlot(new SlotItemHandler(h, 3, 94, 32));
        this.addSlot(new SlotItemHandler(h, 4, 114, 32));
        this.addSlot(new SlotItemHandler(h, 5, 134, 32));
        this.addSlot(new SlotItemHandler(h, 6, 154, 32));

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 133 + i * 19));
        for (int k = 0; k < 9; k++)
            this.addSlot(new Slot(inv, k, 44 + k * 19, 196));

        addDataSlots(data);
    }

    public int getEnergyStored() { return (data.get(3) << 16) | (data.get(2) & 0xFFFF); }
    public int getMaxEnergy()    { return (data.get(5) << 16) | (data.get(4) & 0xFFFF); }

    public int getScaledEnergy() {
        int energy = getEnergyStored(), maxEnergy = getMaxEnergy();
        return maxEnergy != 0 && energy != 0 ? (int) ((long) energy * 110 / maxEnergy) : 0;
    }

    public int getScaledProgress() {
        int progress = data.get(0), maxProgress = data.get(1);
        if (maxProgress == 0 || progress == 0) return 0;
        return Math.min(22, (progress * 22 + maxProgress - 1) / maxProgress);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        final int MACHINE_SLOTS = 7;
        final int INV_END = MACHINE_SLOTS + 36;

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(sourceStack, MACHINE_SLOTS, INV_END, true)) return ItemStack.EMPTY;
        } else {
            if (sourceStack.getItem() instanceof CultivatorUpgradeItem) {
                if (!moveItemStackTo(sourceStack, 2, 3, false)) return ItemStack.EMPTY;
            } else if (sourceStack.getItem() instanceof MachineUpgradeItem) {
                if (!moveItemStackTo(sourceStack, 3, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
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
                player, blockEntity.getBlockState().getBlock());
    }
}
