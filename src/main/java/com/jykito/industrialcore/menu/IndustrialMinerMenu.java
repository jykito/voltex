package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.IndustrialMinerCoreBlockEntity;
import com.jykito.industrialcore.item.custom.NaniteStrainItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/** Industrial miner window: three output rows, a nanite strain slot, the player inventory; sites are read from the client copy of the core. */
public class IndustrialMinerMenu extends MachineMenu {

    public static final int OUT_X = 43, OUT_Y = 140, INV_X = 44, INV_Y = 217, NANITE_X = 222, NANITE_Y = 16;
    private static final int OUT = IndustrialMinerCoreBlockEntity.OUTPUT_SLOTS;
    /** The strain slot comes right after the output. */
    public static final int NANITE_SLOT = OUT;

    public final IndustrialMinerCoreBlockEntity core;
    private final ContainerData data;

    public IndustrialMinerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(13));
    }

    public IndustrialMinerMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.INDUSTRIAL_MINER_MENU.get(), id);
        this.core = (IndustrialMinerCoreBlockEntity) be;
        this.data = data;
        for (int i = 0; i < OUT; i++)
            addSlot(new SlotItemHandler(core.getOutput(), i, OUT_X + (i % 9) * SLOT_STEP, OUT_Y + (i / 9) * SLOT_STEP) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
            });
        addSlot(new SlotItemHandler(core.getNanites(), 0, NANITE_X, NANITE_Y));
        addPlayerInventory(inv, INV_X, INV_Y);
        addDataSlots(data);
    }

    public int energy()      { return unpack(data.get(0), data.get(1)); }
    public int energyCap()   { return unpack(data.get(2), data.get(3)); }
    public int fluid()       { return unpack(data.get(4), data.get(5)); }
    public int fluidCap()    { return unpack(data.get(6), data.get(7)); }
    public int progress()    { return data.get(8); }
    public int cycle()       { return data.get(9); }
    public int parallel()    { return data.get(10); }
    public int lastOres()    { return data.get(11); }
    public IndustrialMinerCoreBlockEntity.Status status() {
        var v = IndustrialMinerCoreBlockEntity.Status.values();
        return v[Math.max(0, Math.min(v.length - 1, data.get(12)))];
    }

    public int scaledEnergy(int px)   { return scale(energy(), energyCap(), px); }
    public int scaledFluid(int px)    { return scale(fluid(), fluidCap(), px); }
    public int scaledProgress(int px) { return scaleProgress(progress(), cycle(), px); }

    /** 0..3 enable/disable a site, 4..7 unlink (handled by the core on the server). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= 2 * IndustrialMinerCoreBlockEntity.MAX_SITES) return false;
        core.onButton(id);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), result = stack.copy();
        if (index <= NANITE_SLOT) {
            if (!moveItemStackTo(stack, NANITE_SLOT + 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!(stack.getItem() instanceof NaniteStrainItem) || !moveItemStackTo(stack, NANITE_SLOT, NANITE_SLOT + 1, false)) {
            return ItemStack.EMPTY;   // nothing goes into the output; from the inventory only a strain into its slot
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return !core.isRemoved() && player.distanceToSqr(core.getBlockPos().getCenter()) < 64 * 64 / 4.0;
    }
}
