package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.LateMachineCoreBlockEntity;
import com.jykito.industrialcore.multiblock.LateMultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Late multiblock core: status and the player inventory; the ore washer also has a processor strain slot in the display. */
public class LateMachineCoreMenu extends MachineMenu {

    public static final int NANITE_X = 222, NANITE_Y = 16;

    public final LateMachineCoreBlockEntity blockEntity;
    /** Whether there is a strain slot (multiblock ore washer only); it comes first. */
    public final boolean nanite;
    private final ContainerData data;

    public LateMachineCoreMenu(int id, Inventory inv, FriendlyByteBuf extra) {
        this(id, inv, (LateMachineCoreBlockEntity) inv.player.level().getBlockEntity(extra.readBlockPos()), new SimpleContainerData(LateMachineCoreBlockEntity.DATA_COUNT));
    }

    public LateMachineCoreMenu(int id, Inventory inv, LateMachineCoreBlockEntity be, ContainerData data) {
        super(ModMenuTypes.LATE_MACHINE_CORE_MENU.get(), id);
        this.blockEntity = be;
        this.data = data;
        nanite = be.acceptsNanites();
        if (nanite) addSlot(new net.minecraftforge.items.SlotItemHandler(be.getNanites(), 0, NANITE_X, NANITE_Y));
        addPlayerInventory(inv, 44, 145);   // the core window is taller than standard: the inventory plate starts at y=141
        addDataSlots(data);
    }

    public boolean isFormed() { return data.get(0) == 1; }
    public LateMultiblockPattern.Problem problem() { return LateMultiblockPattern.Problem.values()[clamp(data.get(1), LateMultiblockPattern.Problem.values().length)]; }
    public LateMultiblockPattern.Cell expected() { return LateMultiblockPattern.Cell.values()[clamp(data.get(2), LateMultiblockPattern.Cell.values().length)]; }
    /** Where the wrong block is, in world coordinates. ContainerData carries a short, and the offset from the core fits. */
    public BlockPos problemPos() { return blockEntity.getBlockPos().offset((short) data.get(3), (short) data.get(4), (short) data.get(5)); }
    public int progress()      { return data.get(6); }
    public int maxProgress()   { return data.get(7); }
    public int energy()        { return unpack(data.get(8), data.get(9)); }
    public int energyCap()     { return unpack(data.get(10), data.get(11)); }
    public int parallel()      { return data.get(12); }
    public int ops()           { return data.get(13); }
    public int inputs()        { return data.get(14); }
    public int outputs()       { return data.get(15); }
    public int energyPorts()   { return data.get(16); }
    public int energyPerTick() { return unpack(data.get(17), data.get(18)); }
    public int speedPercent()  { return data.get(21); }
    public int mode()          { return data.get(22); }
    public int modeCount()     { return data.get(23); }
    public int heat()          { return unpack(data.get(24), data.get(25)); }
    public int heatNeeded()    { return data.get(26); }
    public boolean usesHeat()  { return data.get(27) == 1; }
    public int heatPorts()     { return data.get(28); }

    /** Name key of the current mode; the core block exists on the client too, so the mode list comes from it. */
    public String modeKey() {
        var modes = ((com.jykito.industrialcore.block.custom.LateMachineCoreBlock) blockEntity.getBlockState().getBlock()).modes();
        return modes.get(clamp(mode(), modes.size())).key();
    }

    /** Button 0: next core mode. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        return id == 0 && blockEntity.cycleMode();
    }

    public ItemStack product() {
        int id = unpack(data.get(19), data.get(20));
        if (id < 0) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.byId(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    public int scaledEnergy(int px)   { return scale(energy(), energyCap(), px); }
    public int scaledProgress(int px) { return scaleProgress(progress(), maxProgress(), px); }

    private static int clamp(int v, int n) { return v < 0 || v >= n ? 0 : v; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!nanite) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), result = stack.copy();
        if (index == 0) { if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY; }
        else if (!slots.get(0).mayPlace(stack) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
}
