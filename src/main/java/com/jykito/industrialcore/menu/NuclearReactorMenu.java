package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.NuclearReactorBlockEntity;
import com.jykito.industrialcore.fluid.ModFluids;
import com.jykito.industrialcore.item.custom.ReactorComponentItem;
import com.jykito.industrialcore.item.custom.ReactorFuelRodItem;
import com.jykito.industrialcore.item.custom.ReactorSchemeItem;
import com.jykito.industrialcore.item.upgrade.DirectionalUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class NuclearReactorMenu extends AbstractContainerMenu {

    public final NuclearReactorBlockEntity blockEntity;
    private final ContainerData data;

    private static final int GRID_X0   = 79;
    private static final int GRID_Y0   = 10;
    private static final int GRID_STEP = 18;

    public NuclearReactorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(NuclearReactorBlockEntity.DATA_COUNT));
    }

    public NuclearReactorMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.NUCLEAR_REACTOR_MENU.get(), id);
        this.blockEntity = (NuclearReactorBlockEntity) entity;
        this.data = data;
        var h = this.blockEntity.getItemHandler();

        for (int i = 0; i < NuclearReactorBlockEntity.GRID_SLOTS; i++) {
            int col = i % NuclearReactorBlockEntity.GRID_COLS;
            int row = i / NuclearReactorBlockEntity.GRID_COLS;
            this.addSlot(new SlotItemHandler(h, i,
                    GRID_X0 + col * GRID_STEP,
                    GRID_Y0 + row * GRID_STEP));
        }

        this.addSlot(new SlotItemHandler(h, NuclearReactorBlockEntity.SLOT_COOLANT_IN, 0, 0) {
            @Override public boolean isActive() { return false; }
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        this.addSlot(new SlotItemHandler(h, NuclearReactorBlockEntity.SLOT_COOLANT_OUT, 0, 0) {
            @Override public boolean isActive() { return false; }
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        this.addSlot(new SlotItemHandler(h, NuclearReactorBlockEntity.SLOT_DEPLETED, 0, 0) {
            @Override public boolean isActive() { return false; }
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        this.addSlot(new SlotItemHandler(h, NuclearReactorBlockEntity.SLOT_HOT_IN, 0, 0) {
            @Override public boolean isActive() { return false; }
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        this.addSlot(new SlotItemHandler(h, NuclearReactorBlockEntity.SLOT_HOT_OUT, 0, 0) {
            @Override public boolean isActive() { return false; }
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        for (int i = 0; i < 4; i++) {
            final int slot = NuclearReactorBlockEntity.SLOT_UPG_START + i;
            this.addSlot(new SlotItemHandler(h, slot, 227, 12 + i * 28) {
                @Override public boolean mayPlace(@NotNull ItemStack stack) {
                    return stack.getItem() instanceof DirectionalUpgradeItem;
                }
            });
        }

        this.addSlot(new SlotItemHandler(h, NuclearReactorBlockEntity.SLOT_SCHEME, 18, 100) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof ReactorSchemeItem;
            }
        });

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 44 + j * 19, 133 + i * 19));

        for (int k = 0; k < 9; k++)
            this.addSlot(new Slot(inv, k, 44 + k * 19, 196));

        addDataSlots(data);
    }

    public int getEnergyStored()     { return (data.get(1) << 16) | (data.get(0) & 0xFFFF); }
    public int getHullHeat()         { return (data.get(3) << 16) | (data.get(2) & 0xFFFF); }
    public int getCoolantAmount()    { return data.get(4); }
    public int getHotCoolantAmount() { return data.get(5); }
    public int getEnergyPerTick()    { return (data.get(7) << 16) | (data.get(6) & 0xFFFF); }
    public int getCellHeat(int i)    {
        return (i >= 0 && i < NuclearReactorBlockEntity.GRID_SLOTS) ? data.get(8 + i) : 0;
    }

    public boolean isDangerous() { return getHullHeat() > NuclearReactorBlockEntity.DANGER_HULL_HEAT; }
    public boolean isActive()    { return getEnergyPerTick() > 0; }
    public int getSchemeIndex()  { return data.get(33); }
    public boolean isSchemeMatch() { return data.get(34) == 1; }

    public int getScaledEnergy() {
        int e = getEnergyStored();
        return e > 0 ? Math.min(88, e * 88 / NuclearReactorBlockEntity.MAX_ENERGY) : 0;
    }
    public int getScaledHullHeat() {
        int h = getHullHeat();
        return h > 0 ? Math.min(88, h * 88 / NuclearReactorBlockEntity.MAX_HULL_HEAT) : 0;
    }
    public int getScaledCoolant() {
        int c = getCoolantAmount();
        return c > 0 ? Math.min(64, c * 64 / NuclearReactorBlockEntity.TANK_CAPACITY) : 0;
    }
    public int getScaledHotCoolant() {
        int c = getHotCoolantAmount();
        return c > 0 ? Math.min(64, c * 64 / NuclearReactorBlockEntity.TANK_CAPACITY) : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();

        int beSlots = NuclearReactorBlockEntity.SLOT_COUNT;
        if (index < beSlots) {
            if (!this.moveItemStackTo(slotStack, beSlots, beSlots + 36, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(slotStack, result);
        } else {
            if (slotStack.getItem() instanceof ReactorFuelRodItem
                    || slotStack.getItem() instanceof ReactorComponentItem) {
                if (!this.moveItemStackTo(slotStack, 0, NuclearReactorBlockEntity.GRID_SLOTS, false))
                    return ItemStack.EMPTY;
            } else if (slotStack.getItem() instanceof DirectionalUpgradeItem) {
                if (!this.moveItemStackTo(slotStack, NuclearReactorBlockEntity.SLOT_UPG_START,
                        NuclearReactorBlockEntity.SLOT_UPG_END + 1, false)) return ItemStack.EMPTY;
            } else if (slotStack.getItem() instanceof ReactorSchemeItem) {
                if (!this.moveItemStackTo(slotStack, NuclearReactorBlockEntity.SLOT_SCHEME,
                        NuclearReactorBlockEntity.SLOT_SCHEME + 1, false)) return ItemStack.EMPTY;
            } else if (index < beSlots + 27) {
                if (!this.moveItemStackTo(slotStack, beSlots + 27, beSlots + 36, false)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(slotStack, beSlots, beSlots + 27, false)) return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (slotStack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, slotStack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.NUCLEAR_REACTOR.get());
    }
}
