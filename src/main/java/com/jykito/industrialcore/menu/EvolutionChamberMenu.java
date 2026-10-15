package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.EvolutionChamberBlockEntity;
import com.jykito.industrialcore.item.custom.NaniteStrainItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

/** Evolution chamber window: strain, ore, inventory; the graph is read from the client copy of the chamber. */
public class EvolutionChamberMenu extends MachineMenu {

    public static final int STRAIN_X = 23, STRAIN_Y = 47, FEED_X = 23, FEED_Y = 75, INV_X = 44, INV_Y = 155;
    private static final int MACHINE_SLOTS = 2;

    public final EvolutionChamberBlockEntity chamber;
    private final ContainerData data;

    public EvolutionChamberMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(8));
    }

    public EvolutionChamberMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.EVOLUTION_CHAMBER_MENU.get(), id);
        this.chamber = (EvolutionChamberBlockEntity) be;
        this.data = data;
        addSlot(new SlotItemHandler(chamber.getItems(), EvolutionChamberBlockEntity.SLOT_STRAIN, STRAIN_X, STRAIN_Y));
        addSlot(new SlotItemHandler(chamber.getItems(), EvolutionChamberBlockEntity.SLOT_FEED, FEED_X, FEED_Y));
        addPlayerInventory(inv, INV_X, INV_Y);
        addDataSlots(data);
    }

    public int energy()      { return unpack(data.get(0), data.get(1)); }
    public int energyCap()   { return unpack(data.get(2), data.get(3)); }
    public int heat()        { return data.get(5); }
    public int ticksPerGeneration() { return data.get(7); }
    public EvolutionChamberBlockEntity.Status status() {
        var v = EvolutionChamberBlockEntity.Status.values();
        return v[Math.max(0, Math.min(v.length - 1, data.get(6)))];
    }

    public int scaledEnergy(int px)   { return scale(energy(), energyCap(), px); }
    public int scaledHeat(int px)     { return scale(heat(), EvolutionChamberBlockEntity.MAX_HEAT, px); }
    public int scaledProgress(int px) { return scaleProgress(data.get(4), 1000, px); }

    public ItemStack strain() { return chamber.getItems().getStackInSlot(EvolutionChamberBlockEntity.SLOT_STRAIN); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), result = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            int to = stack.getItem() instanceof NaniteStrainItem ? EvolutionChamberBlockEntity.SLOT_STRAIN : EvolutionChamberBlockEntity.SLOT_FEED;
            if (!slots.get(to).mayPlace(stack) || !moveItemStackTo(stack, to, to + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return !chamber.isRemoved() && player.distanceToSqr(chamber.getBlockPos().getCenter()) < 64;
    }
}
