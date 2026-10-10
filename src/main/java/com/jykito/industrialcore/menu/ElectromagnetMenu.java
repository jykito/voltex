package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.ModBlocks;
import com.jykito.industrialcore.block.entity.ElectromagnetCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Меню ускорителя. Слоты: 0..7 — кольцо входов, 8 — выход, 9/10 — улучшения (пока заблокированы).
 * Энергия синкается через ContainerData(6); жидкости читаются из BE напрямую (синк getUpdateTag).
 */
public class ElectromagnetMenu extends MachineMenu {
    public final ElectromagnetCoreBlockEntity blockEntity;
    private final ContainerData data;
    private final BlockPos anchorPos;   // якорь дистанции stillValid: ядро при обычном открытии, контроллер при удалённом

    // 8 входных слотов: СИНИЕ 0-3 = пучок A (левая дуга), КРАСНЫЕ 4-7 = пучок B (правая дуга)
    private static final int[][] INPUTS = {
        {120, 22}, {97, 31}, {84, 52}, {97, 73},   // синие (A)
        {143, 31}, {156, 52}, {143, 73}, {120, 82} // красные (B)
    };
    private static final int MACHINE_SLOTS = 11;

    public ElectromagnetMenu(int id, Inventory inv, FriendlyByteBuf extra) {
        this(id, inv, extra.readBlockPos(), extra.readBlockPos());
    }

    private ElectromagnetMenu(int id, Inventory inv, BlockPos corePos, BlockPos anchorPos) {
        this(id, inv, inv.player.level().getBlockEntity(corePos), new SimpleContainerData(12), anchorPos);
    }

    public ElectromagnetMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        this(id, inv, entity, data, entity.getBlockPos());
    }

    public ElectromagnetMenu(int id, Inventory inv, BlockEntity entity, ContainerData data, BlockPos anchorPos) {
        super(ModMenuTypes.ELECTROMAGNET_MENU.get(), id);
        this.blockEntity = (ElectromagnetCoreBlockEntity) entity;
        this.data = data;
        this.anchorPos = anchorPos;
        var h = this.blockEntity.getItemHandler();

        for (int i = 0; i < 8; i++) this.addSlot(new SlotItemHandler(h, i, INPUTS[i][0], INPUTS[i][1]));

        this.addSlot(new SlotItemHandler(h, 8, 120, 52) {            // выход
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });
        // Апгрейд и катушка — клетки вровень с полосами резонанса и мощности (y 101..118), под ними место для молнии энергии.
        this.addSlot(new SlotItemHandler(h, 9, 186, 102) {           // апгрейд: Фазозахват (авто-×2)
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() == com.jykito.industrialcore.item.ModItems.PHASE_LOCK_UPGRADE.get();
            }
        });
        this.addSlot(new SlotItemHandler(h, 10, 54, 102) {           // катушка: вставить/вынуть (читается ядром)
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem;
            }
            @Override public int getMaxStackSize() { return 1; }
        });

        addPlayerInventory(inv, 44, 143);

        addDataSlots(data);
    }

    public int getProgress()    { return data.get(0); }   // тик крафта
    public int getMaxProgress() { return data.get(1); }   // длина крафта
    public int getInstability() { return data.get(6); }   // 0..100 %
    public boolean isWorking()  { return data.get(7) != 0; }
    public boolean isDoubling() { return data.get(8) != 0; }  // заряд резонанса полон → ×2
    public int getResonanceTarget() { return data.get(9); }   // 0..100 — дрейфующая цель
    public int getResonanceCharge() { return data.get(10); }  // 0..100 — заряд резонанса
    public int getThrottle()        { return data.get(11); }  // 0..100 — позиция троттла
    public int getCoilTier() {   // из слота катушки (10), синкается контейнером — для инфо-панели (радиус поля)
        var c = slots.get(10).getItem();
        return c.getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem m ? m.getTier() : 0;
    }

    public int getScaledEnergy() {
        int energy = unpack(data.get(2), data.get(3));
        int maxEnergy = unpack(data.get(4), data.get(5));
        int energyBarSize = 110;
        return maxEnergy != 0 && energy != 0 ? energy * energyBarSize / maxEnergy : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();
        final int INV_END = MACHINE_SLOTS + 36;

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(sourceStack, MACHINE_SLOTS, INV_END, true)) return ItemStack.EMPTY;
        } else if (sourceStack.getItem() instanceof com.jykito.industrialcore.item.custom.MagnetCoilItem) {
            if (!moveItemStackTo(sourceStack, 10, 11, false)) return ItemStack.EMPTY; // катушка → слот 10
        } else {
            if (!moveItemStackTo(sourceStack, 0, 8, false)) return ItemStack.EMPTY; // прочее → входы 0..7
        }

        if (sourceStack.getCount() == 0) sourceSlot.set(ItemStack.EMPTY);
        else sourceSlot.setChanged();
        sourceSlot.onTake(player, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() == null) return false;
        // дистанция до якоря: ядро при обычном GUI, контроллер при удалённом (поле не пускает к ядру)
        return player.distanceToSqr(anchorPos.getX() + 0.5, anchorPos.getY() + 0.5, anchorPos.getZ() + 0.5) <= 64.0;
    }
}
