package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.DrillBeaconBlockEntity;
import com.jykito.industrialcore.item.custom.MinerLensItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

/** Окно бурового маяка: слот линзы, ход скана, слой глубины; состав слоя — из клиентской копии маяка. */
public class DrillBeaconMenu extends MachineMenu {

    public static final int LENS_X = 222, LENS_Y = 41, INV_X = 44, INV_Y = 145;

    public final DrillBeaconBlockEntity beacon;
    private final ContainerData data;

    public DrillBeaconMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(7));
    }

    public DrillBeaconMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.DRILL_BEACON_MENU.get(), id);
        this.beacon = (DrillBeaconBlockEntity) be;
        this.data = data;
        addSlot(new SlotItemHandler(beacon.getLens(), 0, LENS_X, LENS_Y));
        addPlayerInventory(inv, INV_X, INV_Y);
        addDataSlots(data);
    }

    public DrillBeaconBlockEntity.State state() {
        var v = DrillBeaconBlockEntity.State.values();
        return v[Math.max(0, Math.min(v.length - 1, data.get(0)))];
    }
    public int step()   { return data.get(1); }
    public int total()  { return data.get(2); }
    public int bandLo() { return data.get(3); }
    public int bandHi() { return data.get(4); }
    public int top()    { return data.get(5); }
    public int minY()   { return (short) data.get(6); }   // низ мира отрицательный: знак через short

    /** Нижняя и верхняя высота слоя (включительно). */
    public int yFrom() { return minY() + 16 * bandLo(); }
    public int yTo()   { return minY() + 16 * bandHi() + 15; }

    public int scaledScan(int px) { return scaleProgress(step(), total(), px); }

    /** 0/1 — нижняя граница вниз/вверх, 2/3 — верхняя, 4 — пересканировать. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id > 4) return false;
        beacon.onButton(id);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), result = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof MinerLensItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return !beacon.isRemoved() && player.distanceToSqr(beacon.getBlockPos().getCenter()) < 64;
    }
}
