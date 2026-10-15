package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.CryoCoreBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class CryoChamberMenu extends MachineMenu {

    /** Chamber slots: input, output, three cryo rods, depth module. */
    private static final int MACHINE_SLOTS = 6;
    private static final int INV_END = MACHINE_SLOTS + 36;

    public final CryoCoreBlockEntity blockEntity;
    private final ContainerData data;

    public CryoChamberMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(10));
    }

    public CryoChamberMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.CRYO_CHAMBER_MENU.get(), containerId);
        this.blockEntity = (CryoCoreBlockEntity) entity;
        this.data = data;

        var h = this.blockEntity.getItemHandler();

        this.addSlot(new SlotItemHandler(h, 0, 92, 35));
        this.addSlot(new SlotItemHandler(h, 1, 148, 35) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        for (int r = 0; r < 3; r++)
            this.addSlot(new SlotItemHandler(h, 2 + r, 97 + r * 23, 75));
        this.addSlot(new SlotItemHandler(h, 5, 120, 55));   // depth module

        // inventory coordinates are the same as the magnet's: the window has the same height
        addPlayerInventory(inv, 44, 143);

        addDataSlots(data);
    }

    public int getEnergyStored() { return unpack(data.get(2), data.get(3)); }
    public int getMaxEnergy()    { return unpack(data.get(4), data.get(5)); }
    public int getDepth()        { return data.get(6); }
    public int getHeatInflow()   { return data.get(7); }
    public boolean isFormed()    { return data.get(8) != 0; }
    /** Up to which stage the inserted module allows. */
    public int getAllowedStage() { return data.get(9); }

    /** Stage by depth: what decides which recipes are available at all. */
    public int getStage() {
        int d = getDepth();
        if (d >= CryoCoreBlockEntity.depthForStage(3)) return 3;
        if (d >= CryoCoreBlockEntity.depthForStage(2)) return 2;
        if (d >= CryoCoreBlockEntity.depthForStage(1)) return 1;
        return 0;
    }

    public int getScaledProgress(int barWidth) {
        int progress = data.get(0), maxProgress = data.get(1);
        return scaleProgress(progress, maxProgress, barWidth);
    }

    public int getScaledEnergy(int barWidth) {
        return scale(getEnergyStored(), getMaxEnergy(), barWidth);
    }

    public int getScaledDepth(int barWidth) {
        int d = getDepth();
        return d <= 0 ? 0 : Math.min(barWidth, d * barWidth / CryoCoreBlockEntity.MAX_DEPTH);
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
            // Rods go into their own slots, everything else into the input.
            if (!moveItemStackTo(sourceStack, 2, MACHINE_SLOTS, false)
                    && !moveItemStackTo(sourceStack, 0, 1, false))
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
                player, ModBlocks.CRYO_CORE.get());
    }
}
