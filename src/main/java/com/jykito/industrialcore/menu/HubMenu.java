package com.jykito.industrialcore.menu;

import com.jykito.industrialcore.block.entity.HubBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class HubMenu extends MachineMenu {

    // slot coordinates (match hub_*_gui.png from tools/gui_gen/build_hub_guis.ps1)
    public static final int INV_X = 44, INV_Y = 169;
    public static final int UP_X = 222, UP_Y = 29;

    @Nullable public final HubBlockEntity be;
    public int selectedFace = -1;
    /** Which filter is shown in the config: 0 = input, 1 = output (client view only). */
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
        addPlayerInventory(inv, INV_X, INV_Y);
    }

    /** 0..5 cycle; 100+face*3+m set an exclusive mode; 200+face select (206 close);
     *  300+face*3+t toggle; 400+face*2+which shift-toggle input/output. */
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
