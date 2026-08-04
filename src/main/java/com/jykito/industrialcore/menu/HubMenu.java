package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.HubBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class HubMenu extends AbstractContainerMenu {

    // координаты слотов (совпадают с текстурами хаба)
    public static final int INV_X = 44, INV_Y = 131, HOTBAR_Y = 194, SLOT_STEP = 19;
    public static final int UP_X = 206, UP_Y = 48;

    @Nullable public final HubBlockEntity be;
    public int selectedFace = -1;
    /** Какой фильтр показан в конфиге: 0 = ввод, 1 = вывод (только клиентский вид). */
    public int filterDir = 0;

    public HubMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(ModMenuTypes.HUB_MENU.get(), id);
        BlockEntity b = inv.player.level().getBlockEntity(buf.readBlockPos());
        this.be = (b instanceof HubBlockEntity h) ? h : null;
        layout(inv);
    }

    public HubMenu(int id, Inventory inv, HubBlockEntity be) {
        super(ModMenuTypes.HUB_MENU.get(), id);
        this.be = be;
        layout(inv);
    }

    private void layout(Inventory inv) {
        if (be != null) {
            for (int f = 0; f < 6; f++) {
                final int face = f;
                addSlot(new SlotItemHandler(be.upgradeHandler, face, UP_X, UP_Y) {
                    @Override public boolean isActive() { return selectedFace == face; }
                    @Override public int getMaxStackSize() { return 1; }
                });
            }
        }
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inv, j + i * 9 + 9, INV_X + j * SLOT_STEP, INV_Y + i * SLOT_STEP));
        for (int k = 0; k < 9; k++)
            addSlot(new Slot(inv, k, INV_X + k * SLOT_STEP, HOTBAR_Y));
    }

    /** 0..5 цикл; 100+грань*3+m задать режим экскл.; 200+грань выбрать (206 закрыть);
     *  300+грань*3+t тумблер; 400+грань*2+which shift-переключить ввод/вывод. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null) return false;
        if (id >= 0 && id < 6) { be.cycleMode(Direction.from3DDataValue(id)); return true; }
        if (id >= 100 && id < 118) {
            int face = (id - 100) / 3, m = (id - 100) % 3;
            be.setMode(Direction.from3DDataValue(face),
                    com.jykito.industrialcore.block.custom.logistics.HubFaceMode.values()[m]);
            return true;
        }
        if (id >= 200 && id <= 206) { selectedFace = (id == 206) ? -1 : (id - 200); return true; }
        if (id >= 300 && id < 318) { be.toggle((id - 300) / 3, (id - 300) % 3); return true; }
        if (id >= 400 && id < 412) { be.toggleDir((id - 400) / 2, (id - 400) % 2); return true; }
        return false;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return be != null && !be.isRemoved()
                && player.distanceToSqr(be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 0.5, be.getBlockPos().getZ() + 0.5) <= 64.0;
    }
}
