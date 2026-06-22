package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.OreWasherBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class OreWasherMenu extends AbstractContainerMenu {
    public final OreWasherBlockEntity blockEntity;
    private final ContainerData data;

    public OreWasherMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(6));
    }

    public OreWasherMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ORE_WASHER_MENU.get(), pContainerId);
        this.blockEntity = (OreWasherBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();

        this.addSlot(new SlotItemHandler(h, 0, 92, 54));

        this.addSlot(new SlotItemHandler(h, 1, 149, 34) { @Override public boolean mayPlace(@NotNull ItemStack s) { return false; } });
        this.addSlot(new SlotItemHandler(h, 2, 149, 54) { @Override public boolean mayPlace(@NotNull ItemStack s) { return false; } });
        this.addSlot(new SlotItemHandler(h, 3, 149, 74) { @Override public boolean mayPlace(@NotNull ItemStack s) { return false; } });

        this.addSlot(new SlotItemHandler(h, 4, 189, 37));
        this.addSlot(new SlotItemHandler(h, 5, 189, 55));
        this.addSlot(new SlotItemHandler(h, 6, 189, 73));
        this.addSlot(new SlotItemHandler(h, 7, 189, 91));

        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 133 + i * 19));
        for (int k = 0; k < 9; ++k)
            this.addSlot(new Slot(inv, k, 44 + k * 19, 196));

        addDataSlots(data);
    }

    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        int arrow = 27;
        if (maxProgress == 0 || progress == 0) return 0;
        return Math.min(arrow, (progress * arrow + maxProgress - 1) / maxProgress);
    }

    public int getScaledEnergy() {
        int energy = (this.data.get(3) << 16) | (this.data.get(2) & 0xFFFF);
        int maxEnergy = (this.data.get(5) << 16) | (this.data.get(4) & 0xFFFF);
        int bar = 110;
        return maxEnergy != 0 && energy != 0 ? (int) ((long) energy * bar / maxEnergy) : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        final int MACHINE_SLOTS = 8;
        final int INV_END = MACHINE_SLOTS + 36;

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(sourceStack, MACHINE_SLOTS, INV_END, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(sourceStack, 0, 1, false)) {
                if (!moveItemStackTo(sourceStack, 4, 8, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();
        sourceSlot.onTake(playerIn, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.ORE_WASHER.get());
    }
}
