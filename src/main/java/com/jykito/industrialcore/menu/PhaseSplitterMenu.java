package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.PhaseSplitterBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class PhaseSplitterMenu extends AbstractContainerMenu {
    public final PhaseSplitterBlockEntity blockEntity;
    private final ContainerData data;

    private static final int TE_SLOT_COUNT     = 8;
    private static final int PLAYER_INV_START  = TE_SLOT_COUNT;
    private static final int PLAYER_INV_COUNT  = 36;

    public PhaseSplitterMenu(int pContainerId, Inventory inv, FriendlyByteBuf buf) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(6));
    }

    public PhaseSplitterMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.PHASE_SPLITTER_MENU.get(), pContainerId);
        this.blockEntity = (PhaseSplitterBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();

        this.addSlot(new SlotItemHandler(h, 0, 86, 60));

        this.addSlot(new SlotItemHandler(h, 1, 144, 41) { @Override public boolean mayPlace(@NotNull ItemStack s) { return false; } });
        this.addSlot(new SlotItemHandler(h, 2, 144, 60) { @Override public boolean mayPlace(@NotNull ItemStack s) { return false; } });
        this.addSlot(new SlotItemHandler(h, 3, 144, 79) { @Override public boolean mayPlace(@NotNull ItemStack s) { return false; } });

        this.addSlot(new SlotItemHandler(h, 4, 188, 38));
        this.addSlot(new SlotItemHandler(h, 5, 188, 55));
        this.addSlot(new SlotItemHandler(h, 6, 188, 73));
        this.addSlot(new SlotItemHandler(h, 7, 188, 91));

        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 43 + j * 19, 134 + i * 19));

        for (int k = 0; k < 9; ++k)
            this.addSlot(new Slot(inv, k, 43 + k * 19, 197));

        addDataSlots(data);
    }

    public int getScaledProgress() {
        int progress    = this.data.get(4);
        int maxProgress = this.data.get(5);
        if (maxProgress == 0 || progress == 0) return 0;
        return Math.min(32, (progress * 32 + maxProgress - 1) / maxProgress);
    }

    public int getScaledEnergy() {
        int energy    = (data.get(1) << 16) | (data.get(0) & 0xFFFF);
        int maxEnergy = (data.get(3) << 16) | (data.get(2) & 0xFFFF);
        return maxEnergy != 0 && energy != 0 ? (int)((long) energy * 110 / maxEnergy) : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack   = sourceStack.copy();

        if (index < TE_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, PLAYER_INV_START, PLAYER_INV_START + PLAYER_INV_COUNT, true))
                return ItemStack.EMPTY;
        } else if (index < PLAYER_INV_START + PLAYER_INV_COUNT) {
            if (!moveItemStackTo(sourceStack, 0, 1, false))
                if (!moveItemStackTo(sourceStack, 4, 8, false))
                    return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
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
